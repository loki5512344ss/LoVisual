package dev.loki.lovisual.render.engine.renderer.warp;

/**
 * Pure numeric helpers backing {@link RenderWarp} construction: unit-square homography solve,
 * perspective corner projection and scalar clamping. Stateless by design.
 */
final class RenderWarpGeometry {
    private RenderWarpGeometry() {
    }

    static double[] computeUnitSquareHomography(float x0, float y0,
                                                 float x1, float y1,
                                                 float x2, float y2,
                                                 float x3, float y3) {
        double dx1 = x1 - x2;
        double dy1 = y1 - y2;
        double dx2 = x3 - x2;
        double dy2 = y3 - y2;
        double dx3 = x0 - x1 + x2 - x3;
        double dy3 = y0 - y1 + y2 - y3;
        double det = dx1 * dy2 - dx2 * dy1;

        double g = 0.0;
        double h = 0.0;
        if (Math.abs(det) > 0.000001) {
            g = (dx3 * dy2 - dx2 * dy3) / det;
            h = (dx1 * dy3 - dx3 * dy1) / det;
        }

        double a = x1 - x0 + g * x1;
        double b = x3 - x0 + h * x3;
        double c = x0;
        double d = y1 - y0 + g * y1;
        double e = y3 - y0 + h * y3;
        double f = y0;
        return new double[]{a, b, c, d, e, f, g, h};
    }

    static float[] projectCorner(float localX,
                                 float localY,
                                 float yawDeg,
                                 float pitchDeg,
                                 float rollDeg,
                                 float cameraDistance,
                                 float perspective,
                                 float scale,
                                 float centerX,
                                 float centerY) {
        float yaw = (float) Math.toRadians(clamp(yawDeg, -85.0f, 85.0f));
        float pitch = (float) Math.toRadians(clamp(pitchDeg, -85.0f, 85.0f));
        float roll = (float) Math.toRadians(rollDeg);

        float x0 = localX;
        float y0 = localY;
        float z0 = 0.0f;

        float cosX = (float) Math.cos(pitch);
        float sinX = (float) Math.sin(pitch);
        float y1 = y0 * cosX - z0 * sinX;
        float z1 = y0 * sinX + z0 * cosX;
        float x1 = x0;

        float cosY = (float) Math.cos(yaw);
        float sinY = (float) Math.sin(yaw);
        float x2 = x1 * cosY + z1 * sinY;
        float z2 = -x1 * sinY + z1 * cosY;
        float y2 = y1;

        float cosZ = (float) Math.cos(roll);
        float sinZ = (float) Math.sin(roll);
        float x3 = x2 * cosZ - y2 * sinZ;
        float y3 = x2 * sinZ + y2 * cosZ;
        float z3 = z2;

        float denom = Math.max(cameraDistance * 0.15f, cameraDistance + z3 * perspective);
        float p = cameraDistance / denom;
        return new float[]{centerX + x3 * p * scale, centerY + y3 * p * scale};
    }

    static float clampStrength(float value) {
        return clamp(value, -4.0f, 4.0f);
    }

    static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
