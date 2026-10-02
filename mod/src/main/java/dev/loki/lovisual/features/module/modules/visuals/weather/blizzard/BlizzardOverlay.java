package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Белая дымка и обмерзание краёв экрана: слой из полупрозрачных рамок,
 * каждая следующая тоньше, поэтому альфа копится к самому краю.
 */
final class BlizzardOverlay {

    static final int FROST_BANDS = 16;
    static final float MAX_VEIL = 0.85f;
    static final float VEIL_SCALE = 0.55f;
    static final float FROST_TOTAL = 0.6f;

    private BlizzardOverlay() {
    }

    static void render(GuiGraphicsExtractor ctx, float strength, float haze,
                       boolean frost, float frostStrength, int tintArgb) {
        if (ctx == null || strength <= 0.01f) return;
        int w = ctx.guiWidth();
        int h = ctx.guiHeight();
        if (w <= 0 || h <= 0) return;
        float veil = veilAlpha(haze, strength);
        if (veil > 0.004f) ctx.fill(0, 0, w, h, withAlpha(tintArgb, veil));
        if (!frost || frostStrength <= 0.01f) return;
        float total = frostTotal(strength, frostStrength);
        if (total <= 0.004f) return;
        int color = withAlpha(tintArgb, frostBandAlpha(total));
        int depth = Math.max(2, Math.round(Math.min(w, h) * 0.20f));
        for (int i = 0; i < FROST_BANDS; i++) {
            int thickness = bandThickness(i, FROST_BANDS, depth);
            ctx.fill(0, 0, w, thickness, color);
            ctx.fill(0, h - thickness, w, h, color);
            ctx.fill(0, thickness, thickness, h - thickness, color);
            ctx.fill(w - thickness, thickness, w, h - thickness, color);
        }
    }

    /** Непрозрачность белой дымки на весь экран: растёт с дымкой и силой шторма, ограничена {@link #MAX_VEIL}. */
    static float veilAlpha(float haze, float strength) {
        return Math.max(0.0f, Math.min(MAX_VEIL, haze * strength * VEIL_SCALE));
    }

    /** Суммарная непрозрачность обмерзания у самого края. */
    static float frostTotal(float strength, float frostStrength) {
        return Math.max(0.0f, FROST_TOTAL * strength * frostStrength);
    }

    /** Непрозрачность одной полосы обмерзания: все полосы вместе дают {@code total}. */
    static float frostBandAlpha(float total) {
        return total / FROST_BANDS;
    }

    /** Толщина i-й полосы: первая на всю глубину, каждая следующая тоньше, поэтому альфа копится у края. */
    static int bandThickness(int i, int bands, int depth) {
        return Math.max(1, Math.round(depth * (1.0f - (float) Math.sqrt(i / (double) bands))));
    }

    static int withAlpha(int argb, float alpha) {
        int a = Math.round(alpha * 255.0f);
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Доля полос обмерзания, покрывающая точку на distance от края экрана. */
    static float edgeCoverage(float distance, float depth, int bands) {
        if (bands <= 0 || depth <= 0.0f) return 0.0f;
        if (distance <= 0.0f) return 1.0f;
        int covered = 0;
        for (int i = 0; i < bands; i++) {
            float inset = depth * (1.0f - (float) Math.sqrt(i / (double) bands));
            if (distance < inset) covered++;
        }
        return covered / (float) bands;
    }
}
