package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.vulkan.VulkanBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.debug.VulkanCrashDiagnostics;

@Mixin(VulkanBackend.class)
public abstract class VulkanBackendMixin {
    @ModifyVariable(method = "createDevice", at = @At("HEAD"), argsOnly = true)
    private GpuDebugOptions lovisual$forceDebugOptions(GpuDebugOptions options) {
        return VulkanCrashDiagnostics.forceDebugOptions(options);
    }
}
