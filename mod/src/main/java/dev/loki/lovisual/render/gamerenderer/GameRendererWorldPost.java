package dev.loki.lovisual.render.gamerenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import dev.loki.lovisual.addon.pipeline.AddonRenderPipelineManager;
import dev.loki.lovisual.api.v0.render.LoVisualRenderStage;
import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.depth.WorldSceneDepth;
import dev.loki.lovisual.render.engine.msaa.MsaaWorldTarget;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.postprocess.render.MenuBackgroundRenderer;
import dev.loki.lovisual.render.engine.profiler.bridge.TracyGpuProfiler;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.sched.HudDeferredGuiElement;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.visuals.LoVisualVisuals;
import dev.loki.lovisual.render.iris.compat.IrisCompatibilityGuards;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.util.screen.ClientScreen;

/**
 * Pre/post-hand post-processing and HUD drain bodies extracted from GameRendererMixin.
 */
public final class GameRendererWorldPost {

    private final GameRendererWorldEngine engine;

    public GameRendererWorldPost(GameRendererWorldEngine engine) {
        this.engine = engine;
    }

    public void preHand(GameRendererWorldEngine.Host host, DeltaTracker tickCounter) {
        if (IrisRuntime.isShaderpackRendererActive()
                && IrisCompatibilityGuards.deferIrisFinalizationForSecondHandScene()) {
            return;
        }
        ProfilerPhase.begin("3d:post_pre_hand");
        try (TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("3d:post_pre_hand")) {
            Minecraft minecraft = host.lovisual$minecraft();
            if (minecraft != null) {
                boolean needsResolvedDepth = !IrisRuntime.isShaderpackRendererActive()
                        && host.lovisual$projection().needsResolvedMainDepth();
                boolean capturedMsaaDepth = needsResolvedDepth
                        && MsaaWorldTarget.isActive()
                        && WorldSceneDepth.captureResolvedMain(MsaaWorldTarget.getMsaaFramebuffer());
                MsaaWorldTarget.resolveToMain(minecraft);
                if (needsResolvedDepth && !capturedMsaaDepth) {
                    WorldSceneDepth.captureResolvedMain(minecraft.gameRenderer.mainRenderTarget());
                }
                LoVisualVisuals.renderWorldBase(minecraft, tickCounter.getGameTimeDeltaPartialTick(true));
            }
            try (RenderPhaseScope phase = LoVisualRenderSystem.phase(RenderPhase.WORLD_POST_PRE_HAND)) {
                PostProcessManager.renderAll(PostProcessPass.Phase.PRE_HAND,
                        tickCounter.getGameTimeDeltaPartialTick(true));
                renderWorldAfterPreHand(host, tickCounter);
            }
        }
        ProfilerPhase.end("3d:post_pre_hand");
    }

    private void renderWorldAfterPreHand(GameRendererWorldEngine.Host host, DeltaTracker tickCounter) {
        Minecraft minecraft = host.lovisual$minecraft();
        if (minecraft == null) {
            engine.renderWorld(host, tickCounter);
            engine.renderPostWorld(host, tickCounter);
            return;
        }

        MsaaWorldTarget.begin(minecraft, MainConfig.get().getMsaa3dSamples(), false, true);
        try {
            engine.renderWorld(host, tickCounter);
            engine.renderPostWorld(host, tickCounter);
        } finally {
            MsaaWorldTarget.resolveToMain(minecraft);
        }
    }

    public void afterHand(GameRendererWorldEngine.Host host, DeltaTracker tickCounter) {
        ProfilerPhase.begin("3d:post_after_hand");
        try (TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("3d:post_after_hand")) {
            Minecraft minecraft = host.lovisual$minecraft();
            if (minecraft != null) {
                MsaaWorldTarget.resolveToMain(minecraft);
            }
            try (RenderPhaseScope phase = LoVisualRenderSystem.phase(RenderPhase.WORLD_POST_HAND)) {
                PostProcessManager.renderAll(PostProcessPass.Phase.POST_HAND,
                        tickCounter.getGameTimeDeltaPartialTick(true));
            }
        }
        ProfilerPhase.end("3d:post_after_hand");
    }

    public void beforeVanillaGui(Minecraft minecraft, DeltaTracker tickCounter) {
        MenuBackgroundRenderer.drainDeferred(minecraft);
        Renderer2D.captureWorldGlassSource();
        if (AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.SCREEN_BEFORE_VANILLA_GUI)) {
            float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
            Renderer2D.COLOR.begin();
            AddonRenderPipelineManager.render2D(LoVisualRenderStage.SCREEN_BEFORE_VANILLA_GUI,
                    null, Renderer2D.COLOR, TextRenderer.get(), null, tickDelta);
            Renderer2D.COLOR.render();
        }
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.BEFORE_VANILLA_GUI);
        drainHudLayersBelowScreenGui();
    }

    public void afterVanillaGui(DeltaTracker tickCounter) {
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.AFTER_VANILLA_GUI);
        if (AddonRenderPipelineManager.hasActiveCallbacks(LoVisualRenderStage.SCREEN_AFTER_VANILLA_GUI)) {
            float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
            Renderer2D.COLOR.begin();
            AddonRenderPipelineManager.render2D(LoVisualRenderStage.SCREEN_AFTER_VANILLA_GUI,
                    null, Renderer2D.COLOR, TextRenderer.get(), null, tickDelta);
            Renderer2D.COLOR.render();
        }
    }

    /**
     * Gui.extractRenderState() extracts vanilla HUD first and the current Screen after it.
     * Normal gameplay keeps LoVisual HUD spliced into that HUD order through HudDeferredGuiElement
     * markers. When a real Screen is open, however, the screen must remain the top GUI owner:
     * HUD/world overlays are drained before GuiRenderer.render(),
     * preserving their internal order while keeping container/inventory GUI above them.
     * Explicit SCREEN_TOP/AFTER_VANILLA_GUI overlays (Dynamic Island, ClickGui top layer, etc.) are
     * intentionally not drained here.
     */
    private static void drainHudLayersBelowScreenGui() {
        if (ClientScreen.current() == null) {
            return;
        }

        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_FIRST);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_BEFORE_MISC_OVERLAYS);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_AFTER_MISC_OVERLAYS);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_AFTER_BOSS_BAR);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_BEFORE_DEMO_TIMER);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_BEFORE_CHAT);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_AFTER_SUBTITLES);
        Renderer2D.drainDeferred2D(Renderer2D.Deferred2DLayer.HUD_LAST);
    }
}
