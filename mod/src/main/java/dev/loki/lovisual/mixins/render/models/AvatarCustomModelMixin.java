package dev.loki.lovisual.mixins.render.models;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.models.CustomModelState;
import dev.loki.lovisual.features.module.modules.visuals.models.CustomModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CustomModels: выбор модели на кадр, без слоёв брони и своя рука от первого лица. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarCustomModelMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void lovisual$captureCustomModel(Avatar avatar, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
        CustomModels models = Modules.get(CustomModels.class);
        ((CustomModelState) state).lovisual$setCustomModel(models == null ? null : models.modelFor(avatar));
    }

    /** Броня, элитры и прочие слои принадлежат модели игрока: у кастомной модели их нет. */
    @Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z",
            at = @At("HEAD"), cancellable = true)
    private void lovisual$hideLayersOnCustomModel(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (((CustomModelState) state).lovisual$customModel() != null) cir.setReturnValue(Boolean.FALSE);
    }

    @Inject(method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void lovisual$customRightHand(PoseStack pose, SubmitNodeCollector collector, int light,
                                          Identifier skin, boolean sleeve, CallbackInfo ci) {
        lovisual$hand(pose, collector, light, true, ci);
    }

    @Inject(method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void lovisual$customLeftHand(PoseStack pose, SubmitNodeCollector collector, int light,
                                         Identifier skin, boolean sleeve, CallbackInfo ci) {
        lovisual$hand(pose, collector, light, false, ci);
    }

    private static void lovisual$hand(PoseStack pose, SubmitNodeCollector collector, int light, boolean right, CallbackInfo ci) {
        CustomModels models = Modules.get(CustomModels.class);
        if (models == null || !models.handFromModel()) return;
        models.submitHand(pose, collector, light, right);
        ci.cancel();
    }
}
