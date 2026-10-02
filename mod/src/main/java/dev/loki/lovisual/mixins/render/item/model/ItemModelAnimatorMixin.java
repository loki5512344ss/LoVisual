package dev.loki.lovisual.mixins.render.item.model;

import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.ext.ModelAnimator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/** Подмена списка квадов слоя предмета на анимированную копию (item_model.lua, {@code animator}). */
@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public abstract class ItemModelAnimatorMixin {

    @ModifyArg(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
            ),
            index = 6
    )
    private List<BakedQuad> lovisual$animateQuads(List<BakedQuad> quads) {
        return ModelAnimator.INSTANCE.apply(quads);
    }
}
