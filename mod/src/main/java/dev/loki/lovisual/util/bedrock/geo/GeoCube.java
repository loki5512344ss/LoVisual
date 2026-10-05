package dev.loki.lovisual.util.bedrock.geo;

import org.joml.Matrix4f;

import java.util.List;

/**
 * A cube of a bone: its own pivot and rotation (radians) plus the baked textured faces in model pixels.
 * Faces are stored as four corners (x, y, z, u, v each, in texture pixels) in drawing order.
 */
public record GeoCube(float[] pivot, float[] rot, List<float[]> faces) {

    public boolean rotated() {
        return rot[0] != 0.0f || rot[1] != 0.0f || rot[2] != 0.0f;
    }

    /** Applies the cube's own rotation about its pivot to {@code matrix} (blocks). */
    public Matrix4f transform(Matrix4f matrix) {
        return matrix.translate(pivot[0] / 16.0f, pivot[1] / 16.0f, pivot[2] / 16.0f)
                .rotateZYX(rot[2], rot[1], rot[0])
                .translate(-pivot[0] / 16.0f, -pivot[1] / 16.0f, -pivot[2] / 16.0f);
    }
}
