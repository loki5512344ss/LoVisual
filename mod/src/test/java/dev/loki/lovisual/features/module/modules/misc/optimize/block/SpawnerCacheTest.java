package dev.loki.lovisual.features.module.modules.misc.optimize.block;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link SpawnerCache} lookup logic. We can't construct a real
 * {@code BaseSpawner} in the test JVM, so the test uses a hand-rolled mock
 * that implements {@link TickCached} directly — the cache only relies on the
 * interface, never on the {@code BaseSpawner} class itself.
 */
public final class SpawnerCacheTest {

    /** Minimal TickCached implementation for testing. */
    private static final class FakeSpawner implements TickCached {
        Object cached;
        long cachedTick = Long.MIN_VALUE;

        @Override
        public Object lovisual$cached() {
            return cached;
        }

        @Override
        public long lovisual$cachedTick() {
            return cachedTick;
        }

        @Override
        public void lovisual$setCached(Object value, long tick) {
            this.cached = value;
            this.cachedTick = tick;
        }
    }

    @AfterEach
    void resetToggles() {
        OptimizeToggles.reset();
    }

    @Test
    void enabledReflectsOptimizeToggles() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertFalse(SpawnerCache.enabled());

        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                true, false, false, false, false);
        assertTrue(SpawnerCache.enabled());

        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertFalse(SpawnerCache.enabled());
    }

    @Test
    void getReturnsNullForEmptyCache() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                true, false, false, false, false);
        FakeSpawner spawner = new FakeSpawner();
        assertNull(SpawnerCache.get(spawner, 100L));
    }

    @Test
    void putThenGetReturnsCachedValueForSameTick() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                true, false, false, false, false);
        FakeSpawner spawner = new FakeSpawner();
        EntityRenderState state = new EntityRenderState();
        SpawnerCache.put(spawner, 42L, state);
        assertSame(state, SpawnerCache.get(spawner, 42L));
    }

    @Test
    void getReturnsNullForDifferentTick() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                true, false, false, false, false);
        FakeSpawner spawner = new FakeSpawner();
        EntityRenderState state = new EntityRenderState();
        SpawnerCache.put(spawner, 42L, state);
        assertNull(SpawnerCache.get(spawner, 43L));
        assertNull(SpawnerCache.get(spawner, 41L));
    }

    @Test
    void putOverwritesPreviousCache() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                true, false, false, false, false);
        FakeSpawner spawner = new FakeSpawner();
        EntityRenderState first = new EntityRenderState();
        EntityRenderState second = new EntityRenderState();
        SpawnerCache.put(spawner, 1L, first);
        SpawnerCache.put(spawner, 2L, second);
        assertSame(second, SpawnerCache.get(spawner, 2L));
        assertNull(SpawnerCache.get(spawner, 1L));
    }

    @Test
    void cachedValuePersistsAcrossMultipleGets() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                true, false, false, false, false);
        FakeSpawner spawner = new FakeSpawner();
        EntityRenderState state = new EntityRenderState();
        SpawnerCache.put(spawner, 100L, state);
        // Multiple reads should all return the same value (no eviction).
        for (int i = 0; i < 5; i++) {
            assertSame(state, SpawnerCache.get(spawner, 100L));
        }
        assertEquals(100L, spawner.lovisual$cachedTick());
    }
}
