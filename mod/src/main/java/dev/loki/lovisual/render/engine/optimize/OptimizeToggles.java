package dev.loki.lovisual.render.engine.optimize;

/**
 * Resolved per-frame toggle snapshot for the Stage 1 optimization helpers
 * (LOD distances, caches, micro-opts). Mirrors {@link OptimizeState}'s shape:
 * an enum-utility holder with volatile fields, written by the {@code Optimize}
 * module on enable/disable and once per frame, read by render code (helpers,
 * future mixins) — never by the module class itself.
 *
 * <p>Stage 1 of {@code mod/docs/optimize-layer-design.md}: backend-independent
 * logic only. GL/Vulkan-specific toggles (frame fence, lazy clear, FBO share,
 * TBO cache, skin atlas, HUD cache) live in their own holders added in Stages
 * 3-4 alongside the mixins that consume them.
 *
 * <p>All module settings are resolved into volatile primitive fields once per frame.
 * System-property overrides are not supported here (the {@code Optimize} module's
 * {@code bool()}/{@code num()} settings are the single source of truth); they can be
 * layered back via {@code -Dlovisual.opt.*} in Stage 2 per the design doc.
 */
public enum OptimizeToggles {
    ;

    private static volatile boolean entityLod;
    private static volatile int entityLodDistance;
    private static volatile boolean particleLod;
    private static volatile boolean particleLight;
    private static volatile boolean particlePhysics;
    private static volatile boolean spawnerCull;
    private static volatile int spawnerDistance;
    private static volatile boolean spawnerTickCache;
    private static volatile boolean blockEntityCache;
    private static volatile boolean preparedTextCache;
    private static volatile boolean microOpts;

    public static boolean entityLod()          { return entityLod; }
    public static int     entityLodDistance()  { return entityLodDistance; }
    public static boolean particleLod()        { return particleLod; }
    public static boolean particleLight()       { return particleLight; }
    public static boolean particlePhysics()     { return particlePhysics; }
    public static boolean spawnerCull()         { return spawnerCull; }
    public static int     spawnerDistance()     { return spawnerDistance; }
    public static boolean spawnerTickCache()    { return spawnerTickCache; }
    public static boolean blockEntityCache()    { return blockEntityCache; }
    public static boolean preparedTextCache()   { return preparedTextCache; }
    public static boolean microOpts()           { return microOpts; }

    /**
     * Push a full snapshot. Called by {@code Optimize.onEnable/onDisable/onFrame}
     * with the resolved {@code (isEnabled() && knob.get())} for every toggle.
     */
    public static void apply(
            boolean entityLod, int entityLodDistance,
            boolean particleLod, boolean particleLight, boolean particlePhysics,
            boolean spawnerCull, int spawnerDistance,
            boolean spawnerTickCache,
            boolean blockEntityCache,
            boolean preparedTextCache,
            boolean microOpts) {
        OptimizeToggles.entityLod          = entityLod;
        OptimizeToggles.entityLodDistance  = entityLodDistance;
        OptimizeToggles.particleLod        = particleLod;
        OptimizeToggles.particleLight      = particleLight;
        OptimizeToggles.particlePhysics    = particlePhysics;
        OptimizeToggles.spawnerCull        = spawnerCull;
        OptimizeToggles.spawnerDistance    = spawnerDistance;
        OptimizeToggles.spawnerTickCache   = spawnerTickCache;
        OptimizeToggles.blockEntityCache   = blockEntityCache;
        OptimizeToggles.preparedTextCache  = preparedTextCache;
        OptimizeToggles.microOpts          = microOpts;
    }

    /** Reset to "nothing on" — used by {@code Optimize.onDisable}. */
    public static void reset() {
        apply(false, 0, false, false, false, false, 0, false, false, false, false);
    }
}
