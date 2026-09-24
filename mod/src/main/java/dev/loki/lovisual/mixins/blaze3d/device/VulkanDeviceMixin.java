package dev.loki.lovisual.mixins.blaze3d.device;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanInstance;
import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageFormatProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceDepthStencilResolvePropertiesKHR;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.util.VulkanRenderStateBridge;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.util.VulkanPipelineVariantKey;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.vulkan.VK12.*;

@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceMixin {
    @Shadow
    private ShaderSource defaultShaderSource;

    @Unique
    private final Map<VulkanPipelineVariantKey, VulkanRenderPipeline> lovisual$pipelineVariants = new HashMap<>();

    @Invoker("compilePipeline")
    protected abstract VulkanRenderPipeline lovisual$compilePipelineVariant(RenderPipeline pipeline, ShaderSource source);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lovisual$captureMsaaCapabilities(ShaderSource shaderSource,
                                                   VulkanInstance instance,
                                                   VulkanPhysicalDevice physicalDevice,
                                                   Set<String> enabledExtensions,
                                                   VkDevice device,
                                                   long vma,
                                                   CheckpointExtension checkpointExtension,
                                                   CallbackInfo ci) {
        if (physicalDevice == null || physicalDevice.vkPhysicalDeviceProperties() == null) return;
        VkPhysicalDeviceLimits limits = physicalDevice.vkPhysicalDeviceProperties().limits();
        int framebufferSamples = limits.framebufferColorSampleCounts()
                & limits.framebufferDepthSampleCounts()
                & limits.framebufferStencilSampleCounts();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int textureUsage = GpuTexture.USAGE_COPY_DST
                    | GpuTexture.USAGE_COPY_SRC
                    | GpuTexture.USAGE_TEXTURE_BINDING
                    | GpuTexture.USAGE_RENDER_ATTACHMENT;
            framebufferSamples &= lovisual$imageSampleCounts(
                    stack,
                    physicalDevice,
                    GpuFormat.RGBA8_UNORM,
                    VulkanConst.textureUsageToVk(textureUsage, GpuFormat.RGBA8_UNORM)
            );
            framebufferSamples &= lovisual$imageSampleCounts(
                    stack,
                    physicalDevice,
                    GpuFormat.D32_FLOAT,
                    VulkanConst.textureUsageToVk(textureUsage, GpuFormat.D32_FLOAT)
            );
            framebufferSamples &= lovisual$imageSampleCounts(
                    stack,
                    physicalDevice,
                    GpuFormat.S8_UINT,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
            );

            VkPhysicalDeviceDepthStencilResolvePropertiesKHR resolve =
                    VkPhysicalDeviceDepthStencilResolvePropertiesKHR.calloc(stack).sType$Default();
            VkPhysicalDeviceProperties2 properties = VkPhysicalDeviceProperties2.calloc(stack)
                    .sType$Default()
                    .pNext(resolve);
            vkGetPhysicalDeviceProperties2(physicalDevice.vkPhysicalDevice(), properties);
            VulkanRenderStateBridge.configureMsaaCapabilities(
                    framebufferSamples,
                    resolve.supportedDepthResolveModes(),
                    resolve.independentResolveNone()
            );
        } catch (Throwable t) {
            VulkanRenderStateBridge.configureMsaaCapabilities(1, 0, false);
            DebugLog.warnOnChange(
                    "msaa.vulkan.capabilities.failed",
                    t.getClass().getSimpleName() + "|" + t.getMessage(),
                    "[MSAA/Vulkan] capability query failed; MSAA remains disabled: %s: %s",
                    t.getClass().getSimpleName(),
                    t.getMessage()
            );
        }
    }

    @Unique
    private static int lovisual$imageSampleCounts(MemoryStack stack,
                                                   VulkanPhysicalDevice physicalDevice,
                                                   GpuFormat format,
                                                   int usage) {
        VkImageFormatProperties properties = VkImageFormatProperties.calloc(stack);
        int result = vkGetPhysicalDeviceImageFormatProperties(
                physicalDevice.vkPhysicalDevice(),
                VulkanConst.toVk(format),
                VK_IMAGE_TYPE_2D,
                VK_IMAGE_TILING_OPTIMAL,
                usage,
                0,
                properties
        );
        return result == VK_SUCCESS ? properties.sampleCounts() : VK_SAMPLE_COUNT_1_BIT;
    }

    @Inject(method = "getOrCompilePipeline", at = @At("HEAD"), cancellable = true)
    private void lovisual$getOrCompilePipelineVariant(RenderPipeline pipeline,
                                                       CallbackInfoReturnable<VulkanRenderPipeline> cir) {
        if (!VulkanRenderStateBridge.needsPipelineVariant(pipeline)) return;

        VulkanPipelineVariantKey key = VulkanRenderStateBridge.pipelineVariantKey(pipeline);
        VulkanRenderPipeline variant = lovisual$pipelineVariants.get(key);
        if (variant != null) {
            cir.setReturnValue(variant);
            return;
        }

        try {
            variant = lovisual$compilePipelineVariant(pipeline, defaultShaderSource);
            if (variant == null || !variant.isValid()) {
                throw new IllegalStateException("Vulkan returned an invalid pipeline variant");
            }
            lovisual$pipelineVariants.put(key, variant);
            DebugLog.stencilOnChange(
                    "vulkan.pipeline.variant.compile",
                    key.toString(),
                    "[Vulkan/RHI] compiled pipeline variant %s",
                    key
            );
            cir.setReturnValue(variant);
        } catch (Throwable t) {
            if (key.usesMsaa()) {
                VulkanRenderStateBridge.disableMsaaAfterFailure("pipeline variant compile " + key, t);
            }
            if (key.usesStencil()) {
                VulkanRenderStateBridge.disableStencilAfterFailure("pipeline variant compile " + key, t);
            }
            DebugLog.error("[Vulkan/RHI] pipeline variant compile failed for " + key, t);
            // The current dynamic-rendering scope is already active. Falling through to vanilla's
            // single-sample/no-stencil pipeline would bind an incompatible VkPipeline and can lose
            // the device, so abort on the CPU and only disable the feature for future passes.
            throw t instanceof RuntimeException runtime ? runtime
                    : new IllegalStateException("Failed to compile Vulkan pipeline variant " + key, t);
        }
    }

    @Inject(method = "clearPipelineCache", at = @At("RETURN"))
    private void lovisual$clearPipelineVariants(CallbackInfo ci) {
        if (lovisual$pipelineVariants.isEmpty()) return;
        for (VulkanRenderPipeline pipeline : lovisual$pipelineVariants.values()) {
            try {
                pipeline.destroy();
            } catch (Throwable t) {
                DebugLog.warnOnChange(
                        "vulkan.pipeline.variant.destroy.failed",
                        t.getClass().getSimpleName() + "|" + t.getMessage(),
                        "[Vulkan/RHI] failed to destroy pipeline variant: %s: %s",
                        t.getClass().getSimpleName(),
                        t.getMessage()
                );
            }
        }
        lovisual$pipelineVariants.clear();
    }
}
