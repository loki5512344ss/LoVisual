package dev.loki.lovisual.features.module.modules.visuals.items.glint.style;

/**
 * Colour of one glint texel for the procedural styles. {@code streak} is the vanilla texture's own brightness
 * pattern (0..1), so each style keeps the familiar diagonal streaks and adds its own look on top.
 */
public final class GlintStyles {
    public static final String PULSE = "Pulse";
    public static final String FIRE = "Fire";
    public static final String GALAXY = "Galaxy";
    public static final String HOLOGRAM = "Hologram";
    public static final String GLITCH = "Glitch";
    public static final String SPARKLE = "Sparkle";
    public static final String AURORA = "Aurora";

    private static final float TAU = (float) (Math.PI * 2.0);

    private GlintStyles() {
    }

    public static boolean handles(String style) {
        return switch (style) {
            case PULSE, FIRE, GALAXY, HOLOGRAM, GLITCH, SPARKLE, AURORA -> true;
            default -> false;
        };
    }

    /** True when the style blends two colours rather than using the primary alone. */
    public static boolean usesSecondary(String style) {
        return switch (style) {
            case FIRE, GALAXY, HOLOGRAM, GLITCH, AURORA -> true;
            default -> false;
        };
    }

    /** RGB (0xRRGGBB) before strength is applied; coverage lifts the glow between streaks. */
    public static int shade(String style, float u, float v, float streak, int primary, int secondary, float coverage) {
        float[] color = new float[3];
        float level;
        switch (style) {
            case PULSE -> {
                set(color, primary);
                level = streak * (0.15f + 0.85f * (float) Math.pow(GlintNoise.wave(TAU * u), 2.0));
            }
            case FIRE -> {
                float heat = clamp((GlintNoise.fbm(u, v, 5, 3, 7) - 0.3f) * 2.4f);
                mix(color, primary, secondary, GlintNoise.smooth(heat));
                lift(color, 0.45f + 0.55f * heat, (float) Math.pow(heat, 6.0) * 0.65f);
                level = 0.35f + 0.65f * streak;
            }
            case GALAXY -> {
                float cloud = clamp((GlintNoise.fbm(u, v, 5, 3, 21) - 0.28f) * 2.4f);
                mix(color, primary, secondary, (float) Math.pow(cloud, 3.0));
                lift(color, 0.3f + 0.7f * cloud, GlintNoise.sparkles(u, v, 12, 0.22f, 0.35f, 33));
                level = 0.4f + 0.6f * streak;
            }
            case HOLOGRAM -> {
                float scan = (float) Math.pow(GlintNoise.wave(16.0f * TAU * v), 2.0);
                mix(color, primary, secondary, GlintNoise.wave(TAU * (v * 0.6f + u * 0.15f)));
                level = clamp(streak * (0.35f + 0.8f * scan));
            }
            case GLITCH -> {
                int cx = (int) Math.floor(u * 16.0f);
                int cy = (int) Math.floor(v * 16.0f);
                if (GlintNoise.hash(cx, cy, 16, 11) <= 0.45f) return 0;
                float pick = GlintNoise.hash(cx, cy, 16, 977);
                if (pick > 0.25f) mix(color, primary, secondary, pick);
                else set(color, 0xFFFFFF);
                level = 0.4f + 0.6f * GlintNoise.hash(cx, cy, 16, 613);
            }
            case SPARKLE -> {
                float glitter = GlintNoise.sparkles(u, v, 14, 0.3f, 0.55f, 5);
                set(color, primary);
                for (int i = 0; i < 3; i++) color[i] = 1.0f + 0.55f * (color[i] - 1.0f);
                level = clamp(glitter + streak * 0.25f);
            }
            case AURORA -> {
                float ribbon = GlintNoise.wave(TAU * (u * 2.0f + 0.22f * (float) Math.cos(TAU * v)));
                mix(color, primary, secondary, ribbon);
                level = clamp((float) Math.pow(ribbon, 4.0) * (0.5f + 0.7f * streak));
            }
            default -> {
                return 0;
            }
        }
        float lit = coverage + (1.0f - coverage) * level;
        return pack(color[0] * lit, color[1] * lit, color[2] * lit);
    }

    private static void set(float[] out, int rgb) {
        out[0] = (rgb >> 16 & 0xFF) / 255.0f;
        out[1] = (rgb >> 8 & 0xFF) / 255.0f;
        out[2] = (rgb & 0xFF) / 255.0f;
    }

    private static void mix(float[] out, int primary, int secondary, float t) {
        float[] a = new float[3];
        float[] b = new float[3];
        set(a, primary);
        set(b, secondary);
        for (int i = 0; i < 3; i++) out[i] = b[i] + t * (a[i] - b[i]);
    }

    private static void lift(float[] color, float body, float extra) {
        for (int i = 0; i < 3; i++) color[i] = clamp(color[i] * body + extra);
    }

    private static float clamp(float x) {
        return Math.max(0.0f, Math.min(1.0f, x));
    }

    private static int pack(float r, float g, float b) {
        return Math.round(clamp(r) * 255.0f) << 16 | Math.round(clamp(g) * 255.0f) << 8 | Math.round(clamp(b) * 255.0f);
    }
}
