package dev.loki.lovisual.features.module.modules.visuals.items.armor;

import net.minecraft.util.Mth;

/** Pure maths for durability-based armor colouring (RGB/ARGB ints). */
public final class ArmorTint {
    public static final String GRADIENT = "Gradient";
    public static final String STEPS = "Steps";
    /** Returned when the armor must keep its own colour. */
    public static final int NO_TINT = 0;

    static final float WARN_FRACTION = 0.5f;
    static final float CRITICAL_FRACTION = 0.25f;
    private static final double PULSE_PERIOD_MS = 190.0;
    private static final float PULSE_DEPTH = 0.4f;
    private static final int WHITE = 0xFFFFFF;

    private ArmorTint() {
    }

    /** Remaining durability 0..1, or -1 for an item without durability. */
    public static float fraction(int damage, int maxDamage) {
        if (maxDamage <= 0) return -1.0f;
        return Mth.clamp(1.0f - (float) damage / maxDamage, 0.0f, 1.0f);
    }

    /** Full above half, warning down to a quarter, broken below; Gradient blends, Steps snaps. */
    public static int ramp(float fraction, String mode, int full, int warn, int broken) {
        float f = Mth.clamp(fraction, 0.0f, 1.0f);
        if (STEPS.equals(mode)) {
            if (f > WARN_FRACTION) return full;
            return f > CRITICAL_FRACTION ? warn : broken;
        }
        return f > WARN_FRACTION
                ? lerpRgb(warn, full, (f - WARN_FRACTION) / WARN_FRACTION)
                : lerpRgb(broken, warn, f / WARN_FRACTION);
    }

    /** Darkens the colour rhythmically, used once the armor is nearly broken. */
    public static int pulse(int rgb, long millis) {
        float wave = (float) ((Math.sin(millis / PULSE_PERIOD_MS) + 1.0) * 0.5);
        float k = 1.0f - PULSE_DEPTH * wave;
        return Math.round((rgb >> 16 & 0xFF) * k) << 16 | Math.round((rgb >> 8 & 0xFF) * k) << 8 | Math.round((rgb & 0xFF) * k);
    }

    /** Final armor tint: white blended towards {@code rgb} by {@code strength}, opaque. */
    public static int tint(int rgb, float strength) {
        return 0xFF000000 | lerpRgb(WHITE, rgb, strength);
    }

    /** Multiplies the layer's own colour (dye) by the tint; {@link #NO_TINT} leaves it alone. */
    public static int apply(int layerColor, int tint) {
        if (tint == NO_TINT) return layerColor;
        int r = (layerColor >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
        int g = (layerColor >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
        int b = (layerColor & 0xFF) * (tint & 0xFF) / 255;
        return (layerColor & 0xFF000000) | r << 16 | g << 8 | b;
    }

    static int lerpRgb(int from, int to, float t) {
        float k = Mth.clamp(t, 0.0f, 1.0f);
        int r = Math.round(Mth.lerp(k, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(k, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(k, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }
}
