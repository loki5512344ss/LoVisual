package dev.loki.lovisual.render.engine.rhi.backend.vulkan.util;

import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.mixininterface.device.IMsaaTexture;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;

import static org.lwjgl.vulkan.VK12.*;

/**
 * MSAA / sample-count domain of {@link VulkanRenderStateBridge}: tracks the
 * requested and current-pass sample counts, the device's supported sample/resolve
 * capabilities, the runtime MSAA-disable latch, and render-pass attachment sample
 * validation. Package-private helper (impl subfolder, {@code vulkan.util} package);
 * the facade exposes it through its public static API.
 */
final class VulkanMsaaState {

    private static final ThreadLocal<Integer> REQUESTED_TEXTURE_SAMPLES = ThreadLocal.withInitial(() -> 1);
    private static volatile int currentRenderPassSamples = 1;
    private static volatile boolean runtimeMsaaDisabled;
    private static volatile String runtimeMsaaDisableReason = "none";
    private static volatile int supportedFramebufferSampleCounts = VK_SAMPLE_COUNT_1_BIT;
    private static volatile int supportedDepthResolveModes;
    private static volatile boolean independentResolveNone;

    private VulkanMsaaState() {
    }

    static void resetForInactiveBackend() {
        currentRenderPassSamples = 1;
        runtimeMsaaDisabled = false;
        runtimeMsaaDisableReason = "none";
    }

    static boolean forceDisableMsaa() {
        return runtimeMsaaDisabled;
    }

    static String msaaDisableReason() {
        return runtimeMsaaDisableReason;
    }

    static void disableMsaaAfterFailure(String reason, Throwable t) {
        runtimeMsaaDisabled = true;
        runtimeMsaaDisableReason = reason == null ? "runtime-failure" : reason;
        String key = runtimeMsaaDisableReason + "|" + (t == null ? "null" : t.getClass().getSimpleName());
        DebugLog.errorOnChange("msaa.vulkan.disabled.after.failure", key,
                "[MSAA/Vulkan] disabling MSAA for future render passes: reason=%s error=%s: %s",
                runtimeMsaaDisableReason,
                t == null ? "none" : t.getClass().getSimpleName(),
                t == null ? "" : t.getMessage());
    }

    static int requestedTextureSamples() {
        if (forceDisableMsaa()) return 1;
        return selectSupportedSamples(REQUESTED_TEXTURE_SAMPLES.get());
    }

    static <T> T withTextureSamples(int samples, java.util.function.Supplier<T> factory) {
        int previous = REQUESTED_TEXTURE_SAMPLES.get();
        REQUESTED_TEXTURE_SAMPLES.set(forceDisableMsaa() ? 1 : selectSupportedSamples(samples));
        try {
            return factory.get();
        } finally {
            REQUESTED_TEXTURE_SAMPLES.set(previous);
        }
    }

    static int normalizeSamples(int samples) {
        return forceDisableMsaa() ? 1 : selectSupportedSamples(samples);
    }

    static int currentRenderPassSamples() {
        return Math.max(1, currentRenderPassSamples);
    }

    static int currentVkSampleCount() {
        return Math.max(1, currentRenderPassSamples);
    }

    static void configureMsaaCapabilities(int framebufferSampleCounts,
                                          int depthResolveModes,
                                          boolean supportsIndependentResolveNone) {
        supportedFramebufferSampleCounts = framebufferSampleCounts | VK_SAMPLE_COUNT_1_BIT;
        supportedDepthResolveModes = depthResolveModes;
        independentResolveNone = supportsIndependentResolveNone;
    }

    static boolean msaaSupported() {
        return !forceDisableMsaa()
                && (supportedFramebufferSampleCounts & ~VK_SAMPLE_COUNT_1_BIT) != 0
                && supportedDepthResolveModes != VK_RESOLVE_MODE_NONE;
    }

    static int depthResolveMode() {
        if ((supportedDepthResolveModes & VK_RESOLVE_MODE_SAMPLE_ZERO_BIT) != 0) {
            return VK_RESOLVE_MODE_SAMPLE_ZERO_BIT;
        }
        if ((supportedDepthResolveModes & VK_RESOLVE_MODE_MAX_BIT) != 0) {
            return VK_RESOLVE_MODE_MAX_BIT;
        }
        throw new IllegalStateException("Vulkan device exposes no supported depth resolve mode");
    }

    static boolean independentResolveNone() {
        return independentResolveNone;
    }

    private static int selectSupportedSamples(int requested) {
        if (requested <= 1) return 1;
        int capped = Math.min(requested, 16);
        for (int candidate = 16; candidate >= 2; candidate >>= 1) {
            if (candidate <= capped && (supportedFramebufferSampleCounts & candidate) != 0) {
                return candidate;
            }
        }
        return 1;
    }

    /** Validate the descriptor's attachments and latch the current-pass sample count. */
    static int beginRenderPass(RenderPassDescriptor descriptor) {
        int samples = validateAndGetSamples(descriptor);
        currentRenderPassSamples = samples;
        return samples;
    }

    static void endRenderPass() {
        currentRenderPassSamples = 1;
    }

    static int samplesFor(@Nullable RenderPassDescriptor descriptor) {
        return validateAndGetSamples(descriptor);
    }

    private static int validateAndGetSamples(@Nullable RenderPassDescriptor descriptor) {
        if (descriptor == null) return 1;
        int samples = 0;
        List<RenderPassDescriptor.Attachment<java.util.Optional<org.joml.Vector4fc>>> colors = descriptor.colorAttachments();
        if (colors != null) {
            for (RenderPassDescriptor.Attachment<?> attachment : colors) {
                samples = mergeAttachmentSamples(samples, samplesFor(attachment));
            }
        }
        samples = mergeAttachmentSamples(samples, samplesFor(descriptor.depthAttachment()));
        if (samples == 0) samples = 1;
        if (samples > 1 && (forceDisableMsaa() || (supportedFramebufferSampleCounts & samples) == 0)) {
            throw new IllegalStateException("Unsupported Vulkan render-pass sample count: " + samples
                    + " (supported mask=0x" + Integer.toHexString(supportedFramebufferSampleCounts) + ")");
        }
        return samples;
    }

    private static int mergeAttachmentSamples(int current, int attachmentSamples) {
        if (attachmentSamples <= 0) return current;
        if (current == 0) return attachmentSamples;
        if (current != attachmentSamples) {
            throw new IllegalStateException("Vulkan render-pass attachment sample mismatch: "
                    + current + "x versus " + attachmentSamples + "x");
        }
        return current;
    }

    private static int samplesFor(@Nullable RenderPassDescriptor.Attachment<?> attachment) {
        if (attachment == null || attachment.textureView() == null || attachment.textureView().texture() == null) {
            return 0;
        }
        if (attachment.textureView().texture() instanceof IMsaaTexture texture && texture.lovisual$isMsaa()) {
            return Math.max(1, texture.lovisual$getSamples());
        }
        return 1;
    }

    static int samplesFor(@Nullable GpuTextureView view) {
        if (view == null || view.texture() == null) return 1;
        if (view.texture() instanceof IMsaaTexture texture && texture.lovisual$isMsaa()) {
            return Math.max(1, texture.lovisual$getSamples());
        }
        return 1;
    }
}
