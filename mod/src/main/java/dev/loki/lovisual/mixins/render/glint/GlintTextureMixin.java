package dev.loki.lovisual.mixins.render.glint;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.items.glint.EnchantGlint;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.state.OptionsRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** EnchantGlint: скорость прокрутки, масштаб полос и своя анимация стилей в матрице текстуры. */
@Mixin(TextureTransform.class)
public abstract class GlintTextureMixin {

    @ModifyVariable(method = "setupGlintTexturing", at = @At("HEAD"), argsOnly = true)
    private static float lovisual$glintScale(float scale) {
        EnchantGlint glint = Modules.get(EnchantGlint.class);
        return glint == null ? scale : scale * glint.scaleFactor();
    }

    @Redirect(method = "setupGlintTexturing",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;glintSpeed:D"))
    private static double lovisual$glintSpeed(OptionsRenderState state) {
        EnchantGlint glint = Modules.get(EnchantGlint.class);
        return glint == null ? state.glintSpeed : state.glintSpeed * glint.speedFactor();
    }

    @ModifyReturnValue(method = "setupGlintTexturing", at = @At("RETURN"))
    private static Matrix4f lovisual$styleMotion(Matrix4f vanilla, @Local(argsOnly = true) float scale) {
        EnchantGlint glint = Modules.get(EnchantGlint.class);
        Matrix4f custom = glint == null ? null : glint.motion(scale);
        return custom == null ? vanilla : custom;
    }
}
