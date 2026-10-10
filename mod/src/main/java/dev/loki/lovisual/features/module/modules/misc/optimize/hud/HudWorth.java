package dev.loki.lovisual.features.module.modules.misc.optimize.hud;

/**
 * Hysteresis controller for the HUD cache. The HUD cache only saves work when
 * several frames share one HUD refresh; near one frame per refresh (refresh
 * interval ~= frame time) it redraws the HUD anyway and just adds a
 * full-screen clear + blend, which costs real FPS on integrated GPUs and
 * 60 Hz monitors. This controller tracks an EMA of frame time
 * ({@code *0.95 + dt*0.05}) and flips the "worth it" state with hysteresis:
 * turns OFF below {@code OFF_BELOW} frames-per-refresh, turns ON above
 * {@code ON_ABOVE}.
 *
 * <p>Pure logic, no Minecraft dependencies, fully testable. The (Stage 3) HUD cache mixin reads
 * {@link #worth()} to decide whether to engage.
 */
public enum HudWorth {
    ;

    private static final double OFF_BELOW = 1.5;
    private static final double ON_ABOVE = 2.0;

    private static double frameNs;
    private static long last;
    private static volatile boolean worth = true;

    /** True when the HUD cache is currently engaged. */
    public static boolean worth() {
        return worth;
    }

    /** True when the HUD cache should be available at all (independent of FPS). */
    public static boolean hysteresisEnabled() {
        return true;
    }

    /**
     * Update the EMA frame time and the hysteresis state. Called once per
     * frame; returns the new {@link #worth()} value.
     *
     * @param now       the current {@code System.nanoTime()} reading
     * @param refreshNs nanoseconds between HUD refreshes (1e9 / monitor Hz)
     */
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

    /** Test-only: reset EMA + hysteresis to defaults. */
    static void reset() {
        frameNs = 0;
        last = 0;
        worth = true;
    }

    /** Test-only: read the EMA frame time in nanoseconds. */
    static double frameNs() {
        return frameNs;
    }
}
