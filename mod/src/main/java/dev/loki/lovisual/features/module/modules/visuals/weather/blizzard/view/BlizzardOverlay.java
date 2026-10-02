package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.view;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Дымка и обмерзание краёв экрана по образцу evo: лёгкая заливка на весь экран и 32 рамки, каждая
 * следующая тоньше, поэтому непрозрачность копится к самому краю.
 */
public final class BlizzardOverlay {

    public static final int FROST_BANDS = 32;
    public static final float FROST_DEPTH_SHARE = 0.2f;
    public static final float FROST_EDGE_ALPHA = 0.55f;
    public static final float HAZE_ALPHA = 0.12f;

    private BlizzardOverlay() {
    }

    /** @param current сила шторма * экспозиция * интенсивность */
    public static void render(GuiGraphicsExtractor ctx, float current, boolean frost, int tintArgb) {
        if (ctx == null || current <= 0.01f) return;
        int w = ctx.guiWidth();
        int h = ctx.guiHeight();
        if (w <= 0 || h <= 0) return;
        ctx.fill(0, 0, w, h, withAlpha(tintArgb, hazeAlpha(current)));
        if (!frost) return;
        int color = withAlpha(tintArgb, frostBandAlpha(current));
        float depth = Math.min(w, h) * FROST_DEPTH_SHARE;
        for (int i = 0; i < FROST_BANDS; i++) {
            int t = Math.round(bandThickness(i, FROST_BANDS, depth));
            if (t <= 0) continue;
            ctx.fill(0, 0, w, t, color);
            ctx.fill(0, h - t, w, h, color);
            ctx.fill(0, t, t, h - t, color);
            ctx.fill(w - t, t, w, h - t, color);
        }
    }

    /** Непрозрачность заливки на весь экран. */
    public static float hazeAlpha(float current) {
        return Math.max(0.0f, HAZE_ALPHA * current);
    }

    /** Непрозрачность одной полосы обмерзания: все полосы вместе у края дают {@code 0.55 * current}. */
    public static float frostBandAlpha(float current) {
        return Math.max(0.0f, FROST_EDGE_ALPHA * current / FROST_BANDS);
    }

    /** Толщина i-й полосы: первая на всю глубину, дальше тоньше, у последней почти ноль. */
    public static float bandThickness(int i, int bands, float depth) {
        return depth * (1.0f - (float) Math.sqrt(i / (double) bands));
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Доля полос обмерзания, покрывающая точку на distance от края экрана. */
    public static float edgeCoverage(float distance, float depth, int bands) {
        if (bands <= 0 || depth <= 0.0f) return 0.0f;
        if (distance <= 0.0f) return 1.0f;
        int covered = 0;
        for (int i = 0; i < bands; i++) {
            if (distance < bandThickness(i, bands, depth)) covered++;
        }
        return covered / (float) bands;
    }
}
