package dev.loki.lovisual.features.module.modules.visuals.models.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The chicken: textured geometry split into movable parts, drawn in place of the player model. Parts are
 * stored as triangle lists, one file per part and material, read on first use. Limbs swing with the walk, the
 * hitting arm swings out with the attack, and held items ride the paws.
 */
public final class ChickenMesh {
    private static final String[] PARTS = {"body", "head", "right_arm", "left_arm", "right_leg", "left_leg"};
    private static final int RIGHT_ARM = 2;
    private static final int LEFT_ARM = 3;
    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath("lovisual", "models/chicken/rudy_sc_1.png"),
            Identifier.fromNamespaceAndPath("lovisual", "models/chicken/bruce_col.png")
    };
    /** Rotation pivots of each part, measured from the converted mesh, in the model's own space. */
    private static final float[][] PIVOTS = {
            {0, 0, 0}, {0, 0.55f, 0}, {-0.52f, 0.55f, 0}, {0.52f, 0.55f, 0}, {-0.18f, 1.12f, 0}, {0.18f, 1.12f, 0}
    };
    private static final int MAX_VERTICES = 1_000_000;

    private float[][][] geometry;

    public void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        float[][][] data = geometry();
        float stride = Math.min(state.walkAnimationSpeed, 1.0f);
        float swing = (float) Math.cos(state.walkAnimationPos * 0.6662f) * 1.2f * stride;
        float rightArm = -swing;
        float leftArm = swing;
        if (state.attackTime > 0) {
            float hit = (float) Math.sin(Math.sqrt(state.attackTime) * Math.PI) * 1.3f;
            if (state.attackArm == HumanoidArm.RIGHT) rightArm -= hit;
            else leftArm -= hit;
        }
        if (state.isUsingItem) {
            boolean mainHand = state.useItemHand == InteractionHand.MAIN_HAND;
            if (mainHand == (state.mainArm == HumanoidArm.RIGHT)) rightArm = -0.8f;
            else leftArm = -0.8f;
        }
        float[] angles = {0, 0, rightArm, leftArm, swing, -swing};
        for (int part = 0; part < PARTS.length; part++) {
            pose.pushPose();
            rotatePart(pose, part, angles[part]);
            for (int material = 0; material < TEXTURES.length; material++) {
                MeshVertices.submit(pose, collector, TEXTURES[material], data[part][material], light, overlay);
            }
            pose.popPose();
        }
        submitItem(state.rightHandItemState, pose, collector, light, state.outlineColor, RIGHT_ARM, rightArm);
        submitItem(state.leftHandItemState, pose, collector, light, state.outlineColor, LEFT_ARM, leftArm);
    }

    /**
     * One arm on its own, for the hand the player sees in front of them. It is shifted so its shoulder sits where
     * the vanilla arm's shoulder does; the swing, item and equip movement are whatever the game already set up.
     */
    public void submitArm(PoseStack pose, SubmitNodeCollector collector, int light, boolean right) {
        float[][][] data = geometry();
        int part = right ? RIGHT_ARM : LEFT_ARM;
        float[] pivot = PIVOTS[part];
        pose.pushPose();
        pose.translate(-pivot[0], -pivot[1], -pivot[2]);
        for (int material = 0; material < TEXTURES.length; material++) {
            MeshVertices.submit(pose, collector, TEXTURES[material], data[part][material], light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }

    private static void rotatePart(PoseStack pose, int part, float angle) {
        if (angle == 0) return;
        float[] pivot = PIVOTS[part];
        pose.translate(pivot[0], pivot[1], pivot[2]);
        pose.mulPose(Axis.XP.rotation(angle));
        pose.translate(-pivot[0], -pivot[1], -pivot[2]);
    }

    private static void submitItem(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
                                   int light, int outlineColor, int arm, float angle) {
        if (item.isEmpty()) return;
        pose.pushPose();
        rotatePart(pose, arm, angle);
        // the paw tip, with the item in the standard third-person hand orientation
        pose.translate(arm == RIGHT_ARM ? -0.57f : 0.57f, 0.92f, -0.02f);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(arm == RIGHT_ARM ? 0.0625f : -0.0625f, 0.125f, 0);
        item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
        pose.popPose();
    }

    private float[][][] geometry() {
        if (geometry == null) {
            float[][][] all = new float[PARTS.length][TEXTURES.length][];
            for (int part = 0; part < PARTS.length; part++) {
                for (int material = 0; material < TEXTURES.length; material++) {
                    all[part][material] = load(PARTS[part], material);
                }
            }
            geometry = all;
        }
        return geometry;
    }

    private static float[] load(String part, int material) {
        String path = "/assets/lovisual/models/chicken/mesh_" + part + "_" + material + ".bin";
        try (InputStream raw = ChickenMesh.class.getResourceAsStream(path)) {
            return raw == null ? new float[0] : read(raw);
        } catch (IOException | RuntimeException e) {
            return new float[0];
        }
    }

    /** A vertex count followed by that many vertices of eight big-endian floats each. */
    static float[] read(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(raw);
        int count = in.readInt();
        if (count < 0 || count > MAX_VERTICES || count % 3 != 0) throw new IOException("Invalid vertex count");
        float[] data = new float[count * MeshVertices.FLOATS_PER_VERTEX];
        for (int i = 0; i < data.length; i++) data[i] = in.readFloat();
        return data;
    }
}
