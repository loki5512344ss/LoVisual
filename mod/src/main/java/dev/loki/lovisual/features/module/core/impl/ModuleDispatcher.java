package dev.loki.lovisual.features.module.core;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.addon.manager.ModuleExtensionManager;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.render.RenderProfiler2D;
import dev.loki.lovisual.render.engine.profiler.render.RenderProfiler3D;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.input.KeyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Per-frame dispatch loops backing {@link ModuleManager}. Iterates the array
 * snapshots maintained by {@link ModuleRegistrar}, gated by {@link Lifecycle}
 * and the add-on {@link ModuleExtensionManager} hooks, and wraps engine
 * render calls in 2D / 3D profiler scopes. Sibling folder sharing the parent's
 * package so {@link ModuleManager}'s package-private snapshots stay reachable.
 */
final class ModuleDispatcher {

    private ModuleDispatcher() { }

    /** Game tick: runs {@link Module#onTick()} on every enabled module when a player is loaded. */
    static void tickAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Lifecycle.isActive()) return;
        for (Module m : ModuleManager.modulesSnapshot) {
            if (m.isEnabled() && ModuleExtensionManager.beforeTick(m)) {
                m.onTick();
                ModuleExtensionManager.afterTick(m);
            }
        }
    }

    /** Interpolation-only pass that runs regardless of world state; drives animation clocks. */
    static void frameAll(float frameDeltaTicks) {
        if (!Lifecycle.isActive()) return;
        for (Module m : ModuleManager.modulesSnapshot) {
            if (m.isEnabled() && ModuleExtensionManager.beforeFrame(m, frameDeltaTicks)) {
                m.onFrame(frameDeltaTicks);
                ModuleExtensionManager.afterFrame(m, frameDeltaTicks);
            }
        }
    }

    /** Legacy 2D HUD pass (Renderer2D not used); iterates modules bound to {@code phase}. */
    static void renderHud(HudPhase phase, GuiGraphicsExtractor ctx, float tickDelta) {
        if (!Lifecycle.canRunHud()) return;
        Module[] phaseModules = ModuleManager.HUD_PHASE_SNAPSHOTS.get(phase);
        if (phaseModules == null) return;
        for (Module m : phaseModules) if (m.isEnabled()) m.onRender2D(ctx, tickDelta);
    }

    /** Engine 2D HUD pass with per-module profiler scopes and add-on extension hooks. */
    static void renderHudEngine(HudPhase phase, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        if (!Lifecycle.canRunHud()) return;
        Module[] phaseModules = ModuleManager.HUD_PHASE_SNAPSHOTS.get(phase);
        if (phaseModules == null) return;
        for (Module m : phaseModules) {
            if (!m.isEnabled()) continue;
            try (ProfilerPhase.Scope _ = ProfilerPhase.scope("2d:module:", m.name());
                 RenderProfiler2D.Section ignored = RenderProfiler2D.section("module:", m.name())) {
                if (ModuleExtensionManager.beforeHudRender(m, renderer, textRenderer, ctx, tickDelta)) {
                    m.onRenderHudEngine(renderer, textRenderer, ctx, tickDelta);
                    ModuleExtensionManager.afterHudRender(m, renderer, textRenderer, ctx, tickDelta);
                }
            }
        }
    }

    /** Foreground (post-HUD) engine pass; runs unconditionally on every enabled module in {@code phase}. */
    static void renderHudEngineForeground(HudPhase phase, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        if (!Lifecycle.canRunHud()) return;
        Module[] phaseModules = ModuleManager.HUD_PHASE_SNAPSHOTS.get(phase);
        if (phaseModules == null) return;
        for (Module m : phaseModules) {
            if (!m.isEnabled()) continue;
            try (ProfilerPhase.Scope phaseScope = ProfilerPhase.scope("2d:module_fg:", m.name());
                 RenderProfiler2D.Section ignored = RenderProfiler2D.section("module_fg:", m.name())) {
                m.onRenderHudEngineForeground(renderer, textRenderer, ctx, tickDelta);
            }
        }
    }

    /** Vanilla 3D world pass; iterates modules bound to {@code phase}. */
    static void renderWorld(WorldPhase phase, PoseStack matrices, SubmitNodeCollector consumers, float tickDelta) {
        if (!Lifecycle.canRunRender()) return;
        Module[] phaseModules = ModuleManager.WORLD_PHASE_SNAPSHOTS.get(phase);
        if (phaseModules == null) return;
        for (Module m : phaseModules) if (m.isEnabled()) m.onRenderWorld(matrices, consumers, tickDelta);
    }

    /** Engine 3D world pass with per-module profiler scopes and add-on extension hooks. */
    static void renderWorldEngine(WorldPhase phase, Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        if (!Lifecycle.canRunRender()) return;
        Module[] phaseModules = ModuleManager.WORLD_PHASE_SNAPSHOTS.get(phase);
        if (phaseModules == null) return;
        for (Module m : phaseModules) {
            if (!m.isEnabled()) continue;
            try (ProfilerPhase.Scope phaseScope = ProfilerPhase.scope("3d:module:", m.name());
                 RenderProfiler3D.Section ignored = RenderProfiler3D.section("module:", m.name())) {
                if (ModuleExtensionManager.beforeWorldRender(m, renderer, depthRenderer, tickDelta)) {
                    m.onRenderWorldEngine(renderer, depthRenderer, tickDelta);
                    ModuleExtensionManager.afterWorldRender(m, renderer, depthRenderer, tickDelta);
                }
            }
        }
    }

    /** True when at least one enabled module is bound to {@code phase}; used to skip the whole pass. */
    static boolean hasEnabledHudPhaseModule(HudPhase phase) {
        if (!Lifecycle.canRunHud()) return false;
        Module[] phaseModules = ModuleManager.HUD_PHASE_SNAPSHOTS.get(phase);
        if (phaseModules == null) return false;
        for (Module m : phaseModules) if (m.isEnabled()) return true;
        return false;
    }

    /** Toggle every module whose keybind was pressed this tick; suppresses the click sound on all but the first. */
    static void handleModuleKeybinds() {
        if (!Lifecycle.isActive()) return;
        boolean soundSuppressedThisPass = false;
        for (Module m : ModuleManager.modulesSnapshot) {
            if (KeyManager.wasPressed(m.name())) {
                ModuleManager.setSuppressToggleSound(soundSuppressedThisPass);
                m.toggleFromKeybind();
                soundSuppressedThisPass = true;
            }
        }
        ModuleManager.setSuppressToggleSound(false);
    }
}
