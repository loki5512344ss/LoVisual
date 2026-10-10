package dev.loki.lovisual.render.engine.optimize.profile;

/**
 * Frame budget for the optimization layer — one place that decides how much
 * work each subsystem is allowed to do per frame, scaled by the user's chosen
 * {@link Mode} and a target FPS. Render code (LOD distance caps, particle
 * caps, our own effects — ProjectileTrails, ElytraTrails, DeathEffects,
 * Blizzard, GodRays) reads these getters every frame instead of hardcoding
 * their own limits.
 *
 * <p>This is NOT auto-degradation by FPS — the user explicitly chooses a mode
 * (default {@link Mode#OFF}) and a target FPS; we never silently lower
 * quality based on measured frame time. The owner's Phase 9 rule stands.
 *
 * <p>Stage 2 of {@code mod/docs/optimize-layer-design.md}.
 */
public enum FrameBudget {
    ;

    /** Aggressiveness level — user-facing setting in the {@code Optimize} module. */
    public enum Mode {
        /** No budget enforcement — every subsystem runs at full quality. */
        OFF,
        /** Tighten the most expensive caps by a small margin (~10%) below the baseline. */
        SOFT,
        /** Aggressive caps tuned for the target FPS — may visually reduce far detail. */
        AGGRESSIVE;

        public boolean active() {
            return this != OFF;
        }
    }

    /** Default target FPS when the user hasn't picked one. 60 is the safe baseline. */
    public static final int DEFAULT_TARGET_FPS = 60;

    /** Multiplier applied to baseline caps when SOFT mode is on (slightly tighter). */
    static final double SOFT_SCALE = 0.9;

    /** Multiplier applied to baseline caps when AGGRESSIVE mode is on. */
    static final double AGGRESSIVE_SCALE = 0.6;

    // Defaults match ZakoOpt's stage-1 caps so the behavior is familiar.
    private static final int BASE_ENTITY_LOD_DISTANCE = 35;
    private static final int BASE_PARTICLE_NEAR_BLOCKS = 16;   // NEAR_SQ = 16²
    private static final int BASE_PARTICLE_MID_BLOCKS  = 32;   // MID_SQ  = 32²
    private static final int BASE_SPAWNER_DISTANCE     = 16;
    private static final int BASE_TRAIL_MAX_POINTS      = 256;
    private static final int BASE_TRAIL_STEP_NS         = 350_000_000;

    private static volatile Mode mode = Mode.OFF;
    private static volatile int targetFps = DEFAULT_TARGET_FPS;

    public static Mode mode() {
        return mode;
    }

    public static int targetFps() {
        return targetFps;
    }

    /** The scale factor applied to baseline caps — 1.0 in OFF, 0.9 in SOFT, 0.6 in AGGRESSIVE. */
    public static double scale() {
        return switch (mode) {
            case OFF -> 1.0;
            case SOFT -> SOFT_SCALE;
            case AGGRESSIVE -> AGGRESSIVE_SCALE;
        };
    }

    /** Apply a new budget snapshot — called by {@code Optimize} on enable/disable/frame. */
    public static void apply(Mode mode, int targetFps) {
        FrameBudget.mode = mode;
        FrameBudget.targetFps = Math.max(15, Math.min(360, targetFps));
    }

    /** Reset to defaults — used by {@code Optimize.onDisable}. */
    public static void reset() {
        apply(Mode.OFF, DEFAULT_TARGET_FPS);
    }

    // --- Per-subsystem caps (read by render code every frame) ---

    /** Squared LOD distance threshold for entities — beyond this, layers/anim get skipped. */
    public static double entityLodDistanceSq() {
        double d = BASE_ENTITY_LOD_DISTANCE * scale();
        return d * d;
    }

    /** Squared distance under which particles are always kept (never thinned). */
    public static double particleNearSq() {
        double d = BASE_PARTICLE_NEAR_BLOCKS * scale();
        return d * d;
    }

    /** Squared distance at which mid-range particle thinning kicks in. */
    public static double particleMidSq() {
        double d = BASE_PARTICLE_MID_BLOCKS * scale();
        return d * d;
    }

    /** Distance beyond which a spawner's display entity is fully culled. */
    public static int spawnerDistance() {
        return Math.max(4, (int) Math.round(BASE_SPAWNER_DISTANCE * scale()));
    }

    /** Max points recorded per trail (ProjectileTrails, ElytraTrails). */
    public static int trailMaxPoints() {
        return Math.max(8, (int) Math.round(BASE_TRAIL_MAX_POINTS * scale()));
    }

    /** Min step between two recorded trail samples, in nanoseconds. */
    public static long trailStepNs() {
        // Step grows (less frequent sampling) as the budget tightens.
        long base = BASE_TRAIL_STEP_NS;
        return Math.round(base / scale());
    }

    /**
     * True when the budget actively limits this subsystem. Renderers can use
     * this to skip work entirely (e.g. parallel particles disabled below
     * a threshold when AGGRESSIVE).
     */
    public static boolean limits(SubSystem subsystem) {
        if (!mode.active()) return false;
        return switch (subsystem) {
            case ENTITIES, PARTICLES, HUD, EFFECTS, TEXT -> true;
        };
    }

    /** Subsystems tracked by the frame profiler — used by {@link #limits(SubSystem)}. */
    public enum SubSystem {
        ENTITIES,
        PARTICLES,
        HUD,
        EFFECTS,
        TEXT
    }
}
