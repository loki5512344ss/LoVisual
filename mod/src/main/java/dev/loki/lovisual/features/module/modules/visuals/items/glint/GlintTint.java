package dev.loki.lovisual.features.module.modules.visuals.items.glint;

import dev.loki.lovisual.features.module.modules.visuals.items.glint.style.GlintStyles;
import net.minecraft.util.Mth;

/** Pure pixel maths for repainting the enchantment glint texture (ARGB ints, u and v in 0..1). */
public final class GlintTint {
    public static final String SOLID = "Solid";
    public static final String GRADIENT = "Gradient";
    public static final String RAINBOW = "Rainbow";
    public static final String HIDDEN = "Hidden";

    private static final float RAINBOW_SKEW = 0.25f;
    private static final float GRADIENT_SKEW = 0.25f;

    private GlintTint() {
    }

    public static int pixel(int argb, float u, float v, String style, int primary, int secondary, float strength) {
        return pixel(argb, u, v, style, primary, secondary, strength, 0.0f);
    }

    /**
     * Colours the glint streaks in a style; the pixel's strongest channel keeps the streak shape and
     * {@code coverage} lifts the glow between the streaks.
     */
    public static int pixel(int argb, float u, float v, String style, int primary, int secondary, float strength, float coverage) {
        if (HIDDEN.equals(style)) return argb & 0x00FFFFFF;
        float streak = Math.max(argb >> 16 & 0xFF, Math.max(argb >> 8 & 0xFF, argb & 0xFF)) / 255.0f;
        if (GlintStyles.handles(style)) {
            return (argb & 0xFF000000) | scaled(GlintStyles.shade(style, u, v, streak, primary, secondary, coverage), strength);
        }
        float level = Math.min(1.0f, (coverage + (1.0f - coverage) * streak) * strength);
        int color = switch (style) {
            case RAINBOW -> Mth.hsvToRgb(frac(u * (1.0f - RAINBOW_SKEW) + v * RAINBOW_SKEW), 1.0f, 1.0f);
            case GRADIENT -> lerp(primary, secondary, frac(u * (1.0f - GRADIENT_SKEW) + v * GRADIENT_SKEW));
            default -> primary;
        };
        return (argb & 0xFF000000) | scaled(color, level);
    }

    private static int scaled(int rgb, float factor) {
        int r = Math.min(255, Math.round((rgb >> 16 & 0xFF) * factor));
        int g = Math.min(255, Math.round((rgb >> 8 & 0xFF) * factor));
        int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return r << 16 | g << 8 | b;
    }

    static int lerp(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }

    private static float frac(float x) {
        return x - (float) Math.floor(x);
    }
}
