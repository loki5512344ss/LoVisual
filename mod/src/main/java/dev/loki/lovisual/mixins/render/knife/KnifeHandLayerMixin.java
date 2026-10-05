package dev.loki.lovisual.mixins.render.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.knife.Knife;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knife: draws the skin in your own third person hand instead of the sword. */
@Mixin(PlayerItemInHandLayer.class)
public abstract class KnifeHandLayerMixin {
    private static final float HAND_PITCH_DEGREES = -90.0f;
    private static final float HAND_YAW_DEGREES = 180.0f;

    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), cancellable = true)
    private void lovisual$knifeThirdPerson(AvatarRenderState state, ItemStackRenderState itemState, ItemStack item,
                                           HumanoidArm arm, PoseStack matrices, SubmitNodeCollector queue, int light,
                                           CallbackInfo ci) {
        Knife knife = Modules.get(Knife.class);
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (knife == null || player == null || player.getId() != state.id || !Knife.isKnife(item)) return;
        AvatarRenderer<?> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
        if (renderer == null) return;
        matrices.pushPose();
        renderer.getModel().translateToHand(state, arm, matrices);
        matrices.mulPose(Axis.XP.rotationDegrees(HAND_PITCH_DEGREES));
        matrices.mulPose(Axis.YP.rotationDegrees(HAND_YAW_DEGREES));
        boolean drawn = knife.renderThirdPerson(item, arm, matrices, queue, light);
        matrices.popPose();
        if (drawn) ci.cancel();
    }
}
