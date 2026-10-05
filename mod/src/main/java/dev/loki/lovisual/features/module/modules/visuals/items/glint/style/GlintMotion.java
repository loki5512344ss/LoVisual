package dev.loki.lovisual.features.module.modules.visuals.items.glint.style;

import org.joml.Matrix4f;

/** Texture-matrix animation of the procedural glint styles; null for styles that keep vanilla motion. */
public final class GlintMotion {
    private static final float DEFAULT_ROTATION = 0.17453292f;

    private GlintMotion() {
    }

    /** {@code seconds} is the speed-scaled clock; {@code scale} the vanilla texture scale. */
    public static Matrix4f matrix(String style, float seconds, float scale) {
        return switch (style) {
            case GlintStyles.PULSE -> build(seconds * 0.073f, seconds * 0.267f, DEFAULT_ROTATION,
                    scale * (1.0f + 0.22f * (float) Math.sin(seconds * 1.6f)));
            case GlintStyles.FIRE -> build(0.05f * (float) Math.sin(seconds * 0.7f), -seconds * 0.45f, 0.0f, scale);
            case GlintStyles.GALAXY -> build(seconds * 0.02f, seconds * 0.015f, DEFAULT_ROTATION + seconds * 0.035f, scale);
            case GlintStyles.HOLOGRAM -> build(seconds * 0.04f, seconds * 0.7f, 0.0f, scale);
            case GlintStyles.GLITCH -> {
                int step = (int) Math.floor(seconds * 7.0f);
                yield build(GlintNoise.hash(step, 0, 64, 31), GlintNoise.hash(0, step, 64, 47), 0.0f, scale);
            }
            case GlintStyles.SPARKLE -> build(seconds * 0.11f, seconds * 0.07f, DEFAULT_ROTATION + seconds * 0.14f, scale);
            case GlintStyles.AURORA -> build(seconds * 0.05f, seconds * 0.12f, 0.08726646f, scale);
            default -> null;
        };
    }

    private static Matrix4f build(float du, float dv, float rotation, float scale) {
        return new Matrix4f().translation(-wrap(du), wrap(dv), 0.0f).rotateZ(rotation).scale(scale);
    }

    static float wrap(float x) {
        return x - (float) Math.floor(x);
    }
}
