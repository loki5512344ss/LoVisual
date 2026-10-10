package dev.loki.lovisual.features.module.modules.misc.optimize.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LruMapTest {

    @Test
    void accessOrderIsRespected() {
        LruMap<String, Integer> map = new LruMap<>(3);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Touch "a" — it should now be the most-recently-used.
        Integer touched = map.get("a");
        assertEquals(1, touched);
        // Adding a fourth entry evicts the LRU, which is "b" (since "a" was just touched and "c" is newest).
        map.put("d", 4);
        assertNull(map.get("b"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.containsKey("d"));
    }

    @Test
    void capIsMaxSizePlusOne() {
        // removeEldestEntry fires when size > maxSize, so we hold up to maxSize entries after the next put.
        LruMap<Integer, String> map = new LruMap<>(2);
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        assertEquals(2, map.size());
        assertNull(map.get(1));
        assertEquals("two", map.get(2));
        assertEquals("three", map.get(3));
    }

    @Test
    void maxSizeAccessorMatchesConstructor() {
        LruMap<String, String> map = new LruMap<>(1024);
        assertEquals(1024, map.maxSize());
    }

    @Test
    void iterationOrderFollowsAccessOrder() {
        LruMap<String, Integer> map = new LruMap<>(5);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.get("a");
        List<String> keys = new ArrayList<>(map.keySet());
        // After touching "a", iteration order should be b, c, a.
        assertEquals(List.of("b", "c", "a"), keys);
    }

    @Test
    void putReplacingExistingKeyDoesNotEvict() {
        LruMap<String, Integer> map = new LruMap<>(2);
        map.put("a", 1);
        map.put("b", 2);
        map.put("a", 100);
        assertEquals(2, map.size());
        assertEquals(100, map.get("a"));
        // "b" should still be present — replacing a key's value doesn't grow size.
        assertSame(2, map.get("b"));
    }
}
