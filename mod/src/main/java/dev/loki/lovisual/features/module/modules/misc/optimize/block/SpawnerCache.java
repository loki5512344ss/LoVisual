package dev.loki.lovisual.features.module.modules.misc.optimize.block;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Utility layer over {@link TickCached} on a spawner ({@code BaseSpawner} via
 * mixin): get/put the cached {@link EntityRenderState} for the spawner's
 * display mob, keyed by tick, skipping the per-frame {@code extractEntity}
 * rebuild. The (Stage 3) spawner renderer mixin populates this on the first
 * frame of a tick and the subsequent frames in the same tick read back from
 * the cache.
 *
 * <p>Takes the {@link TickCached} interface directly rather than the
 * {@code BaseSpawner} class so the contract is testable without a
 * Minecraft runtime — the Stage 3 mixin casts {@code (TickCached) spawner}
 * before calling.
 *
 * <p>Reads {@link OptimizeToggles#spawnerTickCache()} to decide whether caching is enabled.
 */
public enum SpawnerCache {
    ;

    /** True when the cache is engaged this frame. */
    public static boolean enabled() {
        return OptimizeToggles.spawnerTickCache();
    }

    /** Cached state for this tick, or {@code null} if not yet cached. */
    public static EntityRenderState get(TickCached spawner, long tick) {
        return spawner.lovisual$cachedTick() == tick
                ? (EntityRenderState) spawner.lovisual$cached()
                : null;
    }

    /** Write the cached state for the given tick. */
    public static void put(TickCached spawner, long tick, EntityRenderState state) {
        spawner.lovisual$setCached(state, tick);
    }
}
