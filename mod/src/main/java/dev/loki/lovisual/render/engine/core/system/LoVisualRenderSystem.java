package dev.loki.lovisual.render.engine.core.system;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.render.engine.compat.immediatelyfast.ImmediatelyFastRuntime;
import dev.loki.lovisual.render.engine.compat.immediatelyfast.ImmediatelyFastRuntimeSnapshot;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.render.iris.runtime.IrisRuntimeSnapshot;
import dev.loki.lovisual.render.sodium.interop.SodiumTerrainInteropStatsSnapshot;
import dev.loki.lovisual.render.sodium.visibility.SodiumVisibilityStatsSnapshot;
import org.joml.Matrix4f;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.policy.LightPolicy;
import dev.loki.lovisual.render.engine.core.policy.fog.VanillaWorldFogProvider;
import dev.loki.lovisual.render.engine.depth.WorldSceneDepth;
import dev.loki.lovisual.render.engine.framegraph.LoVisualFrameGraph;
import dev.loki.lovisual.render.engine.optimize.profile.FrameProfiler;
import dev.loki.lovisual.render.engine.profiler.frame.FrameStutterProfiler;
import dev.loki.lovisual.render.engine.profiler.frame.RenderFrameProfiler;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;
import dev.loki.lovisual.render.engine.rhi.stats.RhiStatsSnapshot;
import dev.loki.lovisual.render.engine.rhi.backend.SodiumGlBackend;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.LoVisualVulkanBackend;
import dev.loki.lovisual.render.engine.rhi.resource.manager.RenderResourceManager;
import dev.loki.lovisual.render.engine.rhi.resource.manager.RenderResourceStatsSnapshot;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;
import dev.loki.lovisual.render.engine.rhi.uniform.stats.UniformAllocatorStatsSnapshot;
import dev.loki.lovisual.render.engine.text.command.TextCommandStatsSnapshot;
import dev.loki.lovisual.render.engine.text.render.TextRenderSystem;
import dev.loki.lovisual.render.engine.world.stats.WorldRenderStatsSnapshot;
import dev.loki.lovisual.render.sodium.bridge.SodiumFrameContext;
import dev.loki.lovisual.render.sodium.bridge.SodiumRenderBridge;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.Locale;
import dev.loki.lovisual.render.engine.core.context.CameraContext;
import dev.loki.lovisual.render.engine.core.context.FramebufferContext;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.core.frame.FrameLifecycle;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;

/**
 * Single owner of LoVisual render lifecycle.
 * - one LoVisual frame per Minecraft-presented frame;
 * - many scoped RenderPhase entries inside that frame;
 * - backend submission is flushed before framePresented(), not by random renderer classes;
 * - RenderState is a compatibility shim, not the lifecycle owner.
 */
public enum LoVisualRenderSystem {
    ;
    private static final SodiumRenderBridge SODIUM = new SodiumRenderBridge();
    private static final LoVisualFrameGraph FRAME_GRAPH = new LoVisualFrameGraph();
    private static final LoVisualUniformAllocator UNIFORMS = new LoVisualUniformAllocator();

    private static LoVisualRhi rhi;
    private static BackendKind backendKind = BackendKind.UNKNOWN;
    private static RenderFrameContext currentContext;
    private static long frameId;
    private static boolean initialized;
    private static boolean frameOpen;
    private static boolean submissionEnded;
    private static FrameLifecycle lifecycle = FrameLifecycle.IDLE;

    private enum BackendKind {
        UNKNOWN,
        GL,
        VULKAN
    }

    public static void init() {
        if (initialized) {
            ensureBackendMatchesDevice();
            return;
        }
        BackendKind desired = detectBackendKind();
        backendKind = desired == BackendKind.UNKNOWN ? BackendKind.GL : desired;
        rhi = createBackend(backendKind);
        initialized = true;
        DebugLog.renderThreadOnChange(
                "lovisual.rhi.backend",
                backendKind,
                "[LoVisualRHI] backend initialized: %s",
                backendKind
        );
        ensureBackendMatchesDevice();
    }

    private static LoVisualRhi createBackend(BackendKind kind) {
        if (kind == BackendKind.VULKAN) {
            return new LoVisualVulkanBackend();
        }
        return new SodiumGlBackend();
    }

