package dev.loki.lovisual.features.module.modules.misc.optimize.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies {@link CachedStack} — the ThreadLocal-free MemoryStack push helper.
 *
 * <p>Tests run on the test JVM's main thread; the first call populates the
 * cache, subsequent calls from the same thread take the fast path.
 */
public final class CachedStackTest {

    @AfterEach
    void reset() {
        // The cache is a single volatile — for the next test we just push something
        // from this thread again so cachedThread() returns to the current thread.
        // (Cross-test isolation isn't perfect because the cache is global, but
        // the tests below assert observable invariants rather than internal state.)
        CachedStack.push().close();
    }

    @Test
    void pushReturnsValidStack() {
        try (MemoryStack stack = CachedStack.push()) {
            assertNotNull(stack);
            // A sanity check that the returned stack actually works.
            long ptr = stack.nmalloc(8);
            assertNotSame(0L, ptr);
        }
    }

    @Test
    void pushTwiceFromSameThreadReturnsSameStack() {
        MemoryStack first;
        try (MemoryStack s = CachedStack.push()) {
            first = s;
        }
        MemoryStack second;
        try (MemoryStack s = CachedStack.push()) {
            second = s;
        }
        assertSame(first, second,
                "Same-thread push should return the same underlying MemoryStack instance");
        assertEquals(Thread.currentThread(), CachedStack.cachedThread());
    }

    @Test
    void pushFromDifferentThreadUpdatesCachedThread() throws InterruptedException {
        // First call from this thread to populate the cache.
        try (MemoryStack s = CachedStack.push()) {
            assertNotNull(s);
        }
        assertEquals(Thread.currentThread(), CachedStack.cachedThread());

        // Now push from a different thread — the cache should switch.
        Thread other = new Thread(() -> {
            try (MemoryStack s = CachedStack.push()) {
                assertNotNull(s);
            }
        });
        other.start();
        other.join();
        // After the other thread pushed, the cached thread is the other thread.
        assertEquals(other, CachedStack.cachedThread());
        assertNotNull(CachedStack.cachedStack());

        // Restore the cache to this thread for the next test.
        try (MemoryStack s = CachedStack.push()) {
            assertNotNull(s);
        }
        assertEquals(Thread.currentThread(), CachedStack.cachedThread());
    }

    @Test
    void nestedPushesWorkLikeVanillaStack() {
        // Vanilla MemoryStack supports nested stackPush/stackClose — CachedStack should too.
        try (MemoryStack outer = CachedStack.push()) {
            long outerPtr = outer.nmalloc(8);
            try (MemoryStack inner = CachedStack.push()) {
                long innerPtr = inner.nmalloc(8);
                assertNotSame(outerPtr, innerPtr);
            }
        }
    }
}
