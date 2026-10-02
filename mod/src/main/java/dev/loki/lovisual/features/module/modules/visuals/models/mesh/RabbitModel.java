package dev.loki.lovisual.features.module.modules.visuals.models.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * The rabbit: a box model drawn in place of the player. It does not animate itself: the player model it
 * replaces is posed by the game and those angles are copied onto the matching parts, so the rabbit walks,
 * swings and looks around exactly as the player would. One instance per player, because a submitted model is
 * only read later and two players sharing one would both come out in the last pose set.
 */
public final class RabbitModel {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("lovisual", "models/rabbit.png");
    /** The arms sit at a slight angle of their own, which the copied pose is added onto. */
    private static final float ARM_TILT = 0.0873f;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public RabbitModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rabbit = mesh.getRoot().addOrReplaceChild("rabbit",
                box(28, 45, -5.0f, -13.0f, -5.0f, 10.0f, 11.0f, 8.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        rabbit.addOrReplaceChild("right_leg", box(0, 32, -2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f),
                PartPose.offset(-3.0f, -2.0f, -1.0f));
        rabbit.addOrReplaceChild("left_leg", box(0, 32, -2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f),
                PartPose.offset(3.0f, -2.0f, -1.0f));
        rabbit.addOrReplaceChild("left_arm", box(24, 16, 0.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f),
                PartPose.offsetAndRotation(5.0f, -13.0f, -1.0f, 0.0f, 0.0f, -ARM_TILT));
        rabbit.addOrReplaceChild("right_arm", box(24, 16, -2.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f),
                PartPose.offsetAndRotation(-5.0f, -13.0f, -1.0f, 0.0f, 0.0f, ARM_TILT));
        // the face is the head's own boxes: ears, cheeks and the muzzle
        rabbit.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0f, 0.0f, -4.0f, 6.0f, 1.0f, 6.0f)
                        .texOffs(0, 45).addBox(-4.0f, -11.0f, -4.0f, 8.0f, 11.0f, 8.0f)
                        .texOffs(56, 0).addBox(-5.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f)
                        .texOffs(56, 0).addBox(3.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f)
                        .texOffs(46, 0).addBox(1.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f)
                        .texOffs(46, 0).addBox(-4.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f),
                PartPose.offset(0.0f, -14.0f, -1.0f));

        root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        body = root.getChild("rabbit");
        head = body.getChild("head");
        leftArm = body.getChild("left_arm");
        rightArm = body.getChild("right_arm");
        leftLeg = body.getChild("left_leg");
        rightLeg = body.getChild("right_leg");
    }

    /** Draws the rabbit in the pose the player model is in this frame. */
    public void submit(PlayerModel vanilla, AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       int light, int overlay, int color, int outlineColor) {
        // the game poses the player model only after handing it over, so its angles are still last frame's
        vanilla.setupAnim(state);
        copyAngles(vanilla);

        pose.pushPose();
        pose.scale(1.25f, 1.25f, 1.25f);
        pose.translate(0.0f, -0.3f, 0.0f);
        collector.submitModelPart(root, pose, RenderTypes.entityTranslucent(TEXTURE), light, overlay,
                (TextureAtlasSprite) null, color, (ModelFeatureRenderer.CrumblingOverlay) null, outlineColor);
        pose.popPose();
    }

    private void copyAngles(PlayerModel vanilla) {
        copy(vanilla.head, head, 0.0f);
        // the body stays upright instead of following the player's own body rotation, which would tip it over
        body.xRot = 0.0f;
        body.yRot = 0.0f;
        body.zRot = 0.0f;
        copy(vanilla.leftArm, leftArm, -ARM_TILT);
        copy(vanilla.rightArm, rightArm, ARM_TILT);
        copy(vanilla.leftLeg, leftLeg, 0.0f);
        copy(vanilla.rightLeg, rightLeg, 0.0f);
    }

    private static void copy(ModelPart from, ModelPart to, float zTilt) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot + zTilt;
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float width, float height, float depth) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth);
    }
}
