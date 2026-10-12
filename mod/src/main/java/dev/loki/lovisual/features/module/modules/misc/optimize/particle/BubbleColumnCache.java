package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.world.level.material.FluidState;

/**
 * Per-tick cache of {@code ClientLevel.getFluidState(pos)} results for
 * {@code BubbleColumnUpParticle.tick}. Thousands of bubble particles share a
 * few columns; each one re-checks its block's fluid every tick, which is a
 * chunk-section lookup per call. This cache is keyed by {@code BlockPos.asLong()}
 * and cleared once per tick (game-time change), so it never serves stale data
 * across ticks.
 *
 * <p>Render-thread only (vanilla particle ticking is single-threaded). The
 * cache is static because {@code BubbleColumnUpParticle} instances are created
 * and destroyed constantly — per-particle state would be re-initialized too
 * often to be useful.
 *
 * The cache logic is in {@link TickCache} (a generic per-tick keyed cache),
 * so it's unit-testable without Minecraft types.
 */
public enum BubbleColumnCache {
    ;

    private static final TickCache<FluidState> CACHE = new TickCache<>();

    /** True when the cache is engaged this frame. */
    public static boolean enabled() {
        return OptimizeToggles.microOpts();
    }

    /** Look up a cached fluid state for the given block-pos key + tick. */
    public static FluidState get(long posLong, long tick) {
        return CACHE.get(posLong, tick);
    }

    /** Store a fluid state for the given block-pos key. */
    public static void put(long posLong, FluidState fluid) {
        CACHE.put(posLong, fluid);
    }
}