    private static void ensureBackendMatchesDevice() {
        if (!initialized || rhi == null) return;
        BackendKind desired = detectBackendKind();
        if (desired == BackendKind.UNKNOWN || desired == backendKind) return;

        if (frameOpen) {
            DebugLog.warnOnChange(
                    "lovisual.rhi.backend.switch.deferred",
                    backendKind + "->" + desired + "|frameOpen",
                    "[LoVisualRHI] backend switch deferred until frame end: %s -> %s",
                    backendKind,
                    desired
            );
            return;
        }

        LoVisualRhi previous = rhi;
        BackendKind previousKind = backendKind;
        LoVisualRhi next;
        try {
            next = createBackend(desired);
        } catch (Throwable t) {
            DebugLog.error("[LoVisualRHI] failed to create backend " + desired, t);
            return;
        }

        rhi = next;
        backendKind = desired;
        try {
            previous.close();
        } catch (Throwable t) {
            DebugLog.warnOnChange(
                    "lovisual.rhi.backend.old.close.failed",
                    previousKind + "|" + desired + "|" + t.getClass().getSimpleName(),
                    "[LoVisualRHI] old backend close failed during switch %s -> %s: %s: %s",
                    previousKind,
                    desired,
                    t.getClass().getSimpleName(),
                    t.getMessage()
            );
        }
        DebugLog.renderThreadOnChange(
                "lovisual.rhi.backend",
                backendKind,
                "[LoVisualRHI] backend switched: %s -> %s",
                previousKind,
                backendKind
        );
    }

    private static BackendKind detectBackendKind() {
        String backendName = null;
        try {
            var device = RenderSystem.tryGetDevice();
            if (device != null && device.getDeviceInfo() != null) {
                backendName = device.getDeviceInfo().backendName();
            }
        } catch (Throwable ignored) {
            // Fall through to RenderSystem backend description.
        }

        if (backendName == null || backendName.isBlank()) {
            try {
                backendName = RenderSystem.getBackendDescription();
            } catch (Throwable ignored) {
                backendName = null;
            }
        }

        if (backendName == null || backendName.isBlank()) return BackendKind.UNKNOWN;
        String normalized = backendName.toLowerCase(Locale.ROOT);
        if (normalized.contains("vulkan")) return BackendKind.VULKAN;
        if (normalized.contains("opengl") || normalized.contains("gl")) return BackendKind.GL;
        return BackendKind.UNKNOWN;
    }

    public static LoVisualRhi rhi() {
        init();
        ensureBackendMatchesDevice();
        return rhi;
    }

    public static SodiumRenderBridge sodium() {
        return SODIUM;
    }

    public static RenderResourceManager resources() {
        return rhi().resources();
    }

    public static LoVisualFrameGraph frameGraph() {
        return FRAME_GRAPH;
    }

    public static LoVisualUniformAllocator uniforms() {
        return UNIFORMS;
    }

    public static RenderFrameContext currentContext() {
        return currentContext;
    }

    public static FrameLifecycle lifecycle() {
        return lifecycle;
    }

    /**
     * Opens the LoVisual frame if needed, otherwise refreshes camera/viewport data without advancing frameId.
     */
    public static RenderFrameContext beginFrame(float tickDelta, Matrix4f projection, Matrix4f modelView) {
        return beginFrame(tickDelta, tickDelta, tickDelta, projection, modelView);
    }

    public static RenderFrameContext beginFrame(float tickProgress,
                                                float frameDeltaTicks,
                                                float fixedDeltaTicks,
                                                Matrix4f projection,
                                                Matrix4f modelView) {
        LoVisualRhi activeRhi = rhi();
        SodiumFrameContext sodiumFrame;
        if (!frameOpen) {
            frameId++;
            activeRhi.beginFrame(frameId);
            UNIFORMS.beginFrame(frameId);
            TextRenderSystem.beginFrame();
            sodiumFrame = SODIUM.beginFrame();
            RenderFrameProfiler.beginFrame(frameId);
            FrameProfiler.beginFrame();
            frameOpen = true;
            submissionEnded = false;
            lifecycle = FrameLifecycle.RECORDING;
        } else {
            sodiumFrame = SODIUM.currentFrameContext();
        }

        RenderPhase phase = currentContext != null ? currentContext.phase() : RenderPhase.NONE;
        RenderFrameContext ctx = new RenderFrameContext(
                frameId,
                tickProgress,
                frameDeltaTicks,
                fixedDeltaTicks,
                phase,
                CameraContext.capture(projection, modelView),
                ViewportContext.capture(),
                FramebufferContext.capture(),
                WorldSceneDepth::mainDepthView,
                VanillaWorldFogProvider.INSTANCE,
                LightPolicy.VANILLA,
                SODIUM.visibilityProvider(),
                sodiumFrame
        );
        currentContext = ctx;
        RenderState.applyContext(ctx);
        return ctx;
    }

