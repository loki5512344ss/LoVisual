package dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.view;

/**
 * Белая пелена по образцу evo: штормовой туман сжимает дальность видимости и подмешивает цвет снега.
 * Сила пелены = сила шторма * экспозиция (под крышей пелены нет). Чистая математика.
 */
public final class BlizzardFog {

    public static final float MIN_STRENGTH = 0.01f;
    public static final float START_SHARE = 0.05f;
    public static final float SKY_SHARE = 0.6f;
    public static final float BRIGHTNESS_GAIN = 1.6f;
    public static final float BRIGHTNESS_FLOOR = 0.15f;
    public static final float COLOR_BLEND = 0.9f;

    private BlizzardFog() {
    }

    public static boolean active(float strength) {
        return strength > MIN_STRENGTH;
    }

    /** Дальность конца тумана: видимость в штиль, делённая на силу; слабая пелена почти не ограничивает обзор. */
    public static float endDistance(float visibility, float strength) {
        return visibility / strength;
    }

    /** Дальность начала тумана. */
    public static float startDistance(float visibility, float strength) {
        return endDistance(visibility, strength) * START_SHARE;
    }

    /** Туман только сжимает дальность, но не отодвигает ванильный. */
    public static float limit(float vanilla, float fog) {
        return Math.min(vanilla, fog);
    }

    /** Яркость цвета снега: светлее небо, светлее пелена; всегда в 0..1. */
    public static float brightness(float r, float g, float b) {
        float v = Math.max(r, Math.max(g, b)) * BRIGHTNESS_GAIN + BRIGHTNESS_FLOOR;
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    /** Канал цвета тумана: от ванильного к оттенку снега по силе пелены. */
    public static float blend(float vanilla, float tintChannel, float brightness, float strength) {
        float t = strength * COLOR_BLEND;
        return vanilla + (tintChannel * brightness - vanilla) * t;
    }
}
