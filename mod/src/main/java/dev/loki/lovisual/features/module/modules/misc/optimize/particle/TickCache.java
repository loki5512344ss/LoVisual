package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Generic per-tick cache keyed by {@code long}. The cache is cleared once per
 * tick change (game-time change), so it never serves stale data across ticks.
 *
 * <p>Used by {@link BubbleColumnCache} for {@code ClientLevel.getFluidState}
 * results — thousands of bubble particles share a few columns; each one
 * re-checks its block's fluid every tick. Generic on the value type so the
 * logic is unit-testable without Minecraft types.
 *
 * <p>Render-thread only. The cache is static because particle instances are
 * created and destroyed constantly — per-particle state would be re-initialized
 * too often to be useful.
 *
 */
public final class TickCache<V> {

    private final Long2ObjectOpenHashMap<V> map = new Long2ObjectOpenHashMap<>();
    private long cachedTick = Long.MIN_VALUE;

    /**
     * Look up a cached value for the given key, on the given tick. If the tick
     * differs from the cached tick, the cache is cleared first.
     *
     * @param key  the packed position key (e.g. {@code BlockPos.asLong()})
     * @param tick the current game tick
     * @return the cached value, or {@code null} if not cached
     */
    public V get(long key, long tick) {
        if (tick != cachedTick) {
            map.clear();
            cachedTick = tick;
        }
        return map.get(key);
    }

    /**
     * Store a value for the given key, on the current tick. The cache must
     * have been primed by a {@link #get} call first — that's where the tick
     * check happens; this method assumes the cache is fresh.
     */
    public void put(long key, V value) {
        map.put(key, value);
    }

    /** Number of cached entries on the current tick. */
    public int size() {
        return map.size();
    }

    /** The tick the cache currently holds data for. */
    public long cachedTick() {
        return cachedTick;
    }

    /** Reset the cache to empty (used by tests and on disable). */
    public void reset() {
        map.clear();
        cachedTick = Long.MIN_VALUE;
    }
}
