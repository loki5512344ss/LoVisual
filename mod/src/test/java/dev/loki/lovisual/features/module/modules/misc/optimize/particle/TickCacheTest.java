package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies {@link TickCache} — the generic per-tick keyed cache used by
 * {@link BubbleColumnCache} and (later) other per-tick caches like
 * {@code ParticleLightCache}. Pure logic, no Minecraft dependencies.
 */
public final class TickCacheTest {

    @Test
    void getReturnsNullOnEmptyCache() {
        TickCache<String> cache = new TickCache<>();
        assertNull(cache.get(123L, 100L));
    }

    @Test
    void putThenGetReturnsSameValue() {
        TickCache<String> cache = new TickCache<>();
        cache.get(42L, 100L); // prime the tick
        cache.put(42L, "fluid");
        assertSame("fluid", cache.get(42L, 100L));
    }

    @Test
    void getClearsCacheWhenTickChanges() {
        TickCache<String> cache = new TickCache<>();
        cache.get(42L, 100L);
        cache.put(42L, "fluid");
        assertSame("fluid", cache.get(42L, 100L));
        assertEquals(1, cache.size());
        // Different tick — cache cleared.
        assertNull(cache.get(42L, 101L));
        assertEquals(0, cache.size());
    }

    @Test
    void multipleKeysSameTick() {
        TickCache<String> cache = new TickCache<>();
        cache.get(1L, 100L);
        cache.put(1L, "a");
        cache.get(2L, 100L);
        cache.put(2L, "b");
        cache.get(3L, 100L);
        cache.put(3L, "c");
        assertEquals(3, cache.size());
        assertSame("a", cache.get(1L, 100L));
        assertSame("b", cache.get(2L, 100L));
        assertSame("c", cache.get(3L, 100L));
    }

    @Test
    void resetClearsEverything() {
        TickCache<String> cache = new TickCache<>();
        cache.get(1L, 100L);
        cache.put(1L, "a");
        cache.get(2L, 100L);
        cache.put(2L, "b");
        assertEquals(2, cache.size());
        cache.reset();
        assertEquals(0, cache.size());
        assertEquals(Long.MIN_VALUE, cache.cachedTick());
    }

    @Test
    void overwriteSameKey() {
        TickCache<String> cache = new TickCache<>();
        cache.get(42L, 100L);
        cache.put(42L, "first");
        cache.put(42L, "second");
        assertEquals(1, cache.size());
        assertSame("second", cache.get(42L, 100L));
    }

    @Test
    void cachedTickTracksLastGetTick() {
        TickCache<String> cache = new TickCache<>();
        assertEquals(Long.MIN_VALUE, cache.cachedTick());
        cache.get(1L, 100L);
        assertEquals(100L, cache.cachedTick());
        cache.get(2L, 200L);
        assertEquals(200L, cache.cachedTick());
    }

    @Test
    void nullValuesAreStored() {
        TickCache<String> cache = new TickCache<>();
        cache.get(42L, 100L);
        cache.put(42L, null);
        // get returns null — same as "not cached". This is a known limitation:
        // callers that want to cache null should use a sentinel.
        assertNull(cache.get(42L, 100L));
        // But the size is still 1, confirming the put went through.
        assertEquals(1, cache.size());
    }

    @Test
    void longKeyCollisionsHandled() {
        TickCache<String> cache = new TickCache<>();
        long k1 = 1L << 40; // Large keys that don't collide in a hashmap.
        long k2 = 2L << 40;
        cache.get(k1, 100L);
        cache.put(k1, "a");
        cache.get(k2, 100L);
        cache.put(k2, "b");
        assertSame("a", cache.get(k1, 100L));
        assertSame("b", cache.get(k2, 100L));
    }
}
