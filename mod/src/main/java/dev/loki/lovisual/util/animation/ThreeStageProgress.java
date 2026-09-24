package dev.loki.lovisual.util.animation;

/**
 * Чистая математика трёхфазной анимации (появление/жизнь/исчезновение) —
 * идея из Soup HitBubbles (ThreeStageAnimation), без библиотеки именованных
 * интерполяций референса: appear — ease-out (быстрый старт), disappear —
 * ease-in (медленный старт затухания).
 */
public enum ThreeStageProgress {
    ;

    public enum Stage { APPEAR, EXIST, DISAPPEAR, FINISHED }

    public static Stage stage(double elapsedMs, double appearMs, double existMs, double disappearMs) {
        if (elapsedMs < appearMs) return Stage.APPEAR;
        if (elapsedMs < appearMs + existMs) return Stage.EXIST;
        if (elapsedMs < appearMs + existMs + disappearMs) return Stage.DISAPPEAR;
        return Stage.FINISHED;
    }

    public static boolean isFinished(double elapsedMs, double appearMs, double existMs, double disappearMs) {
        return stage(elapsedMs, appearMs, existMs, disappearMs) == Stage.FINISHED;
    }

    /** @return общая "видимость" 0..1: растёт на appear, держится 1 на exist, падает на disappear. */
    public static float visibility(double elapsedMs, double appearMs, double existMs, double disappearMs) {
        Stage stage = stage(elapsedMs, appearMs, existMs, disappearMs);
        return switch (stage) {
            case APPEAR -> easeOut(clamp01(safeDiv(elapsedMs, appearMs)));
            case EXIST -> 1.0f;
            case DISAPPEAR -> 1.0f - easeIn(clamp01(safeDiv(elapsedMs - appearMs - existMs, disappearMs)));
            case FINISHED -> 0.0f;
        };
    }

    private static float easeOut(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv;
    }

    private static float easeIn(float t) {
        return t * t;
    }

    private static double safeDiv(double value, double divisor) {
        return divisor <= 0.0 ? 1.0 : value / divisor;
    }

    private static float clamp01(double v) {
        if (Double.isNaN(v)) return 0.0f;
        if (v < 0.0) return 0.0f;
        if (v > 1.0) return 1.0f;
        return (float) v;
    }
}
