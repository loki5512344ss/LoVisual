package dev.loki.lovisual;

import dev.loki.lovisual.events.impl.render.GameTickEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.addon.core.AddonManager;
import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.config.style.StyleConfig;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.command.core.CommandManager;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.i18n.I18nPreflightContributors;
import dev.loki.lovisual.features.gui.hud.registry.HudElements;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.nondraggable.StaticHudElementBootstrap;
import dev.loki.lovisual.features.gui.hud.nondraggable.StaticHudElementRegistry;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.features.module.lifecycle.ModuleAutoLoader;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.platform.serverpolicy.ServerPolicyClient;
import dev.loki.lovisual.render.effects.NetherPortalRiftPass;
import dev.loki.lovisual.render.effects.SleepOverlayPass;
import dev.loki.lovisual.render.engine.LoVisualRenderEngineBootstrap;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.prewarm.RenderPrewarmContributors;
import dev.loki.lovisual.render.engine.prewarm.RenderPrewarmManager;
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
import dev.loki.lovisual.util.player.simulation.PlayerSimulationCache;
import dev.loki.lovisual.util.pvp.tracker.PvpTargetTracker;
import dev.loki.lovisual.util.pvp.tracker.PvpTracker;
import dev.loki.lovisual.util.target.TargetManager;
import dev.loki.lovisual.util.time.TimerController;

public class LoVisual implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LoVisualRenderEngineBootstrap.init();
        PostProcessManager.register(new NetherPortalRiftPass());
        PostProcessManager.register(new SleepOverlayPass());
        MainConfig.get(); // Load global settings (debug flag)
        AccountConfig.get();
        ProxyBackend.init();
        CommandManager.init();
        ServerPolicyClient.init();
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
                if (StyleConfig.get().handleCycleKey()) {
                    Notifier.info("Surface style: " + StyleConfig.get().style().id());
                }
            }
            TargetManager.tick();
            ModuleManager.tickAll();
            DraggableHudElementRegistry.tickAll();
            StaticHudElementRegistry.tickAll();
        });

        /* ====================== WORLD RENDER ====================== */

        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> HudRender.renderWorldPhase(
                WorldPhase.BEFORE_ENTITIES,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        LevelRenderEvents.AFTER_SOLID_FEATURES.register(ctx -> HudRender.renderWorldPhase(
                WorldPhase.AFTER_ENTITIES,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(ctx -> HudRender.renderWorldPhase(
                WorldPhase.BEFORE_TRANSLUCENT,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        LevelRenderEvents.END_MAIN.register(ctx -> HudRender.renderWorldPhase(
                WorldPhase.END_MAIN,
                ctx.poseStack(),
                ctx.submitNodeCollector()
        ));

        // === HUD 2D OVERLAYS ===

        HudElementRegistry.addFirst(
                Identifier.fromNamespaceAndPath("lovisual", "hud_first"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.FIRST, ctx, tc)
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("lovisual", "hud_last"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.LAST, ctx, tc)
        );

        // остальные позиции HUD:
        HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("lovisual", "hud_before_misc"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.BEFORE_MISC_OVERLAYS, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_misc"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.AFTER_MISC_OVERLAYS, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.SLEEP,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_boss"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.AFTER_BOSS_BAR, ctx, tc)
        );

        HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.DEMO_TIMER,
                Identifier.fromNamespaceAndPath("lovisual", "hud_before_demo"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.BEFORE_DEMO_TIMER, ctx, tc)
        );

        HudElementRegistry.attachElementBefore(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("lovisual", "hud_before_chat"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.BEFORE_CHAT, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_subtitles"),
                (ctx, tc) -> HudRender.renderHudPhase(HudPhase.AFTER_SUBTITLES, ctx, tc)
        );

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath("lovisual", "hud_after_hotbar"),
                (ctx, tc) -> HudRender.renderHudAfterHotbar(ctx, tc)
        );

        Lifecycle.activate();
        RenderPrewarmManager.prewarm("client initialized");
    }
}
