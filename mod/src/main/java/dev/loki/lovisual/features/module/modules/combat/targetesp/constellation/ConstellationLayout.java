package dev.loki.lovisual.features.module.modules.combat.targetesp.constellation;

/**
 * Pure geometry of the "constellation" TargetESP mode: stars on a tilted,
 * slightly wobbling orbit around the target centre. Offsets are relative to
 * the target's feet position.
 */
public final class ConstellationLayout {
    private static final double TAU = Math.PI * 2.0;

    private ConstellationLayout() {
    }

    /**
     * @param out    3 * stars slots, filled with x, y, z per star
     * @param orbit  orbit radius in target widths
     * @param time   seconds
     * @param speed  spin multiplier
     */
    public static void positions(double[] out, int stars, float width, float height,
                                 float orbit, double time, float speed) {
        double spin = time * speed * 1.5;
        for (int s = 0; s < stars; s++) {
            double frac = (double) s / stars;
            double base = frac * TAU;
            double incl = Math.PI * 0.35 * Math.sin(frac * Math.PI * 1.5 + 0.5);
            double ang = spin + base;
            double r = orbit * width * (0.85 + 0.15 * Math.sin(base * 2.3));
            out[s * 3] = Math.cos(ang) * r;
            out[s * 3 + 1] = height / 2.0
                    + Math.sin(ang) * Math.sin(incl) * r * 0.7
                    + Math.sin(time * 1.2 + frac * TAU) * 0.2;
            out[s * 3 + 2] = Math.sin(ang) * Math.cos(incl) * r;
        }
    }

    /** Star brightness pulse in [0.5, 1.0]. */
    public static float pulse(double time, int star, int stars) {
        double frac = (double) star / stars;
        return (float) (0.75 + 0.25 * Math.sin(time * 3.0 + frac * TAU * 1.7));
    }
}
