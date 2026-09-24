package dev.loki.lovisual.render.engine.rhi.backend.vulkan.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

/**
 * Vulkan-side fixed pipeline state bridge for LoVisual's GL-era dynamic state.
 *
 * <p>This is the stable public entry point used by the Vulkan mixins and backends.
 * The concrete state lives in two package-private domain helpers:
 * {@link VulkanMsaaState} (sample counts and MSAA capabilities) and
 * {@link VulkanStencilState} (stencil clip stack, per-pass attachment latch and
 * pipeline-variant derivation). The render-pass begin/end orchestration that spans
 * both domains stays here.</p>
 *
 * <p>Important Vulkan rule: do not destroy/recreate cached VkPipelines while the current
 * command buffer may already reference them. Stencil modes are therefore represented through a
 * separate per-mode pipeline variant cache in VulkanDeviceMixin, not by calling
 * GpuDevice.clearPipelineCache() from setStencilMode().</p>
 */
public enum VulkanRenderStateBridge {
    ;

    private static volatile boolean vulkanBackendActive;

    public static void setVulkanBackendActive(boolean active) {
        vulkanBackendActive = active;
        if (!active) {
            VulkanMsaaState.resetForInactiveBackend();
            VulkanStencilState.resetForInactiveBackend();
        }
    }

    public static boolean vulkanBackendActive() {
        return vulkanBackendActive;
    }

    // ---- MSAA / sample-count domain (delegates to VulkanMsaaState) ----

    public static boolean forceDisableMsaa() {
        return VulkanMsaaState.forceDisableMsaa();
    }

    public static String msaaDisableReason() {
        return VulkanMsaaState.msaaDisableReason();
    }

    public static void disableMsaaAfterFailure(String reason, Throwable t) {
        VulkanMsaaState.disableMsaaAfterFailure(reason, t);
    }

    public static boolean forceDisableCheckpoints() {
        return VulkanRuntimeGuards.FORCE_DISABLE_VULKAN_CHECKPOINTS;
    }

    public static boolean forceDisableDebugLabels() {
        return VulkanRuntimeGuards.FORCE_DISABLE_VULKAN_DEBUG_LABELS;
    }

    public static int requestedTextureSamples() {
        return VulkanMsaaState.requestedTextureSamples();
    }

    public static <T> T withTextureSamples(int samples, java.util.function.Supplier<T> factory) {
        return VulkanMsaaState.withTextureSamples(samples, factory);
    }

    public static int normalizeSamples(int samples) {
        return VulkanMsaaState.normalizeSamples(samples);
    }

    public static int currentRenderPassSamples() {
        return VulkanMsaaState.currentRenderPassSamples();
    }

    public static int currentVkSampleCount() {
        return VulkanMsaaState.currentVkSampleCount();
    }

    public static void configureMsaaCapabilities(int framebufferSampleCounts,
                                                 int depthResolveModes,
                                                 boolean supportsIndependentResolveNone) {
        VulkanMsaaState.configureMsaaCapabilities(framebufferSampleCounts, depthResolveModes, supportsIndependentResolveNone);
    }

    public static boolean msaaSupported() {
        return VulkanMsaaState.msaaSupported();
    }

    public static int depthResolveMode() {
        return VulkanMsaaState.depthResolveMode();
    }

    public static boolean independentResolveNone() {
        return VulkanMsaaState.independentResolveNone();
    }

    public static void beginRenderPass(RenderPassDescriptor descriptor) {
        if (!vulkanBackendActive) return;
        int samples = VulkanMsaaState.beginRenderPass(descriptor);
        VulkanStencilState.beginRenderPass(samples);
    }

    public static void endRenderPass() {
        VulkanMsaaState.endRenderPass();
        VulkanStencilState.endRenderPass();
    }

    public static int samplesFor(@Nullable RenderPassDescriptor descriptor) {
        return VulkanMsaaState.samplesFor(descriptor);
    }

    public static int samplesFor(@Nullable GpuTextureView view) {
        return VulkanMsaaState.samplesFor(view);
    }

    // ---- Stencil / shape-clip domain (delegates to VulkanStencilState) ----

    public static boolean forceDisableStencil() {
        return VulkanStencilState.forceDisableStencil();
    }

    public static String stencilDisableReason() {
        return VulkanStencilState.stencilDisableReason();
    }

    public static void disableStencilAfterFailure(String reason, Throwable t) {
        VulkanStencilState.disableStencilAfterFailure(reason, t);
    }

    public static boolean currentRenderPassNeedsStencilAttachment() {
        return VulkanStencilState.currentRenderPassNeedsStencilAttachment();
    }

    public static void markCurrentRenderPassStencilAttachment(boolean attached) {
        VulkanStencilState.markCurrentRenderPassStencilAttachment(attached);
    }

    public static boolean currentRenderPassHasStencilAttachment() {
        return VulkanStencilState.currentRenderPassHasStencilAttachment();
    }

    public static void pushStencilOffscreenBypass() {
        VulkanStencilState.pushStencilOffscreenBypass();
    }

    public static void popStencilOffscreenBypass() {
        VulkanStencilState.popStencilOffscreenBypass();
    }

    public static boolean stencilOffscreenBypassed() {
        return VulkanStencilState.stencilOffscreenBypassed();
    }

    public static void setStencilMode(StencilMode mode, int reference, int compareReference) {
        VulkanStencilState.setStencilMode(mode, reference, compareReference);
    }

    public static void requestStencilClear() {
        VulkanStencilState.requestStencilClear();
    }

    public static boolean consumeStencilClearRequested() {
        return VulkanStencilState.consumeStencilClearRequested();
    }

    public static StencilMode stencilMode() {
        return VulkanStencilState.stencilMode();
    }

    public static int stencilReference() {
        return VulkanStencilState.stencilReference();
    }

    public static int stencilCompareReference() {
        return VulkanStencilState.stencilCompareReference();
    }

    public static boolean pipelineParticipatesInShapeClip(RenderPipeline pipeline) {
        return VulkanStencilState.pipelineParticipatesInShapeClip(pipeline);
    }

    public static boolean pipelineNeedsStencilFormat(RenderPipeline pipeline) {
        return VulkanStencilState.pipelineNeedsStencilFormat(pipeline);
    }

    public static boolean stencilTestEnabledForCurrentPipeline(RenderPipeline pipeline) {
        return VulkanStencilState.stencilTestEnabledForCurrentPipeline(pipeline);
    }

    public static boolean suppressColorWrites() {
        return VulkanStencilState.suppressColorWrites();
    }

    public static void beginPipelineCompile(RenderPipeline pipeline) {
        VulkanStencilState.beginPipelineCompile(pipeline);
    }

    public static void endPipelineCompile() {
        VulkanStencilState.endPipelineCompile();
    }

    public static boolean needsPipelineVariant(RenderPipeline pipeline) {
        return VulkanStencilState.needsPipelineVariant(pipeline);
    }

    public static VulkanPipelineVariantKey pipelineVariantKey(RenderPipeline pipeline) {
        return VulkanStencilState.pipelineVariantKey(pipeline);
    }

    public static int dynamicStencilReference() {
        return VulkanStencilState.dynamicStencilReference();
    }

    public enum StencilMode {
        DISABLED,
        WRITE,
        RESTORE,
        TEST
    }
}
