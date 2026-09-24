package dev.loki.lovisual;

import dev.loki.lovisual.addon.pipeline.AddonRenderPipelineManager;
import dev.loki.lovisual.api.v0.render.LoVisualRenderStage;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.nondraggable.StaticHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.SwapTooltip;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.mixininterface.render.IGuiGraphics;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.render.RenderProfiler2D;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import dev.loki.lovisual.render.helpers.util.TickDelta;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * HUD and world-phase rendering driven by the client entry point. Hosts the raw/scaled/logical
 * HUD pass orchestration that {@link LoVisual} registers against the Fabric HUD and level render
 * hooks, so behaviour stays identical to the inlined implementation.
 */
final class HudRender {
    private HudRender() {
    }

    static void renderWorldPhase(WorldPhase phase,
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

    static void renderHudPhase(HudPhase phase, GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
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

    static void renderHudAfterHotbar(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
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
}
