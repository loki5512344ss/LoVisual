package dev.loki.lovisual.util.bedrock.geo;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * One bone of a Bedrock model. Pivots and offsets are in model pixels, rotations in radians, already in the
 * entity frame (x mirrored). The {@code rot/offset/scale} arrays are the animated pose, the {@code base*}
 * ones the pose stored in the file.
 */
public final class GeoBone {
    public final String name;
    public final float[] pivot;
    public final float[] baseRot;
    public final float[] rot = new float[3];
    public final float[] offset = new float[3];
    public final float[] scale = {1.0f, 1.0f, 1.0f};
    public final List<GeoCube> cubes = new ArrayList<>();
    public final List<GeoBone> children = new ArrayList<>();

    GeoBone(String name, float[] pivot, float[] baseRot) {
        this.name = name;
        this.pivot = pivot;
        this.baseRot = baseRot;
        resetPose();
    }

    public void resetPose() {
        System.arraycopy(baseRot, 0, rot, 0, 3);
        java.util.Arrays.fill(offset, 0.0f);
        java.util.Arrays.fill(scale, 1.0f);
    }

    /** Applies this bone's pose to {@code matrix}: offset, then rotation and scale about the pivot. */
    public Matrix4f transform(Matrix4f matrix) {
        return matrix.translate(offset[0] / 16.0f, offset[1] / 16.0f, offset[2] / 16.0f)
                .translate(pivot[0] / 16.0f, pivot[1] / 16.0f, pivot[2] / 16.0f)
                .rotateZYX(rot[2], rot[1], rot[0])
                .scale(scale[0], scale[1], scale[2])
                .translate(-pivot[0] / 16.0f, -pivot[1] / 16.0f, -pivot[2] / 16.0f);
    }
}
