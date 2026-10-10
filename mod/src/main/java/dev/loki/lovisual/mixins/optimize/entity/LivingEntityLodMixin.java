package dev.loki.lovisual.mixins.optimize.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.loki.lovisual.features.module.modules.misc.optimize.entity.EntityLod;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3: skip {@code shouldRenderLayers(state)} for far living entities —
 * the entity's armour, held items, eyes, cape, elytra, and stuck arrows are
 * all submitted by {@code renderLayers}, and they're invisible past the
 * configured LOD distance. Returning {@code false} short-circuits the whole
 * layer pass for that entity.
 *
 * <p>The vanilla {@code shouldRenderLayers} returns true for every visible
 * entity; we AND it with "not far" so far entities skip the layers but
 * close entities render them as usual.
 *
 * <p>Ported from {@code zako.opt.mixin.entity.LivingEntityLodMixin} (ZakoOpt,
 * LGPL-3.0). Original author: Zako — https://t.me/StarikZako.
 *
 * <p>Gated on the {@code entity_lod} toggle via {@link EntityLod#far(double)}.
 * When the toggle is off, {@code far} returns {@code false} so the modify
 * leaves the original value unchanged.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityLodMixin {

    @ModifyExpressionValue(
            method = "submit",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z")
    )
    private boolean lovisual$skipFarLayers(boolean original, LivingEntityRenderState state) {
        if (!original) {
            return false;
        }
        // Stage 1 collapses player LOD into entity LOD — same toggle, same threshold.
        return !EntityLod.far(state.distanceToCameraSq);
    }
}
