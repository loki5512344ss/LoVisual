package dev.loki.lovisual.features.module.modules.visuals.knife.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.loki.lovisual.features.module.modules.visuals.knife.KnifeSkin;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeAnimator;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeClip;
import dev.loki.lovisual.features.module.modules.visuals.knife.model.KnifeModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;

/**
 * Draws a knife skin as the first person view model (with the player's own arms attached at the model's hand
 * markers) and in a third person hand. The pose stack is expected at the camera, like vanilla's hand rendering.
 */
public final class KnifeRenderer {
    private static final float EQUIP_DROP = -0.6f;
    private static final float ARM_ALIGN_X = 0.0625f;
    private static final float ARM_ALIGN_Y = 0.5f;
    private static final float THIRD_HAND_X = 1.0f / 16.0f;
    private static final float THIRD_HAND_Y = 0.125f;
    private static final float THIRD_HAND_Z = -0.625f;

    private final KnifeBones primary = new KnifeBones();
    private final KnifeBones rest = new KnifeBones();
    private final Matrix4f armPose = new Matrix4f();

    public void firstPerson(AbstractClientPlayer player, KnifeSkin skin, KnifeModel model, KnifeAnimator animator,
                            PoseStack stack, SubmitNodeCollector collector, int light, float retire) {
        float[] pose = animator.pose();
        boolean mirrored = player.getMainArm() == HumanoidArm.LEFT;
        stack.pushPose();
        stack.translate(0.0f, retire * EQUIP_DROP, 0.0f);
        if (mirrored) stack.scale(-1.0f, 1.0f, 1.0f);
        applyCamera(model, pose, stack);
        applyViewAnchor(model, stack);
        primary.build(model, pose, animator.hidden());
        KnifeGeometry.submit(primary, model, skin.texture(), stack, collector, light);
        arm(model.rightHand(), true, mirrored, player, stack, collector, light);
        arm(model.leftHand(), false, mirrored, player, stack, collector, light);
        stack.popPose();
    }

    /** Draws the resting knife in the hand the stack has been moved to (see KnifeHandLayerMixin). */
    public void thirdPerson(KnifeSkin skin, KnifeModel model, HumanoidArm arm, PoseStack stack,
                            SubmitNodeCollector collector, int light) {
        KnifeSkin.Transform place = arm == HumanoidArm.RIGHT ? skin.thirdRight() : skin.thirdLeft();
        stack.pushPose();
        stack.translate((arm == HumanoidArm.LEFT ? -1.0f : 1.0f) * THIRD_HAND_X, THIRD_HAND_Y, THIRD_HAND_Z);
        stack.translate(place.translation()[0] / 16.0f, place.translation()[1] / 16.0f, place.translation()[2] / 16.0f);
        stack.mulPose(Axis.XP.rotationDegrees(place.rotation()[0]));
        stack.mulPose(Axis.YP.rotationDegrees(place.rotation()[1]));
        stack.mulPose(Axis.ZP.rotationDegrees(place.rotation()[2]));
        stack.scale(place.scale()[0], place.scale()[1], place.scale()[2]);
        stack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        int hand = model.thirdPersonHand();
        if (hand >= 0) stack.translate(-model.pivotX(hand) / 16.0f, -model.pivotY(hand) / 16.0f, -model.pivotZ(hand) / 16.0f);
        rest.build(model, null, null);
        KnifeGeometry.submit(rest, model, skin.texture(), stack, collector, light);
        stack.popPose();
    }

    /** The model's camera bone shakes the view during inspects and attacks. */
    private static void applyCamera(KnifeModel model, float[] pose, PoseStack stack) {
        int camera = model.camera();
        int at = camera * KnifeClip.STRIDE;
        if (camera < 0 || at + KnifeClip.STRIDE > pose.length) return;
        stack.translate(pose[at + 3] / 16.0f, -pose[at + 4] / 16.0f, -pose[at + 5] / 16.0f);
        stack.mulPose(Axis.ZP.rotationDegrees(-pose[at + 2]));
        stack.mulPose(Axis.YP.rotationDegrees(pose[at + 1]));
        stack.mulPose(Axis.XP.rotationDegrees(pose[at]));
    }

    private static void applyViewAnchor(KnifeModel model, PoseStack stack) {
        int anchor = model.viewAnchor();
        if (anchor < 0) return;
        stack.translate(-model.pivotX(anchor) / 16.0f, -model.pivotY(anchor) / 16.0f, -model.pivotZ(anchor) / 16.0f);
    }

    private void arm(int bone, boolean right, boolean mirrored, AbstractClientPlayer player, PoseStack stack,
                     SubmitNodeCollector collector, int light) {
        if (bone < 0) return;
        AvatarRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        if (renderer == null) return;
        Identifier skin = player.getSkin().body().texturePath();
        armPose.set(primary.world[bone]).translate(right ? -ARM_ALIGN_X : ARM_ALIGN_X, ARM_ALIGN_Y, 0.0f);
        stack.pushPose();
        stack.mulPose(armPose);
        if (mirrored) stack.scale(-1.0f, 1.0f, 1.0f);
        if (right) renderer.renderRightHand(stack, collector, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        else renderer.renderLeftHand(stack, collector, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        stack.popPose();
    }
}
