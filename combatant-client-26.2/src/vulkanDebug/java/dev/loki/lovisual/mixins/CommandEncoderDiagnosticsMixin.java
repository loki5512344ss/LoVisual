package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.debug.VulkanCrashDiagnostics;

@Mixin(CommandEncoder.class)
public abstract class CommandEncoderDiagnosticsMixin {
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
    private static String lovisual$label(RenderPassDescriptor descriptor) {
        if (descriptor == null || descriptor.label() == null) return "<null-label>";
        try {
            String label = descriptor.label().get();
            return label == null ? "<null-label>" : label;
        } catch (Throwable t) {
            return "<label-error:" + t.getClass().getSimpleName() + ">";
        }
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("HEAD"))
    private void lovisual$wrapperCreateHead(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.create.head",
                "label", lovisual$label(descriptor),
                "colors", descriptor == null || descriptor.colorAttachments() == null ? -1 : descriptor.colorAttachments().size(),
                "depth", descriptor != null && descriptor.depthAttachment() != null);
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoderBackend;createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPassBackend;", shift = At.Shift.BEFORE))
    private void lovisual$backendCreateBefore(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.backend.create.before", "label", lovisual$label(descriptor));
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoderBackend;createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPassBackend;", shift = At.Shift.AFTER))
    private void lovisual$backendCreateAfter(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.backend.create.after", "label", lovisual$label(descriptor));
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("RETURN"))
    private void lovisual$wrapperCreateReturn(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> cir) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.create.return",
                "label", lovisual$label(descriptor),
                "pass", cir.getReturnValue() == null ? "<null>" : cir.getReturnValue().getClass().getName());
    }

    @Inject(method = "submitRenderPass", at = @At("HEAD"))
    private void lovisual$wrapperSubmitHead(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.submit.head");
    }

    @Inject(method = "submitRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoderBackend;submitRenderPass()V", shift = At.Shift.BEFORE))
    private void lovisual$backendSubmitBefore(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.backend.submit.before");
    }

    @Inject(method = "submitRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoderBackend;submitRenderPass()V", shift = At.Shift.AFTER))
    private void lovisual$backendSubmitAfter(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.backend.submit.after");
    }

    @Inject(method = "submitRenderPass", at = @At("TAIL"))
    private void lovisual$wrapperSubmitTail(CallbackInfo ci) {
        if (!lovisual$isVulkanBackend()) return;
        VulkanCrashDiagnostics.breadcrumbQuiet("commandencoder.wrapper.submit.tail");
    }
}
