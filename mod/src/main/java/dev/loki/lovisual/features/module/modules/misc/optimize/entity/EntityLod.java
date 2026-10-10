package dev.loki.lovisual.features.module.modules.misc.optimize.entity;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;

/**
 * Entity LOD gate: is an entity far enough that the renderer should skip its
 * layers (armour, held items, eyes, cape, elytra, stuck arrows) and freeze its
 * animation? Compares squared distance to the configured threshold so the
 * caller doesn't have to {@code Math.sqrt}.
 *
 * <p>The companion {@code farPlayer} exists so a config can drop
 * players separately from entities (the {@code playerLod} toggle); in LoVisual
 * Stage 1 we collapse the two — the same toggle gates both. Stage 2 may split
 * them again if profiling shows players are the bigger cost.
 */
public enum EntityLod {
    ;

    /** Squared threshold at and above which an entity is "far". */
    public static double farThresholdSq() {
        double d = OptimizeToggles.entityLodDistance();
        return d * d;
    }

    /** True when LOD is on and the entity is beyond the configured distance. */
    public static boolean far(double distanceSq) {
        if (!OptimizeToggles.entityLod()) {
            return false;
        }
        return distanceSq > farThresholdSq();
    }

    /** Same as {@link #far} — players and entities share one toggle in Stage 1. */
    public static boolean farPlayer(double distanceSq) {
        return far(distanceSq);
    }
}
