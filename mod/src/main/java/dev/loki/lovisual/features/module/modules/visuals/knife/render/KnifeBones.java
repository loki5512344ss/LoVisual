package dev.loki.lovisual.features.module.modules.visuals.knife.render;

import dev.loki.lovisual.features.module.modules.visuals.knife.anim.KnifeClip;
import dev.loki.lovisual.features.module.modules.visuals.knife.model.KnifeModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** World matrices of a posed knife model, one per bone, plus which bones are drawn. */
final class KnifeBones {
    private static final float DEGREES = (float) (Math.PI / 180.0);

    Matrix4f[] world = new Matrix4f[0];
    boolean[] visible = new boolean[0];
    private final Matrix4f local = new Matrix4f();
    private final Quaternionf rotation = new Quaternionf();

    /**
     * Poses every bone. {@code pose} holds nine floats per bone (see {@link KnifeClip}); a null pose is the
     * model at rest. Bones flagged in {@code hidden} and the two hand marker bones are not drawn.
     */
    void build(KnifeModel model, float[] pose, boolean[] hidden) {
        int count = model.boneCount();
        ensure(count);
        for (int i = 0; i < count; i++) {
            visible[i] = hidden == null || i >= hidden.length || !hidden[i];
            int at = i * KnifeClip.STRIDE;
            boolean posed = pose != null && at + KnifeClip.STRIDE <= pose.length;
            float rx = model.restX(i) - (posed ? pose[at] * DEGREES : 0.0f);
            float ry = model.restY(i) - (posed ? pose[at + 1] * DEGREES : 0.0f);
            float rz = model.restZ(i) + (posed ? pose[at + 2] * DEGREES : 0.0f);

            local.identity();
            if (posed) local.translate(-pose[at + 3] / 16.0f, pose[at + 4] / 16.0f, pose[at + 5] / 16.0f);
            float px = model.pivotX(i) / 16.0f;
            float py = model.pivotY(i) / 16.0f;
            float pz = model.pivotZ(i) / 16.0f;
            local.translate(px, py, pz);
            local.rotate(rotation.rotationZ(rz).rotateY(ry).rotateX(rx));
            if (posed) local.scale(pose[at + 6], pose[at + 7], pose[at + 8]);
            local.translate(-px, -py, -pz);

            int parent = model.parentOf(i);
            if (parent < 0) world[i].set(local);
            else world[i].set(world[parent]).mul(local);
        }
        hideHands(model);
    }

    private void hideHands(KnifeModel model) {
        int right = model.rightHand();
        int left = model.leftHand();
        if (right >= 0) visible[right] = false;
        if (left >= 0) visible[left] = false;
    }

    private void ensure(int count) {
        if (world.length >= count) return;
        Matrix4f[] grown = new Matrix4f[count];
        System.arraycopy(world, 0, grown, 0, world.length);
        for (int i = world.length; i < count; i++) grown[i] = new Matrix4f();
        world = grown;
        visible = new boolean[count];
    }
}
