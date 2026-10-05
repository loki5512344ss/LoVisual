package dev.loki.lovisual.features.theme;

import java.util.Locale;

/**
 * The animated shader backdrop of a theme, drawn behind the ClickGUI window and tinted with the theme's accents.
 * {@code intensity} 0 means off; {@code speed} and {@code scale} stretch time and the pattern.
 */
public record Backdrop(Mode mode, float intensity, float speed, float scale) {
    public enum Mode {
        NONE, IRIDESCENCE, MAGMA, DIGITAL_RAIN, GRID_HORIZON, SINGULARITY, EMBER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Mode number the shader switches on; 0 for none. */
        public int shaderIndex() {
            return ordinal();
        }

        public static Mode byId(String id) {
            if (id != null) {
                for (Mode mode : values()) {
                    if (mode.id().equalsIgnoreCase(id.trim())) return mode;
                }
            }
            return NONE;
        }
    }

    public static final float MAX_INTENSITY = 1.5f;
    public static final float MIN_SPEED = 0.1f;
    public static final float MAX_SPEED = 3.0f;
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.5f;
    public static final Backdrop NONE = new Backdrop(Mode.NONE, 0.0f, 1.0f, 1.0f);

    public Backdrop {
        mode = mode == null ? Mode.NONE : mode;
        intensity = clamp(intensity, 0.0f, MAX_INTENSITY);
        speed = clamp(speed, MIN_SPEED, MAX_SPEED);
        scale = clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public static Backdrop of(Mode mode, float intensity) {
        return new Backdrop(mode, intensity, 1.0f, 1.0f);
    }

    public boolean isActive() {
        return mode != Mode.NONE && intensity > 0.0f;
    }

    public Backdrop withIntensity(float value) {
        return new Backdrop(mode, value, speed, scale);
    }

    /** Same mode: all numbers blend. Different mode: the new backdrop fades in from nothing. */
    public Backdrop lerp(Backdrop to, float t) {
        float f = clamp(t, 0.0f, 1.0f);
        if (mode != to.mode) return new Backdrop(to.mode, to.intensity * f, to.speed, to.scale);
        return new Backdrop(mode, intensity + (to.intensity - intensity) * f,
                speed + (to.speed - speed) * f, scale + (to.scale - scale) * f);
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
