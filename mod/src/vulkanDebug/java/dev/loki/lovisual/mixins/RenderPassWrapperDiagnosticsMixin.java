package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.debug.VulkanCrashDiagnostics;

@Mixin(RenderPass.class)
public abstract class RenderPassWrapperDiagnosticsMixin {
    @Unique
    private static boolean lovisual$isVulkanBackend() {
        try {
            if (RenderSystem.getDevice() == null || RenderSystem.getDevice().getDeviceInfo() == null) return false;
            String backendName = RenderSystem.getDevice().getDeviceInfo().backendName();
            return backendName != null && backendName.toLowerCase(java.util.Locale.ROOT).contains("vulkan");
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private static String lovisual$pipelineId(RenderPipeline pipeline) {
        if (pipeline == null) return "<null>";
        try {
            return String.valueOf(pipeline.getLocation());
        } catch (Throwable ignored) {
            return pipeline.toString();
        }
    }

    @Inject(method = "setPipeline", at = @At("HEAD"))
    private void lovisual$setPipelineHead(RenderPipeline pipeline, CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.setpipeline.head", "pipeline", lovisual$pipelineId(pipeline));
    }

    @Inject(
            method = "setPipeline",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPassBackend;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", shift = At.Shift.BEFORE)
    )
    private void lovisual$backendSetPipelineBefore(RenderPipeline pipeline, CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.backend.setpipeline.before", "pipeline", lovisual$pipelineId(pipeline));
    }

    @Inject(
            method = "setPipeline",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPassBackend;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", shift = At.Shift.AFTER)
    )
    private void lovisual$backendSetPipelineAfter(RenderPipeline pipeline, CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.backend.setpipeline.after", "pipeline", lovisual$pipelineId(pipeline));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void lovisual$closeHead(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.close.head");
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", shift = At.Shift.BEFORE))
    private void lovisual$closeOnFinishBefore(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.close.onfinish.before");
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", shift = At.Shift.AFTER))
    private void lovisual$closeOnFinishAfter(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.close.onfinish.after");
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void lovisual$closeTail(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("renderpass.wrapper.close.tail");
    }
}
