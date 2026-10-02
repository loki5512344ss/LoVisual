package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

import java.util.Locale;

/**
 * Форма ленты шлейфа (как у evo ElytraTrails): сужение вдоль следа, реакция на скорость, плавное начало,
 * затухание у камеры и колебания. Чистые функции, без зависимостей от Minecraft.
 */
public final class RibbonShape {

    static final float SPEED_MIN = 0.35f;
    static final float SPEED_MAX = 1.6f;
    static final float SPEED_WIDTH_FLOOR = 0.45f;
    static final float SPEED_ALPHA_FLOOR = 0.25f;
    static final float HEAD_RAMP_POINTS = 3.0f;
    static final float NEAR_FADE_START = 0.75f;
    static final float NEAR_FADE_RANGE = 1.5f;
    private static final float TAU = (float) (Math.PI * 2.0);

    private RibbonShape() {
    }

    /** Как меняется ширина от головы к хвосту. */
    public enum Taper {
        UNIFORM, GROW, SHRINK;

        public static Taper of(String name) {
            if (name == null) return UNIFORM;
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "grow" -> GROW;
                case "shrink" -> SHRINK;
                default -> UNIFORM;
            };
        }
    }

    /** Форма пути, в которую «вплетена» лента. */
    public enum Motion {
        STRAIGHT, WAVE, HELIX;

        public static Motion of(String name) {
            if (name == null) return STRAIGHT;
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "wave" -> WAVE;
                case "helix" -> HELIX;
                default -> STRAIGHT;
            };
        }
    }

    /** Удалённость точки от головы: 0 у самой новой, 1 у самой старой. */
    public static float tailness(int index, int count) {
        if (count <= 1) return 0.0f;
        return 1.0f - index / (float) (count - 1);
    }

    /** Множитель ширины: у головы 1, у хвоста {@code amount} (Grow) или {@code 1/amount} (Shrink). */
    public static float taper(Taper mode, float amount, float tailness) {
        return switch (mode) {
            case GROW -> 1.0f + tailness * (amount - 1.0f);
            case SHRINK -> 1.0f + tailness * (1.0f / amount - 1.0f);
            default -> 1.0f;
        };
    }

    /** 0 на медленном планировании, 1 на полной скорости. */
    public static float speedFactor(float speed) {
        float v = (speed - SPEED_MIN) / (SPEED_MAX - SPEED_MIN);
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }

    public static float speedWidth(boolean reactive, float speed) {
        return reactive ? SPEED_WIDTH_FLOOR + (1.0f - SPEED_WIDTH_FLOOR) * speedFactor(speed) : 1.0f;
    }

    public static float speedAlpha(boolean reactive, float speed) {
        return reactive ? SPEED_ALPHA_FLOOR + (1.0f - SPEED_ALPHA_FLOOR) * speedFactor(speed) : 1.0f;
    }

    /** Плавное начало: самые новые точки прозрачны и проявляются за три точки. */
    public static float headRamp(boolean fadeStart, int index, int count) {
        if (!fadeStart) return 1.0f;
        float v = (count - 1 - index) / HEAD_RAMP_POINTS;
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }

    /** Лента гаснет, когда камера вплотную к ней. */
    public static float nearFade(float distance) {
        float v = (distance - NEAR_FADE_START) / NEAR_FADE_RANGE;
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }

    /** Оставшаяся жизнь точки, линейно от 1 до 0. */
    public static float life(long now, long birthMs, long lifeMs) {
        if (lifeMs <= 0) return 0.0f;
        float v = 1.0f - (now - birthMs) / (float) lifeMs;
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }

    /**
     * Смещение точки от оси пути: {в сторону, по бинормали}. Зависит от времени рождения точки,
     * поэтому узор едет вместе с точкой, а не пляшет на месте.
     */
    public static float[] motion(Motion mode, long birthMs, float amplitude, float frequency) {
        if (mode == Motion.STRAIGHT) return new float[]{0.0f, 0.0f};
        double cycles = (birthMs * 0.001 * frequency) % 1.0;
        float phase = (float) cycles * TAU;
        float side = (float) Math.sin(phase) * amplitude;
        float binormal = mode == Motion.HELIX ? (float) Math.cos(phase) * amplitude : 0.0f;
        return new float[]{side, binormal};
    }
}
