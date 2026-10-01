package dev.loki.lovisual.render.engine.renderer.quick;

/** Alpha stops of the two-layer "glass shine" outline: bright top-left and bottom-right, dim between. */
public final class GlassEdge {
    private static final int WHITE = 0x00FFFFFF;

    private GlassEdge() {
    }

    /**
     * @param alpha overall opacity in [0, 1]
     * @param shine highlight strength in [0, 1]
     * @return {startA, endA, startB, endB} ARGB colors: layer A fades TL to BR, layer B fades BR to TL
     */
    public static int[] stops(float alpha, float shine) {
        float a = clamp01(alpha);
        float s = clamp01(shine);
        return new int[]{
                white(a * (0.35f + 0.30f * s)),
                white(a * 0.08f),
                white(a * 0.06f),
                white(a * (0.22f + 0.28f * s))
        };
    }

    private static int white(float alpha) {
        return (Math.round(clamp01(alpha) * 255f) << 24) | WHITE;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
