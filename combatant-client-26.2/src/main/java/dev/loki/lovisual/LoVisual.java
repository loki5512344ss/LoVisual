/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual;

import dev.loki.lovisual.events.impl.render.GameTickEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.addon.core.AddonManager;
import dev.loki.lovisual.addon.pipeline.AddonRenderPipelineManager;
import dev.loki.lovisual.api.v0.render.LoVisualRenderStage;
import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.command.core.CommandManager;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.i18n.I18nPreflightContributors;
import dev.loki.lovisual.features.gui.hud.registry.HudElements;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.nondraggable.StaticHudElementBootstrap;
import dev.loki.lovisual.features.gui.hud.nondraggable.StaticHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.SwapTooltip;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.lifecycle.ModuleAutoLoader;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.mixininterface.render.IGuiGraphics;
import dev.loki.lovisual.render.effects.NetherPortalRiftPass;
import dev.loki.lovisual.render.effects.SleepOverlayPass;
import dev.loki.lovisual.render.engine.LoVisualRenderEngineBootstrap;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.render.RenderProfiler2D;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.prewarm.RenderPrewarmContributors;
import dev.loki.lovisual.render.engine.prewarm.RenderPrewarmManager;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import dev.loki.lovisual.render.helpers.util.TickDelta;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.block.bed.SelfBedTracker;
import dev.loki.lovisual.util.click.AttackPressing;
import dev.loki.lovisual.util.combat.tracking.AntiBotTracker;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristics;
import dev.loki.lovisual.util.combat.rotation.SprintController;
import dev.loki.lovisual.util.entity.simulation.BoatSimulationCache;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.util.network.control.BacktrackController;
import dev.loki.lovisual.util.network.control.BlinkManager;
import dev.loki.lovisual.util.proxy.ProxyBackend;
import dev.loki.lovisual.util.network.control.FakeLagController;
import dev.loki.lovisual.util.player.inventory.core.InventorySwap;
import dev.loki.lovisual.util.player.simulation.PlayerSimulationCache;
import dev.loki.lovisual.util.pvp.tracker.PvpTargetTracker;
import dev.loki.lovisual.util.pvp.tracker.PvpTracker;
import dev.loki.lovisual.util.target.TargetManager;
import dev.loki.lovisual.util.time.TimerController;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.KeyBindSetting;
import dev.loki.lovisual.features.module.core.Module;

public class LoVisual implements ClientModInitializer {

    private static void renderWorldPhase(WorldPhase phase,
                                         com.mojang.blaze3d.vertex.PoseStack matrices,
                                         net.minecraft.client.renderer.SubmitNodeCollector consumers) {
        if (!Lifecycle.canRunRender()) return;

        ModuleManager.renderWorld(
                phase,
                matrices,
                consumers,
                TickDelta.tickProgress(false)
        );
    }

    private static void refreshHudFrameTiming(DeltaTracker tickCounter) {
        LoVisualRenderSystem.updateFrameTiming(
                TickDelta.tickProgress(tickCounter, false),
                TickDelta.frameDeltaTicks(tickCounter),
                TickDelta.fixedDeltaTicks(tickCounter)
        );
    }

    private static void renderHudPhase(HudPhase phase, GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        if (!Lifecycle.canRunHud()) return;
        float tickProgress = TickDelta.tickProgress(tickCounter, false);
        ModuleManager.renderHud(phase, ctx, tickProgress);
        Renderer2D.Deferred2DLayer layer = deferredLayerForHudPhase(phase);
        if (renderHudEngine(phase, layer, ctx, tickCounter)) {
            appendHudDeferredMarker(layer, ctx);
        }
    }

