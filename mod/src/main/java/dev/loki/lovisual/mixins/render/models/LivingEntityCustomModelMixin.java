package dev.loki.lovisual.mixins.render.models;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.models.CustomModelState;
import dev.loki.lovisual.features.module.modules.visuals.models.CustomModels;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** CustomModels: модель игрока подменяется ровно там, где её передают на отрисовку. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityCustomModelMixin {

    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void lovisual$submitCustomModel(SubmitNodeCollector collector, Model model, Object state, PoseStack pose,
                                            RenderType renderType, int light, int overlay, int color,
                                            TextureAtlasSprite sprite, int outlineColor,
                                            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        String custom = state instanceof CustomModelState carrier ? carrier.lovisual$customModel() : null;
        CustomModels models = custom == null ? null : Modules.get(CustomModels.class);
        if (models != null && state instanceof AvatarRenderState avatarState
                && models.submit(custom, model, avatarState, pose, collector, light, overlay, color, outlineColor)) {
            return;
        }
        // the raw types are needed here: the intercepted call has an erased state type
        collector.submitModel(model, state, pose, renderType, light, overlay, color, sprite, outlineColor, crumbling);
    }
}
