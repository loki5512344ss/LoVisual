package dev.loki.lovisual.render.engine.rhi.backend.vulkan.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.mixininterface.render.IRenderPipeline;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.util.VulkanRenderStateBridge.StencilMode;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.util.logging.DebugLog;

/**
 * Stencil / shape-clip domain of {@link VulkanRenderStateBridge}: the logical clip
 * stack (mode + references), the per-pass stencil-attachment latch, the offscreen
 * bypass depth, the runtime stencil-disable latch, the pipeline-compile context and
 * the pipeline-variant key derivation. Package-private helper (impl subfolder,
 * {@code vulkan.util} package); the facade exposes it through its public static API.
 */
final class VulkanStencilState {

    private static final ThreadLocal<RenderPipeline> COMPILING_PIPELINE = new ThreadLocal<>();
    private static volatile StencilMode stencilMode = StencilMode.DISABLED;
    private static volatile int stencilReference;
    private static volatile int stencilCompareReference;
    private static volatile boolean stencilClearRequested;
    private static volatile boolean currentRenderPassStencilAttachment;
    private static volatile int stencilOffscreenBypassDepth;
    private static volatile boolean runtimeStencilDisabled;
    private static volatile String runtimeStencilDisableReason = "none";

    private VulkanStencilState() {
    }

    static void resetForInactiveBackend() {
        stencilMode = StencilMode.DISABLED;
        stencilReference = 0;
        stencilCompareReference = 0;
        stencilClearRequested = false;
        currentRenderPassStencilAttachment = false;
        stencilOffscreenBypassDepth = 0;
        runtimeStencilDisabled = false;
        runtimeStencilDisableReason = "none";
        COMPILING_PIPELINE.remove();
    }

    static boolean forceDisableStencil() {
        return runtimeStencilDisabled;
    }

    static String stencilDisableReason() {
        return runtimeStencilDisableReason;
    }

    static void disableStencilAfterFailure(String reason, Throwable t) {
        runtimeStencilDisabled = true;
        runtimeStencilDisableReason = reason == null ? "runtime-failure" : reason;
        stencilMode = StencilMode.DISABLED;
        stencilReference = 0;
        stencilCompareReference = 0;
        stencilClearRequested = false;
        currentRenderPassStencilAttachment = false;
        stencilOffscreenBypassDepth = 0;
        String key = runtimeStencilDisableReason + "|" + (t == null ? "null" : t.getClass().getSimpleName());
        DebugLog.errorOnChange("shapeclip.vulkan.disabled.after.failure", key,
                "[ShapeClip/Vulkan] disabling stencil after failure: reason=%s error=%s: %s",
                runtimeStencilDisableReason,
                t == null ? "none" : t.getClass().getSimpleName(),
                t == null ? "" : t.getMessage());
    }

    /** Latch the per-pass stencil-attachment flag for the pass whose sample count was just set. */
    static void beginRenderPass(int samples) {
        currentRenderPassStencilAttachment = !forceDisableStencil() && stencilAttachmentRequiredForNextPass();
        DebugLog.stencilOnChange("vulkan.renderpass.compat",
                samples + "|" + currentRenderPassStencilAttachment + "|" + stencilMode,
                "[Vulkan/RHI] render pass compatibility: samples=%d stencilAttachment=%s stencilMode=%s",
                samples, currentRenderPassStencilAttachment, stencilMode);
    }

    static void endRenderPass() {
        currentRenderPassStencilAttachment = false;
        COMPILING_PIPELINE.remove();
    }

    private static boolean stencilAttachmentRequiredForNextPass() {
        if (forceDisableStencil() || stencilOffscreenBypassDepth > 0) return false;
        return stencilMode != StencilMode.DISABLED || stencilClearRequested;
    }

    static boolean currentRenderPassNeedsStencilAttachment() {
        return VulkanRenderStateBridge.vulkanBackendActive() && !forceDisableStencil() && currentRenderPassStencilAttachment;
    }

    static void markCurrentRenderPassStencilAttachment(boolean attached) {
        currentRenderPassStencilAttachment = VulkanRenderStateBridge.vulkanBackendActive() && !forceDisableStencil() && attached;
    }

