package dev.loki.lovisual.mixins.render.glint;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.items.glint.EnchantGlint;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.state.OptionsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** EnchantGlint: скорость прокрутки и масштаб полос блеска в матрице текстуры. */
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
}
