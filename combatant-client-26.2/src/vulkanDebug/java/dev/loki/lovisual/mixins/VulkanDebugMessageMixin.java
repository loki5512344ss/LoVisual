package dev.loki.lovisual.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.debug.VulkanCrashDiagnostics;

@Mixin(targets = "com.mojang.blaze3d.vulkan.VulkanDebug$Enabled")
public abstract class VulkanDebugMessageMixin {
    @Inject(method = "onDebugMessage", at = @At("HEAD"))
    private void lovisual$captureDriverDebugMessage(int severity,
                                                     int type,
                                                     long callbackData,
                                                     long userData,
                                                     CallbackInfoReturnable<Integer> cir) {
        VulkanCrashDiagnostics.driverDebugMessage(severity, type, callbackData);
    }
}
