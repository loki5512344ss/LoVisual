package dev.loki.lovisual.features.gui.mainmenu.hero.logic;

/** Time-of-day helpers for the hero menu clock widget. */
public final class DayCycle {
    public static final int MORNING = 0;
    public static final int DAY = 1;
    public static final int EVENING = 2;
    public static final int NIGHT = 3;

    private DayCycle() {
    }

    public static int period(int hour) {
        if (hour >= 5 && hour < 12) return MORNING;
        if (hour >= 12 && hour < 17) return DAY;
        if (hour >= 17 && hour < 22) return EVENING;
        return NIGHT;
    }

    /** Fraction of the day elapsed, in [0, 1). */
    public static float progress(int hour, int minute) {
        return (hour * 60 + minute) / 1440.0f;
    }

    /** Centre x of the bar thumb; the thumb never leaves the rounded bar ends. */
    public static float thumbX(float barX, float barW, float barH, float progress) {
        float r = barH * 0.5f;
        float p = Math.max(0f, Math.min(1f, progress));
        return barX + r + p * (barW - r * 2f);
    }
}