    private static boolean renderHudEngine(HudPhase phase,
                                           Renderer2D.Deferred2DLayer layer,
                                           GuiGraphicsExtractor ctx,
                                           DeltaTracker tickCounter) {
        boolean rawMain = hasHudMainPassWork(phase, HudRenderSpace.UNSCALED)
                || AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.HUD_RAW);
        boolean scaledMain = hasHudMainPassWork(phase, HudRenderSpace.SCALED)
                || AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.HUD_SCALED);
        boolean logicalMain = hasHudMainPassWork(phase, HudRenderSpace.UNSCALED_LOGICAL)
                || AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.HUD_LOGICAL);
        boolean rawForeground = hasHudForegroundPassWork(phase, HudRenderSpace.UNSCALED)
                || AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.HUD_RAW_FOREGROUND);
        boolean scaledForeground = hasHudForegroundPassWork(phase, HudRenderSpace.SCALED)
                || AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.HUD_SCALED_FOREGROUND);
        boolean logicalForeground = hasHudForegroundPassWork(phase, HudRenderSpace.UNSCALED_LOGICAL)
                || AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.HUD_LOGICAL_FOREGROUND);
        if (!rawMain && !scaledMain && !logicalMain && !rawForeground && !scaledForeground && !logicalForeground) {
            return false;
        }

        float tickProgress = TickDelta.tickProgress(tickCounter, false);
        TextRenderer textRenderer = TextRenderer.get();

        FullScreenRenderer.ensureInit();
        ViewportContext.beginUnscaled(ctx);
        refreshHudFrameTiming(tickCounter);

        Renderer2D.withDeferredLayer(layer, () -> {
            SystemCursor.beginFrame(ctx);
            try (RenderPhaseScope hudPhase = LoVisualRenderSystem.phase(RenderPhase.HUD_MAIN, "2d:phase:" + phase.name())) {
                // Raw HUD pass
                if (rawMain) {
                    Renderer2D.COLOR.begin();
                    ModuleManager.renderHudEngine(phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    StaticHudElementRegistry.renderAllEngine(phase, HudRenderSpace.UNSCALED, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    AddonRenderPipelineManager.render2D(LoVisualRenderStage.HUD_RAW, phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    Renderer2D.COLOR.render();
                }

                // Scaled fixed HUD pass
                if (scaledMain) {
                    ViewportContext.beginScaled(ctx);
                    Renderer2D.COLOR.begin();
                    StaticHudElementRegistry.renderAllEngine(phase, HudRenderSpace.SCALED, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    AddonRenderPipelineManager.render2D(LoVisualRenderStage.HUD_SCALED, phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    Renderer2D.COLOR.render();
                }

                // Logical HUD pass
                if (logicalMain) {
                    ViewportContext.beginUnscaledLogical(ctx);
                    Renderer2D.COLOR.begin();
                    StaticHudElementRegistry.renderAllEngine(phase, HudRenderSpace.UNSCALED_LOGICAL, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    DraggableHudElementRegistry.renderAllEngine(phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    AddonRenderPipelineManager.render2D(LoVisualRenderStage.HUD_LOGICAL, phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    Renderer2D.COLOR.render();
                }

                // Raw foreground
                if (rawForeground) {
                    ViewportContext.beginUnscaled(ctx);
                    Renderer2D.COLOR.begin();
                    ModuleManager.renderHudEngineForeground(phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    StaticHudElementRegistry.renderAllEngineForeground(phase, HudRenderSpace.UNSCALED, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    AddonRenderPipelineManager.render2D(LoVisualRenderStage.HUD_RAW_FOREGROUND, phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    Renderer2D.COLOR.render();
                }

                // Scaled fixed HUD foreground
                if (scaledForeground) {
                    ViewportContext.beginScaled(ctx);
                    Renderer2D.COLOR.begin();
                    StaticHudElementRegistry.renderAllEngineForeground(phase, HudRenderSpace.SCALED, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    AddonRenderPipelineManager.render2D(LoVisualRenderStage.HUD_SCALED_FOREGROUND, phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    Renderer2D.COLOR.render();
                }

                // Logical foreground
                if (logicalForeground) {
                    ViewportContext.beginUnscaledLogical(ctx);
                    Renderer2D.COLOR.begin();
                    StaticHudElementRegistry.renderAllEngineForeground(phase, HudRenderSpace.UNSCALED_LOGICAL, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    DraggableHudElementRegistry.renderAllEngineForeground(phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    AddonRenderPipelineManager.render2D(LoVisualRenderStage.HUD_LOGICAL_FOREGROUND, phase, Renderer2D.COLOR, textRenderer, ctx, tickProgress);
                    Renderer2D.COLOR.render();
                }
            } finally {
                SystemCursor.endFrame();
                ViewportContext.end(ctx);
            }
        });
        return true;
    }

    private static void renderHudAfterHotbar(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        if (!Lifecycle.canRunHud()) return;
        boolean swapTooltipWork = SwapTooltip.hasWork();
        boolean targetHudWork = DraggableHudElementRegistry.hasEngineWidgetWork("target_hud");
        if (!swapTooltipWork && !targetHudWork) return;

        float tickProgress = TickDelta.tickProgress(tickCounter, false);
        TextRenderer textRenderer = TextRenderer.get();
        Renderer2D.Deferred2DLayer layer = Renderer2D.Deferred2DLayer.HUD_AFTER_MISC_OVERLAYS;

        FullScreenRenderer.ensureInit();
        ViewportContext.beginUnscaled(ctx);
        refreshHudFrameTiming(tickCounter);

        Renderer2D.withDeferredLayer(layer, () -> {
            SystemCursor.beginFrame(ctx);
            try (RenderPhaseScope hudPhase = LoVisualRenderSystem.phase(RenderPhase.HUD_EFFECTS, "2d:after_hotbar")) {
                // Raw pass
                if (swapTooltipWork) {
                    try (ProfilerPhase.Scope tooltipsScope = ProfilerPhase.scope("2d:after_hotbar:swap_tooltip");
                         RenderProfiler2D.Section ignoredTooltips = RenderProfiler2D.section("swap_tooltip")) {
                        Renderer2D.COLOR.begin();
                        SwapTooltip.render();
                        Renderer2D.COLOR.render();
                    }
                }

                // Logical pass
                if (targetHudWork) {
                    ViewportContext.beginUnscaledLogical(ctx);
                    try (ProfilerPhase.Scope targetHudScope = ProfilerPhase.scope("2d:after_hotbar:target_hud");
                         RenderProfiler2D.Section ignoredTargetHud = RenderProfiler2D.section("target_hud")) {
                        Renderer2D.COLOR.begin();
                        DraggableHudElementRegistry.renderEngineWidget(
                                "target_hud",
                                Renderer2D.COLOR,
                                textRenderer,
                                ctx,
                                tickProgress
                        );
                        Renderer2D.COLOR.render();
                    }
                }
            } finally {
                SystemCursor.endFrame();
                ViewportContext.end(ctx);
            }
        });
        appendHudDeferredMarker(layer, ctx);
    }

    private static void appendHudDeferredMarker(Renderer2D.Deferred2DLayer layer, GuiGraphicsExtractor ctx) {
        if (!(ctx instanceof IGuiGraphics graphics)) return;
        ctx.nextStratum();
        graphics.lovisual$addDeferredHudMarker(layer);
        ctx.nextStratum();
    }

    private static Renderer2D.Deferred2DLayer deferredLayerForHudPhase(HudPhase phase) {
        if (phase == null) return Renderer2D.Deferred2DLayer.BEFORE_VANILLA_GUI;
        return switch (phase) {
            case NONE -> Renderer2D.Deferred2DLayer.BEFORE_VANILLA_GUI;
            case FIRST -> Renderer2D.Deferred2DLayer.HUD_FIRST;
            case BEFORE_MISC_OVERLAYS -> Renderer2D.Deferred2DLayer.HUD_BEFORE_MISC_OVERLAYS;
            case AFTER_MISC_OVERLAYS -> Renderer2D.Deferred2DLayer.HUD_AFTER_MISC_OVERLAYS;
            case AFTER_BOSS_BAR -> Renderer2D.Deferred2DLayer.HUD_AFTER_BOSS_BAR;
            case BEFORE_DEMO_TIMER -> Renderer2D.Deferred2DLayer.HUD_BEFORE_DEMO_TIMER;
            case BEFORE_CHAT -> Renderer2D.Deferred2DLayer.HUD_BEFORE_CHAT;
            case AFTER_SUBTITLES -> Renderer2D.Deferred2DLayer.HUD_AFTER_SUBTITLES;
            case LAST -> Renderer2D.Deferred2DLayer.HUD_LAST;
        };
    }

    private static boolean hasHudMainPassWork(HudPhase phase, HudRenderSpace space) {
        boolean moduleWork = space == HudRenderSpace.UNSCALED && ModuleManager.hasHudEngineWork(phase);
        boolean staticWork = StaticHudElementRegistry.hasEngineWork(phase, space, false);
        boolean draggableWork = space == HudRenderSpace.UNSCALED_LOGICAL && DraggableHudElementRegistry.hasEngineWork(phase, false);
        return moduleWork || staticWork || draggableWork;
    }

    private static boolean hasHudForegroundPassWork(HudPhase phase, HudRenderSpace space) {
        boolean moduleWork = space == HudRenderSpace.UNSCALED && ModuleManager.hasHudEngineForegroundWork(phase);
        boolean staticWork = StaticHudElementRegistry.hasEngineWork(phase, space, true);
        boolean draggableWork = space == HudRenderSpace.UNSCALED_LOGICAL && DraggableHudElementRegistry.hasEngineWork(phase, true);
        return moduleWork || staticWork || draggableWork;
    }

    @Override
    public void onInitializeClient() {
        LoVisualRenderEngineBootstrap.init();
        PostProcessManager.register(new NetherPortalRiftPass());
        PostProcessManager.register(new SleepOverlayPass());
        MainConfig.get(); // Load global settings (debug flag)
        AccountConfig.get();
        ProxyBackend.init();
        CommandManager.init();
        MediaSessionService.get().init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MediaSessionService.get().shutdown());

        // -------- CONFIG + BINDS ----------
        // Enabled addons declare built-in module exclusions before auto-discovery.
        AddonManager.prepareModuleLoading();
        ModuleAutoLoader.load("dev.loki.lovisual.features.module.modules");
        AddonManager.init();
        // Initialize modules' enabled state and settings from new config system
        ModuleManager.loadAllModuleConfigs();
        AddonManager.notifyClientReady();
        // Key binds are restored via Module config (KeyBindSetting) in ModuleManager.loadAllModuleConfigs()
        HudElements.init(); // init HUD config + widgets
        StaticHudElementBootstrap.init(); // init vanilla HUD elements config
        Events.BUS.register(I18nPreflightContributors.INSTANCE);
        Events.BUS.register(RenderPrewarmContributors.INSTANCE);
        Events.BUS.register(RotationManager.INSTANCE);
        Events.BUS.register(AttackPressing.INSTANCE);
        Events.BUS.register(InventorySwap.INSTANCE);
        Events.BUS.register(SprintController.INSTANCE);
        Events.BUS.register(AntiBotTracker.INSTANCE);
        Events.BUS.register(CombatProtocolHeuristics.INSTANCE);
        Events.BUS.register(PvpTracker.INSTANCE);
        Events.BUS.register(PvpTargetTracker.INSTANCE);
        Events.BUS.register(SelfBedTracker.INSTANCE);
        Events.BUS.register(BoatSimulationCache.INSTANCE);
        Events.BUS.register(PlayerSimulationCache.INSTANCE);
        Events.BUS.register(BlinkManager.INSTANCE);
        Events.BUS.register(FakeLagController.INSTANCE);
        Events.BUS.register(BacktrackController.INSTANCE);
        Events.BUS.register(TimerController.INSTANCE);

        // -------- КЛЮЧЕВЫЕ ИВЕНТЫ --------

        // === ТИКИ ===

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!Lifecycle.isActive()) return;
            Events.BUS.post(new GameTickEvent());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!Lifecycle.isActive()) return;

            if (!ClickGuiRenderer.isBlockingModuleKeybinds()) {
                ModuleManager.handleModuleKeybinds();
            }
            TargetManager.tick();
            ModuleManager.tickAll();
            DraggableHudElementRegistry.tickAll();
            StaticHudElementRegistry.tickAll();
        });

        /* ====================== WORLD RENDER ====================== */

        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> renderWorldPhase(
                WorldPhase.BEFORE_ENTITIES,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        LevelRenderEvents.AFTER_SOLID_FEATURES.register(ctx -> renderWorldPhase(
                WorldPhase.AFTER_ENTITIES,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(ctx -> renderWorldPhase(
                WorldPhase.BEFORE_TRANSLUCENT,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        LevelRenderEvents.END_MAIN.register(ctx -> renderWorldPhase(
                WorldPhase.END_MAIN,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        // === HUD 2D OVERLAYS ===

        HudElementRegistry.addFirst(
                Identifier.fromNamespaceAndPath("lovisual", "hud_first"),
                (ctx, tc) -> renderHudPhase(HudPhase.FIRST, ctx, tc)
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("lovisual", "hud_last"),
                (ctx, tc) -> renderHudPhase(HudPhase.LAST, ctx, tc)
        );

        // остальные позиции HUD:
        HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("lovisual", "hud_before_misc"),
                (ctx, tc) -> renderHudPhase(HudPhase.BEFORE_MISC_OVERLAYS, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_misc"),
                (ctx, tc) -> renderHudPhase(HudPhase.AFTER_MISC_OVERLAYS, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.SLEEP,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_boss"),
                (ctx, tc) -> renderHudPhase(HudPhase.AFTER_BOSS_BAR, ctx, tc)
        );

        HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.DEMO_TIMER,
                Identifier.fromNamespaceAndPath("lovisual", "hud_before_demo"),
                (ctx, tc) -> renderHudPhase(HudPhase.BEFORE_DEMO_TIMER, ctx, tc)
        );

        HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("lovisual", "hud_before_chat"),
                (ctx, tc) -> renderHudPhase(HudPhase.BEFORE_CHAT, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_subtitles"),
                (ctx, tc) -> renderHudPhase(HudPhase.AFTER_SUBTITLES, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_hotbar"),
                (ctx, tc) -> renderHudAfterHotbar(ctx, tc)
        );

        Lifecycle.activate();
        RenderPrewarmManager.prewarm("client initialized");
    }
}
