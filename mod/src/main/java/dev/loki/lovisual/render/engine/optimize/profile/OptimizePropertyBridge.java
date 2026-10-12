package dev.loki.lovisual.render.engine.optimize.profile;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridge between {@code -Dlovisual.opt.*} system properties and the
 * engine-side {@link OptimizeToggles}/{@link FrameBudget} state holders.
 *
 * <p>System-property overrides are the benchmark escape hatch — they let you
 * flip a toggle or a budget cap without touching the in-game {@code Optimize}
 * module UI. Useful for A/B testing in Stage 5.
 *
 * <p>Supported properties (all optional):
 * <ul>
 *   <li>{@code -Dlovisual.opt.all=false} — disables every toggle regardless of module state.</li>
 *   <li>{@code -Dlovisual.opt.entity_lod=true|false} — overrides the entity_lod toggle.</li>
 *   <li>{@code -Dlovisual.opt.particle_lod=true|false}</li>
 *   <li>{@code -Dlovisual.opt.spawner_cull=true|false}</li>
 *   <li>{@code -Dlovisual.opt.spawner_tick_cache=true|false}</li>
 *   <li>{@code -Dlovisual.opt.block_entity_cache=true|false}</li>
 *   <li>{@code -Dlovisual.opt.prepared_text_cache=true|false}</li>
 *   <li>{@code -Dlovisual.opt.micro_opts=true|false}</li>
 *   <li>{@code -Dlovisual.opt.fbo_share=true|false}</li>
 *   <li>{@code -Dlovisual.opt.parallel_particles=auto|true|false} — tri-state; {@code auto} defers
 *       to the module value + the {@code cores >= 4} gate inside {@code Workers}.</li>
 *   <li>{@code -Dlovisual.opt.entity_lod_distance=35} — overrides the distance (4..256).</li>
 *   <li>{@code -Dlovisual.opt.spawner_distance=16} — overrides the distance (4..64).</li>
 *   <li>{@code -Dlovisual.opt.budget_mode=off|soft|aggressive}</li>
 *   <li>{@code -Dlovisual.opt.target_fps=120}</li>
 * </ul>
 *
 * <p>Properties are resolved once per frame via {@link #resolve(Snapshot)} and
 * the resulting {@link Snapshot} is applied to {@link OptimizeToggles} and
 * {@link FrameBudget}. {@code Optimize} module reads the snapshot in its
 * {@code onFrame(float)} lifecycle.
 *
 * <p>Stage 2 of {@code mod/docs/optimize-layer-design.md}.
 */
public enum OptimizePropertyBridge {
    ;

    /** Cached parsed values — {@link System#getProperty} is too slow for the hot path. */
    private static volatile Snapshot cached = new Snapshot();

    /** True when the user wants every override disabled (forced off via {@code all=false}). */
    public static boolean allOff() {
        return "false".equals(System.getProperty("lovisual.opt.all"));
    }

    /**
     * Read the current {@code -Dlovisual.opt.*} properties into a fresh snapshot.
     * The Optimize module calls this once per frame.
     */
    public static Snapshot resolve() {
        Snapshot s = new Snapshot();
        s.allOff = allOff();

        s.entityLod = boolOverride("entity_lod");
        s.particleLod = boolOverride("particle_lod");
        s.spawnerCull = boolOverride("spawner_cull");
        s.spawnerTickCache = boolOverride("spawner_tick_cache");
        s.blockEntityCache = boolOverride("block_entity_cache");
        s.preparedTextCache = boolOverride("prepared_text_cache");
        s.microOpts = boolOverride("micro_opts");
        s.fboShare = boolOverride("fbo_share");

        s.entityLodDistance = intOverride("entity_lod_distance", -1);
        s.spawnerDistance = intOverride("spawner_distance", -1);

        s.parallelParticles = triStateOverride("parallel_particles");

        s.budgetMode = budgetModeOverride();
        s.targetFps = intOverride("target_fps", -1);
        cached = s;
        return s;
    }

    /** Last resolved snapshot — render code can read this without re-parsing. */
    public static Snapshot lastResolved() {
        return cached;
    }

    /** Reset cache — used by tests. */
    static void resetCache() {
        cached = new Snapshot();
    }

    /** True if any system property is currently set, false otherwise. */
    public static boolean hasOverrides() {
        for (String key : OVERRIDE_KEYS) {
            if (System.getProperty("lovisual.opt." + key) != null) return true;
        }
        return System.getProperty("lovisual.opt.all") != null;
    }

    // ---- helpers ----

    private static final String[] OVERRIDE_KEYS = {
            "entity_lod", "particle_lod", "spawner_cull", "spawner_tick_cache",
            "block_entity_cache", "prepared_text_cache", "micro_opts", "fbo_share",
            "entity_lod_distance", "spawner_distance", "budget_mode", "target_fps",
            "parallel_particles"
    };

    /**
     * Returns {@code null} when the property isn't set (use the module-supplied
     * value); {@code Boolean.TRUE}/{@code FALSE} when it's set.
     */
    private static Boolean boolOverride(String key) {
        String p = System.getProperty("lovisual.opt." + key);
        return p == null ? null : Boolean.parseBoolean(p);
    }

    private static int intOverride(String key, int fallback) {
        String p = System.getProperty("lovisual.opt." + key);
        if (p == null) return fallback;
        try {
            return (int) Double.parseDouble(p);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Tri-state override: {@code null} when the property is unset or set to {@code auto}
     * (defer to module + cores gate), {@code Boolean.TRUE}/{@code FALSE} when the user forced it.
     */
    private static Boolean triStateOverride(String key) {
        String p = System.getProperty("lovisual.opt." + key);
        if (p == null) return null;
        String v = p.trim().toLowerCase();
        if (v.isEmpty() || v.equals("auto")) return null;
        return Boolean.parseBoolean(v);
    }

    private static FrameBudget.Mode budgetModeOverride() {
        String p = System.getProperty("lovisual.opt.budget_mode");
        if (p == null) return null;
        return switch (p.toLowerCase()) {
            case "off" -> FrameBudget.Mode.OFF;
            case "soft" -> FrameBudget.Mode.SOFT;
            case "aggressive" -> FrameBudget.Mode.AGGRESSIVE;
            default -> null;
        };
    }

    /**
     * Resolved snapshot. Each field is one of:
     * <ul>
     *   <li>{@code null} for booleans — use the module's value.</li>
     *   <li>{@code -1} for ints when unset — use the module's value.</li>
     *   <li>the explicit override when the property is set.</li>
     * </ul>
     */
    public static final class Snapshot {
        public boolean allOff;
        public Boolean entityLod;
        public Boolean particleLod;
        public Boolean spawnerCull;
        public Boolean spawnerTickCache;
        public Boolean blockEntityCache;
        public Boolean preparedTextCache;
        public Boolean microOpts;
        public Boolean fboShare;
        public int entityLodDistance = -1;
        public int spawnerDistance = -1;
        /** Tri-state override: null = AUTO (use module + cores gate), TRUE/FALSE = forced. */
        public Boolean parallelParticles;
        public FrameBudget.Mode budgetMode;  // null = use module value
        public int targetFps = -1;

        /** True when this snapshot should override the given module-side boolean. */
        public boolean overrides(Boolean override, boolean moduleValue) {
            if (allOff) return false;
            if (override != null) return override;
            return moduleValue;
        }

        public int overrides(int override, int moduleValue) {
            if (allOff) return moduleValue;
            return override > 0 ? override : moduleValue;
        }

        public FrameBudget.Mode overrides(FrameBudget.Mode override, FrameBudget.Mode moduleValue) {
            if (allOff) return FrameBudget.Mode.OFF;
            return override != null ? override : moduleValue;
        }

        public int resolveTargetFps(int moduleValue) {
            if (allOff) return moduleValue;
            return targetFps > 0 ? targetFps : moduleValue;
        }

        /** Test-only: pretty-print. */
        @Override
        public String toString() {
            Map<String, Object> m = new HashMap<>();
            m.put("allOff", allOff);
            m.put("entity_lod", entityLod);
            m.put("particle_lod", particleLod);
            m.put("spawner_cull", spawnerCull);
            m.put("spawner_tick_cache", spawnerTickCache);
            m.put("block_entity_cache", blockEntityCache);
            m.put("prepared_text_cache", preparedTextCache);
            m.put("micro_opts", microOpts);
            m.put("fbo_share", fboShare);
            m.put("entity_lod_distance", entityLodDistance);
            m.put("spawner_distance", spawnerDistance);
            m.put("parallel_particles", parallelParticles);
            m.put("budget_mode", budgetMode);
            m.put("target_fps", targetFps);
            return m.toString();
        }
    }
}