    /**
     * Refreshes only timing data for phases that do not own the world camera/projection
     * themselves, for example Fabric HUD callbacks.
     */
    public static RenderFrameContext updateFrameTiming(float tickProgress,
                                                       float frameDeltaTicks,
                                                       float fixedDeltaTicks) {
        RenderFrameContext base = ensureFrameContext();
        currentContext = base.withTiming(tickProgress, frameDeltaTicks, fixedDeltaTicks);
        RenderState.applyContext(currentContext);
        return currentContext;
    }

    public static RenderFrameContext ensureFrameContext() {
        if (currentContext != null) return currentContext;
        return beginFrame(RenderState.tickProgress, RenderState.frameDeltaTicks, RenderState.fixedDeltaTicks, new Matrix4f(), new Matrix4f(RenderSystem.getModelViewStack()));
    }

    public static RenderPhaseScope phase(RenderPhase phase) {
        return phase(phase, null);
    }

    public static RenderPhaseScope phase(RenderPhase phase, String label) {
        RenderFrameContext before = currentContext;
        RenderFrameContext base = ensureFrameContext();
        RenderPhase next = phase == null ? RenderPhase.NONE : phase;
        currentContext = base.withPhase(next);
        RenderState.applyContext(currentContext);
        return new RenderPhaseScope(next, before, RenderFrameProfiler.phase(next, label));
    }

    public static void restorePhase(RenderFrameContext previous) {

        currentContext = previous;
        if (currentContext != null) {
            RenderState.applyContext(currentContext);
        } else {
            RenderState.clearContextBackedState();
        }
    }

    public static void endRenderSubmission() {
        if (!initialized || !frameOpen || submissionEnded) return;
        try {
            TextRenderSystem.flush();
            rhi.endRenderSubmission();
            lifecycle = FrameLifecycle.SUBMITTED;
        } finally {
            submissionEnded = true;
        }
    }

    /**
     * Called after the window surface has been presented. Rendering submission must already
     * be closed before GpuSurface.blitFromTexture(), otherwise late LoVisual draws miss the
     * presented frame and can leave stale main-target contents for the next frame.
     */
    public static void onFramePresented() {
        if (!initialized) return;
        try {
            if (!submissionEnded) {
                // Fallback for unusual renderFrame paths. Normal visible frames close in
                // GpuSurfaceMixin#lovisual$beforeSurfaceBlit, before the surface copy.
                endRenderSubmission();
            }
            rhi.framePresented();
            UNIFORMS.onFramePresented();
            if (FrameStutterProfiler.isEnabled()) {
                FrameStutterProfiler.onFramePresented(rhiStatsSnapshot(), uniformStatsSnapshot(), resourceStatsSnapshot());
            }
            RenderFrameProfiler.endFrame(null, null);
            FrameProfiler.endFrame();
            lifecycle = FrameLifecycle.PRESENTED;
        } finally {
            currentContext = null;
            frameOpen = false;
            submissionEnded = false;
            lifecycle = FrameLifecycle.IDLE;
            RenderState.clearContextBackedState();
        }
    }

    public static RhiStatsSnapshot rhiStatsSnapshot() {
        return rhi().stats().snapshot();
    }

    public static SodiumVisibilityStatsSnapshot sodiumVisibilityStatsSnapshot() {
        return SODIUM.visibilityStatsSnapshot();
    }

    public static SodiumTerrainInteropStatsSnapshot sodiumTerrainStatsSnapshot() {
        return SODIUM.terrainInterop().statsSnapshot();
    }

    public static IrisRuntimeSnapshot irisSnapshot() {
        return IrisRuntime.snapshot();
    }

    public static ImmediatelyFastRuntimeSnapshot immediatelyFastSnapshot() {
        return ImmediatelyFastRuntime.snapshot();
    }

    public static UniformAllocatorStatsSnapshot uniformStatsSnapshot() {
        return UNIFORMS.statsSnapshot();
    }

    public static RenderResourceStatsSnapshot resourceStatsSnapshot() {
        return resources().statsSnapshot();
    }

    public static WorldRenderStatsSnapshot worldRenderStatsSnapshot() {
        return Renderer3D.worldStatsSnapshot();
    }

    public static TextCommandStatsSnapshot textStatsSnapshot() {
        return TextRenderSystem.statsSnapshot();
    }
}
