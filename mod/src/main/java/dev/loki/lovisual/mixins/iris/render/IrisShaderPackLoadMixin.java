package dev.loki.lovisual.mixins.iris.render;

import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.render.iris.patch.ShaderPatchEngine;

@Pseudo
@Mixin(value = Iris.class, remap = false)
public abstract class IrisShaderPackLoadMixin {
    @Inject(method = "loadExternalShaderpack", at = @At("HEAD"), remap = false)
    private static void lovisual$beginShaderPatchSession(String shaderPackName, CallbackInfoReturnable<Boolean> cir) {
        ShaderPatchEngine.beginShaderPackLoad(shaderPackName);
    }

    @Inject(method = "loadExternalShaderpack", at = @At("RETURN"), remap = false)
    private static void lovisual$endShaderPatchSession(String shaderPackName, CallbackInfoReturnable<Boolean> cir) {
        ShaderPatchEngine.endShaderPackLoad(shaderPackName);
    }
}
