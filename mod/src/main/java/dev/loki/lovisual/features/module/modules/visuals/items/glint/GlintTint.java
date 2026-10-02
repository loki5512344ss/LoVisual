package dev.loki.lovisual.features.module.modules.visuals.items.glint;

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

    /** Colours the glint streaks in a style; the pixel's strongest channel keeps the streak shape. */
    public static int pixel(int argb, float u, float v, String style, int primary, int secondary, float strength) {
        if (HIDDEN.equals(style)) return argb & 0x00FFFFFF;
        float level = Math.min(1.0f, (Math.max(argb >> 16 & 0xFF, Math.max(argb >> 8 & 0xFF, argb & 0xFF)) / 255.0f) * strength);
        int color = switch (style) {
            case RAINBOW -> Mth.hsvToRgb(frac(u * (1.0f - RAINBOW_SKEW) + v * RAINBOW_SKEW), 1.0f, 1.0f);
            case GRADIENT -> lerp(primary, secondary, frac(u * (1.0f - GRADIENT_SKEW) + v * GRADIENT_SKEW));
            default -> primary;
        };
        int r = Math.round((color >> 16 & 0xFF) * level);
        int g = Math.round((color >> 8 & 0xFF) * level);
        int b = Math.round((color & 0xFF) * level);
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
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
