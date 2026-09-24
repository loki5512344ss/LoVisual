package dev.loki.lovisual.mixins.render.entity.crystal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.EndCrystal;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Тонировка модели энд-кристалла по настройкам {@link EndCrystal}.
 * {@code Model#renderToBuffer} — единственная точка, где весь меш (все части
 * разом) отправляется на отрисовку с одним общим ARGB-цветом, поэтому тут же
 * подменяется итоговый {@code color} для {@link EndCrystalModel}, если у
 * модуля выбран Custom-режим окраски.
 */
@Mixin(Model.class)
public abstract class EndCrystalModelTintMixin {

    @WrapOperation(
            method = "renderToBuffer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void lovisual$tintEndCrystal(ModelPart root, PoseStack poseStack, VertexConsumer buffer, int light,
                                          int overlay, int color, Operation<Void> original) {
        if ((Object) this instanceof EndCrystalModel) {
            EndCrystal module = Modules.get(EndCrystal.class);
            if (module != null && module.isEnabled()) {
                int tint = module.resolveTintArgb();
                if (tint != -1) {
                    original.call(root, poseStack, buffer, light, overlay, tint);
                    return;
                }
            }
        }
        original.call(root, poseStack, buffer, light, overlay, color);
    }
}
