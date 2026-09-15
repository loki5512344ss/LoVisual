/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.EndCrystal;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Тонировка стекла/куба энд-кристалла и скрытие отдельных частей модели по
 * настройкам {@link EndCrystal}. Ванильный {@code render(...)} зовёт
 * {@code this.glass.render(...)} дважды подряд (внешний куб, затем повёрнутый
 * вложенный) и {@code this.cube.render(...)} один раз — различаем эти три
 * вызова счётчиком {@link #lovisual$glassCallOrdinal}, сбрасываемым в начале
 * каждого рендера кристалла.
 */
@Mixin(EndCrystalRenderer.class)
public abstract class EndCrystalRendererMixin {

    @Shadow
    private ModelPart glass;
    @Shadow
    private ModelPart cube;

    @Unique
    private int lovisual$glassCallOrdinal;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void lovisual$resetOrdinal(Object entity, float entityYaw, float partialTicks, PoseStack poseStack,
                                       Object buffer, int packedLight, CallbackInfo ci) {
        lovisual$glassCallOrdinal = 0;
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V")
    )
    private void lovisual$tintOrHideModelPart(ModelPart instance, PoseStack poseStack, VertexConsumer vertexConsumer,
                                              int light, int overlay, Operation<Void> original) {
        EndCrystal module = Modules.get(EndCrystal.class);
        if (module == null || !module.isEnabled()) {
            original.call(instance, poseStack, vertexConsumer, light, overlay);
            return;
        }

        boolean visible;
        if (instance == this.cube) {
            visible = module.cubeVisible.get();
        } else {
            lovisual$glassCallOrdinal++;
            visible = lovisual$glassCallOrdinal == 1 ? module.outerGlassVisible.get() : module.innerGlassVisible.get();
        }
        if (!visible) return;

        int tint = module.resolveTintArgb();
        if (tint == -1) {
            original.call(instance, poseStack, vertexConsumer, light, overlay);
        } else {
            instance.render(poseStack, vertexConsumer, light, overlay, tint);
        }
    }
}