    static boolean currentRenderPassHasStencilAttachment() {
        return VulkanRenderStateBridge.vulkanBackendActive() && !forceDisableStencil() && currentRenderPassStencilAttachment;
    }

    static void pushStencilOffscreenBypass() {
        stencilOffscreenBypassDepth++;
    }

    static void popStencilOffscreenBypass() {
        if (stencilOffscreenBypassDepth <= 0) return;
        stencilOffscreenBypassDepth--;
    }

    static boolean stencilOffscreenBypassed() {
        return stencilOffscreenBypassDepth > 0;
    }

    static void setStencilMode(StencilMode mode, int reference, int compareReference) {
        if (forceDisableStencil()) {
            stencilMode = StencilMode.DISABLED;
            stencilReference = 0;
            stencilCompareReference = 0;
            stencilClearRequested = false;
            currentRenderPassStencilAttachment = false;
            return;
        }
        stencilMode = mode == null ? StencilMode.DISABLED : mode;
        stencilReference = clampStencilReference(reference);
        stencilCompareReference = clampStencilReference(compareReference);
    }

    static void requestStencilClear() {
        if (forceDisableStencil()) return;
        stencilClearRequested = true;
    }

    static boolean consumeStencilClearRequested() {
        if (forceDisableStencil()) {
            stencilClearRequested = false;
            return false;
        }
        boolean requested = stencilClearRequested;
        stencilClearRequested = false;
        return requested;
    }

    static StencilMode stencilMode() {
        return stencilMode;
    }

    static int stencilReference() {
        return stencilReference;
    }

    static int stencilCompareReference() {
        return stencilCompareReference;
    }

    static boolean pipelineParticipatesInShapeClip(RenderPipeline pipeline) {
        if (forceDisableStencil()) return false;
        if (!(pipeline instanceof IRenderPipeline lovisualPipeline)) return false;
        ShapeClipRenderPassContract contract = lovisualPipeline.lovisual$getShapeClipContract();
        return contract != null && contract.participates();
    }

    /**
     * Dynamic rendering compatibility is per active render pass, not per logical clip contract.
     * If pStencilAttachment is present, every graphics pipeline used in that dynamic rendering scope
     * must be compiled with the same stencilAttachmentFormat. Non-participating pipelines simply keep
     * stencilTestEnable=false.
     */
    static boolean pipelineNeedsStencilFormat(RenderPipeline pipeline) {
        return currentRenderPassHasStencilAttachment();
    }

    static boolean stencilTestEnabledForCurrentPipeline(RenderPipeline pipeline) {
        return !stencilOffscreenBypassed()
                && currentRenderPassHasStencilAttachment()
                && pipelineParticipatesInShapeClip(pipeline)
                && stencilMode != StencilMode.DISABLED;
    }

    static boolean suppressColorWrites() {
        RenderPipeline pipeline = COMPILING_PIPELINE.get();
        return !stencilOffscreenBypassed()
                && currentRenderPassHasStencilAttachment()
                && pipeline != null
                && pipelineParticipatesInShapeClip(pipeline)
                && (stencilMode == StencilMode.WRITE || stencilMode == StencilMode.RESTORE);
    }

    static void beginPipelineCompile(RenderPipeline pipeline) {
        COMPILING_PIPELINE.set(pipeline);
    }

    static void endPipelineCompile() {
        COMPILING_PIPELINE.remove();
    }

    static boolean needsPipelineVariant(RenderPipeline pipeline) {
        if (!VulkanRenderStateBridge.vulkanBackendActive()) return false;
        return currentRenderPassHasStencilAttachment() || VulkanMsaaState.currentRenderPassSamples() != 1;
    }

    static VulkanPipelineVariantKey pipelineVariantKey(RenderPipeline pipeline) {
        StencilMode effectiveMode = stencilTestEnabledForCurrentPipeline(pipeline) ? stencilMode : StencilMode.DISABLED;
        boolean stencilFormat = currentRenderPassHasStencilAttachment();
        return new VulkanPipelineVariantKey(
                pipeline,
                VulkanMsaaState.currentRenderPassSamples(),
                stencilFormat,
                effectiveMode,
                pipelineParticipatesInShapeClip(pipeline)
        );
    }

    static int dynamicStencilReference() {
        return stencilCompareReference;
    }

    private static int clampStencilReference(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
