package dev.loki.lovisual.render.engine.renderer.quick;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;

/**
 * Short static entry point for everyday UI drawing on {@link Renderer2D#COLOR}: no softness or
 * mask parameters, color last, one radius. Use the full Renderer2D facade for anything exotic.
 * All calls are no-ops until the renderer is initialised.
 */
public final class Draw {
    private static final float SHADOW_INNER_ALPHA = 0.03f;
    private static final float SHINE_ANGLE = 135f;

    private Draw() {
    }

    public static void rect(float x, float y, float w, float h, int argb) {
        rect(x, y, w, h, 0f, argb);
    }

    public static void rect(float x, float y, float w, float h, float radius, int argb) {
        Renderer2D r = Renderer2D.COLOR;
        if (r != null) r.roundedRect(x, y, w, h, radius, argb);
    }

    public static void rect(float x, float y, float w, float h,
                            float topLeft, float topRight, float bottomRight, float bottomLeft, int argb) {
        Renderer2D r = Renderer2D.COLOR;
        if (r != null) r.roundedRectCorners(x, y, w, h, topLeft, topRight, bottomRight, bottomLeft, argb);
    }

    public static void gradient(float x, float y, float w, float h, float radius,
                                int startArgb, int endArgb, float angleDeg) {
        Renderer2D r = Renderer2D.COLOR;
        if (r != null) r.roundedRectGradient(x, y, w, h, radius, startArgb, endArgb, angleDeg);
    }

    public static void outline(float x, float y, float w, float h, float radius, float thickness, int argb) {
        Renderer2D r = Renderer2D.COLOR;
        if (r != null) r.roundedRectStroke(x, y, w, h, radius, thickness, argb);
    }

    /** White outline that shines at the top-left and bottom-right corners, like a glass edge. */
    public static void glassOutline(float x, float y, float w, float h, float radius, float thickness,
                                    float alpha, float shine) {
        Renderer2D r = Renderer2D.COLOR;
        if (r == null) return;
        int[] c = GlassEdge.stops(alpha, shine);
        r.roundedRectStrokeGradient(x, y, w, h, radius, thickness, c[0], c[1], SHINE_ANGLE);
        r.roundedRectStrokeGradient(x, y, w, h, radius, thickness, c[2], c[3], SHINE_ANGLE);
    }

    public static void shadow(float x, float y, float w, float h, float radius, float blur, int argb) {
        Renderer2D r = Renderer2D.COLOR;
        if (r != null) r.roundedRectSoftShadow(x, y, w, h, radius, blur, SHADOW_INNER_ALPHA, argb);
    }

    public static void circle(float cx, float cy, float radius, int argb) {
        Renderer2D r = Renderer2D.COLOR;
        if (r != null) r.circle(cx, cy, radius, argb);
    }
}
