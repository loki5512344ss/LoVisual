package dev.loki.lovisual.render.engine.optimize.hud;

/**
 * Stage 3 step 6: the HUD cache only saves work when several frames share one
 * HUD refresh; near one frame per refresh it redraws the HUD anyway and just
 * adds a full-screen clear and blend, which costs real FPS on integrated GPUs
 * and 60 Hz monitors. A hysteresis band (off below 1.5, on above 2.0 frames
 * per refresh) keeps it from flipping every frame.
 */
public enum HudWorth {
    ;

    private static final double OFF_BELOW = 1.5;
    private static final double ON_ABOVE = 2.0;

    private static double frameNs;
    private static long last;
    private static volatile boolean worth = true;

    public static boolean update(long now, long refreshNs) {
        long dt = now - last;
        last = now;
        if (dt > 0 && dt < 1_000_000_000L) {
            frameNs = frameNs == 0 ? dt : frameNs * 0.95 + dt * 0.05;
        }
        double framesPerRefresh = frameNs == 0 ? ON_ABOVE : refreshNs / frameNs;
        if (worth ? framesPerRefresh < OFF_BELOW : framesPerRefresh > ON_ABOVE) {
            worth = !worth;
        }
        return worth;
    }

    public static boolean worth() {
        return worth;
    }

    public static void reset() {
        frameNs = 0;
        last = 0;
        worth = true;
    }
}
