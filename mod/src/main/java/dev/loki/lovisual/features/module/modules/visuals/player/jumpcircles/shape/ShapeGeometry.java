package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape;

/** Pure geometry for the JumpCircles shapes; every vertex stays inside the unit circle. */
public final class ShapeGeometry {
    public static final float EDGE_SHADE = 0.55f;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final int STAR_POINTS = 5;
    private static final float STAR_INNER = 0.45f;
    private static final float DIAMOND_WIDTH = 0.7f;
    private static final int SPIRAL_ARMS = 3;
    private static final int HEX_SIDES = 6;
    private static final float WAVE_AMPLITUDE = 0.08f;
    private static final float WAVE_COUNT = 6.0f;
    private static final float CROSS_CORE = 0.12f;
    private static final int PETAL_COUNT = 6;
    private static final float PETAL_CENTER = 0.55f;
    private static final float PETAL_LENGTH = 0.45f;
    private static final float PETAL_WIDTH = 0.2f;

    private ShapeGeometry() {
    }

    /**
     * Rebuilds {@code out} for the given shape.
     *
     * @param segments  detail per arm, petal or ring (used by SPIRAL, WAVE, PETALS)
     * @param thickness ring band width in unit radius (used by SPIRAL, HEXAGON, WAVE)
     * @param phase     wave travel phase in radians (used by WAVE)
     */
    public static void build(Shape shape, ShapeMesh out, int segments, float thickness, float phase) {
        out.clear();
        int seg = Math.max(3, segments);
        float band = Math.max(0.02f, Math.min(0.5f, thickness));
        switch (shape) {
            case STAR -> star(out);
            case DIAMOND -> diamond(out);
            case SPIRAL -> spiral(out, seg, band);
            case HEXAGON -> hexagon(out, band);
            case WAVE -> wave(out, Math.max(8, seg), band, phase);
            case CROSS -> cross(out);
            case PETALS -> petals(out, seg);
        }
    }

    private static void star(ShapeMesh m) {
        int center = m.vertex(0, 0, 0.5f, 0);
        int n = STAR_POINTS * 2;
        int prev = -1;
        for (int i = 0; i <= n; i++) {
            int k = i % n;
            boolean tip = (k & 1) == 0;
            float a = TWO_PI * k / n - (float) Math.PI / 2f;
            float r = tip ? 1f : STAR_INNER;
            int v = m.vertex((float) Math.cos(a) * r, (float) Math.sin(a) * r,
                    seamless((float) k / n), tip ? EDGE_SHADE : 0.2f);
            if (prev >= 0) m.triangle(center, prev, v);
            prev = v;
        }
    }

    private static void diamond(ShapeMesh m) {
        int center = m.vertex(0, 0, 0.5f, 0);
        int north = m.vertex(0, -1, 0f, EDGE_SHADE);
        int east = m.vertex(DIAMOND_WIDTH, 0, 0.5f, EDGE_SHADE);
        int south = m.vertex(0, 1, 1f, EDGE_SHADE);
        int west = m.vertex(-DIAMOND_WIDTH, 0, 0.5f, EDGE_SHADE);
        m.triangle(center, north, east);
        m.triangle(center, east, south);
        m.triangle(center, south, west);
        m.triangle(center, west, north);
    }

