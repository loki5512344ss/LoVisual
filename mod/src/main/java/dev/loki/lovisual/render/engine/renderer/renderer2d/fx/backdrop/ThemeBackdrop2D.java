package dev.loki.lovisual.render.engine.renderer.renderer2d.fx.backdrop;

import dev.loki.lovisual.features.theme.Backdrop;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

/** Draws a theme's animated shader backdrop into a rounded rect, optionally as one side of a wipe. */
public final class ThemeBackdrop2D {
    /** How a draw takes part in the theme-change wipe. */
    public enum Wipe {
        NONE(0.0f), REVEAL(1.0f), ERASE(2.0f);

        private final float kind;

        Wipe(float kind) {
            this.kind = kind;
        }
    }

    private static final double CLOCK_WRAP_SECONDS = 4096.0;
    private static final float DARK_SHARE = 0.55f;
    private static final float LIGHT_SHARE = 0.45f;

    private ThemeBackdrop2D() {
    }

    /** The three ramp colours of a backdrop: a darkened accent, the accent, and the accent lifted towards the text colour. */
    public static int[] palette(int accent, int text) {
        return new int[]{scale(accent, DARK_SHARE), accent | 0xFF000000, mix(accent, text, LIGHT_SHARE)};
    }

    public static float clock() {
        return (float) (System.nanoTime() / 1.0E9 % CLOCK_WRAP_SECONDS);
    }

    public static void draw(Renderer2D r, double x, double y, double w, double h, float radius, Backdrop backdrop,
                            int accent, int text, float opacity, Wipe wipe, float wipeProgress) {
        if (w <= 0.0 || h <= 0.0 || !backdrop.isActive() || opacity <= 0.0f) return;
        DrawBatch batch = Batch2D.open(r, UiBatchType.THEME_BACKDROP);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] colors = palette(accent, text);
        int[] first = Batch2D.rgba(colors[0]);
        float[] second = Batch2D.rgbaNorm(colors[1]);
        float[] third = Batch2D.rgbaNorm(colors[2]);
        first[3] = Math.round(Math.max(0.0f, Math.min(opacity, 1.0f)) * 255.0f);
        float time = clock();
        float mode = backdrop.mode().shaderIndex();

        int i1 = vertex(mesh, x, y, x, y, w, h, first, second, third, radius, mode, backdrop, time, wipe, wipeProgress);
        int i2 = vertex(mesh, x, y + h, x, y, w, h, first, second, third, radius, mode, backdrop, time, wipe, wipeProgress);
        int i3 = vertex(mesh, x + w, y + h, x, y, w, h, first, second, third, radius, mode, backdrop, time, wipe, wipeProgress);
        int i4 = vertex(mesh, x + w, y, x, y, w, h, first, second, third, radius, mode, backdrop, time, wipe, wipeProgress);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    private static int vertex(MeshBuilder mesh, double px, double py, double rx, double ry, double w, double h,
                              int[] first, float[] second, float[] third, float radius, float mode, Backdrop backdrop,
                              float time, Wipe wipe, float wipeProgress) {
        return mesh.vec2(px, py).local2(px, py).color(first[0], first[1], first[2], first[3])
                .vec4((float) rx, (float) ry, (float) w, (float) h)
                .vec4(radius, 0.0f, mode, backdrop.intensity())
                .vec4(second[0], second[1], second[2], second[3])
                .vec4(third[0], third[1], third[2], third[3])
                .vec4(time, backdrop.scale(), backdrop.speed(), 0.0f)
                .vec4(0.5f, 0.5f, wipeProgress, wipe.kind)
                .next();
    }

    private static int scale(int argb, float factor) {
        int r = Math.round((argb >> 16 & 0xFF) * factor);
        int g = Math.round((argb >> 8 & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
