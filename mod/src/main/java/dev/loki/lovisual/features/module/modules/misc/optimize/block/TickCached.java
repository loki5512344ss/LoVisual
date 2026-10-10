package dev.loki.lovisual.features.module.modules.misc.optimize.block;

/**
 * Mixin-contract: a generic per-tick cache slot on a target object
 * (block-entity, spawner, piston moving block). Stores one cached value plus
 * the tick it was written; the helper (e.g. {@link SpawnerCache}) checks
 * {@link #lovisual$cachedTick()} against the current tick and reads
 * {@link #lovisual$cached()} if it matches, otherwise lets the caller rebuild
 * and writes the fresh value via {@link #lovisual$setCached(Object, long)}.
 */
public interface TickCached {
    Object lovisual$cached();

    long lovisual$cachedTick();

    void lovisual$setCached(Object value, long tick);
}