    private static void spiral(ShapeMesh m, int segments, float band) {
        for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
            float start = TWO_PI * arm / SPIRAL_ARMS;
            int prevIn = -1;
            int prevOut = -1;
            for (int k = 0; k <= segments; k++) {
                float u = (float) k / segments;
                float a = start + u * TWO_PI * 0.3f;
                float center = (1f - band * 0.5f) * (0.5f + 0.5f * u);
                float half = band * 0.5f * (float) Math.sin(Math.PI * u);
                float grad = seamless((arm + u) / SPIRAL_ARMS);
                int in = polar(m, a, center - half, grad, 0f);
                int out = polar(m, a, center + half, grad, EDGE_SHADE);
                strip(m, prevIn, prevOut, in, out);
                prevIn = in;
                prevOut = out;
            }
        }
    }

    private static void hexagon(ShapeMesh m, float band) {
        for (int i = 0; i < HEX_SIDES; i++) {
            float a0 = TWO_PI * i / HEX_SIDES;
            float a1 = TWO_PI * (i + 1) / HEX_SIDES;
            float shade = (i & 1) == 0 ? 0f : EDGE_SHADE;
            float grad = seamless((float) i / HEX_SIDES);
            int in0 = polar(m, a0, 1f - band, grad, shade);
            int out0 = polar(m, a0, 1f, grad, shade);
            int in1 = polar(m, a1, 1f - band, grad, shade);
            int out1 = polar(m, a1, 1f, grad, shade);
            strip(m, in0, out0, in1, out1);
        }
    }

    private static void wave(ShapeMesh m, int segments, float band, float phase) {
        int prevIn = -1;
        int prevOut = -1;
        int firstIn = -1;
        int firstOut = -1;
        for (int k = 0; k < segments; k++) {
            float a = TWO_PI * k / segments;
            float outer = 1f - WAVE_AMPLITUDE + WAVE_AMPLITUDE * (float) Math.sin(WAVE_COUNT * a + phase);
            float grad = seamless((float) k / segments);
            int in = polar(m, a, Math.max(0f, outer - band), grad, 0f);
            int out = polar(m, a, outer, grad, EDGE_SHADE);
            if (firstIn < 0) {
                firstIn = in;
                firstOut = out;
            }
            strip(m, prevIn, prevOut, in, out);
            prevIn = in;
            prevOut = out;
        }
        strip(m, prevIn, prevOut, firstIn, firstOut);
    }

    private static void cross(ShapeMesh m) {
        int c0 = m.vertex(CROSS_CORE, -CROSS_CORE, 0.5f, 0f);
        int c1 = m.vertex(CROSS_CORE, CROSS_CORE, 0.5f, 0f);
        int c2 = m.vertex(-CROSS_CORE, CROSS_CORE, 0.5f, 0f);
        int c3 = m.vertex(-CROSS_CORE, -CROSS_CORE, 0.5f, 0f);
        m.triangle(c0, c1, c2);
        m.triangle(c0, c2, c3);
        m.triangle(c0, c1, m.vertex(1f, 0f, 0f, EDGE_SHADE));
        m.triangle(c1, c2, m.vertex(0f, 1f, 0.5f, EDGE_SHADE));
        m.triangle(c2, c3, m.vertex(-1f, 0f, 1f, EDGE_SHADE));
        m.triangle(c3, c0, m.vertex(0f, -1f, 0.5f, EDGE_SHADE));
    }

    private static void petals(ShapeMesh m, int segments) {
        for (int p = 0; p < PETAL_COUNT; p++) {
            float a = TWO_PI * p / PETAL_COUNT;
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            float grad = seamless((float) p / PETAL_COUNT);
            int center = m.vertex(cos * PETAL_CENTER, sin * PETAL_CENTER, grad, 0f);
            int prev = -1;
            for (int k = 0; k <= segments; k++) {
                float u = TWO_PI * k / segments;
                float along = PETAL_CENTER + PETAL_LENGTH * (float) Math.cos(u);
                float side = PETAL_WIDTH * (float) Math.sin(u);
                int v = m.vertex(cos * along - sin * side, sin * along + cos * side, grad, EDGE_SHADE);
                if (prev >= 0) m.triangle(center, prev, v);
                prev = v;
            }
        }
    }

    private static int polar(ShapeMesh m, float angle, float radius, float gradient, float shade) {
        return m.vertex((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, gradient, shade);
    }

    /** Joins two consecutive inner/outer vertex pairs of a band; the first pair has index -1 and is skipped. */
    private static void strip(ShapeMesh m, int in0, int out0, int in1, int out1) {
        if (in0 < 0) return;
        m.triangle(in0, out0, out1);
        m.triangle(in0, out1, in1);
    }

    /** Maps [0,1] to a there-and-back ramp so a gradient wraps around a closed ring without a seam. */
    private static float seamless(float f) {
        return 1f - Math.abs(2f * f - 1f);
    }
}
