package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.api;

import java.util.function.DoubleUnaryOperator;

/** Функции плавности для Lua-скриптов (`Easings:easeOutBack(t)`), t в [0,1]. */
public final class Easings {

    private static final double C1 = 1.70158;
    private static final double C3 = C1 + 1.0;

    private Easings() {
    }

    public static double outQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    public static double inOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    public static double outCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    public static double inOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    public static double outBack(double t) {
        return 1 + C3 * Math.pow(t - 1, 3) + C1 * Math.pow(t - 1, 2);
    }

    public static double inOutBack(double t) {
        double c2 = C1 * 1.525;
        return t < 0.5
                ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
    }

    public static double outElastic(double t) {
        if (t <= 0 || t >= 1) return Math.max(0, Math.min(1, t));
        return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1;
    }

    public static double outBounce(double t) {
        double n = 7.5625, d = 2.75;
        if (t < 1 / d) return n * t * t;
        if (t < 2 / d) { t -= 1.5 / d; return n * t * t + 0.75; }
        if (t < 2.5 / d) { t -= 2.25 / d; return n * t * t + 0.9375; }
        t -= 2.625 / d;
        return n * t * t + 0.984375;
    }

    /** Имя функции в Lua и сама функция. */
    public static java.util.Map<String, DoubleUnaryOperator> all() {
        java.util.Map<String, DoubleUnaryOperator> m = new java.util.LinkedHashMap<>();
        m.put("easeOutQuad", Easings::outQuad);
        m.put("easeInOutCubic", Easings::inOutCubic);
        m.put("easeOutCubic", Easings::outCubic);
        m.put("easeInOutSine", Easings::inOutSine);
        m.put("easeOutBack", Easings::outBack);
        m.put("easeInOutBack", Easings::inOutBack);
        m.put("easeOutElastic", Easings::outElastic);
        m.put("easeOutBounce", Easings::outBounce);
        return m;
    }
}
