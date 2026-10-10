package dev.loki.lovisual.features.module.modules.misc.optimize.entity;

import org.lwjgl.system.MemoryStack;

/**
 * Hot-path helper: skip the {@link MemoryStack#stackPush()} ThreadLocal lookup
 * on the thread that called last (the render thread). The lookup is cheap on
 * its own, but Sodium's entity renderer calls it once per cuboid per frame
 * (thousands of times), and a single volatile read + identity compare is
 * strictly cheaper than the ThreadLocal hash lookup.
 *
 * <p>Stores the {@code (thread, stack)} pair in one record so another thread
 * never sees a half-updated pair. The first call from a new thread rewrites
 * the volatile; subsequent calls from the same thread take the fast path.
 *
 * <p>Used by the (Stage 3) Sodium {@code EntityRenderer.renderCuboid} mixin.
 */
public enum CachedStack {
    ;

    private record ThreadStack(Thread thread, MemoryStack stack) {
    }

    // One record so another thread never sees a thread/stack pair half-updated.
    private static volatile ThreadStack cached = new ThreadStack(null, null);

    /**
     * Push a frame on the render thread's cached stack, falling back to the
     * normal ThreadLocal lookup on a thread switch.
     */
    public static MemoryStack push() {
        ThreadStack current = cached;
        Thread thread = Thread.currentThread();
        if (current.thread() != thread) {
            current = new ThreadStack(thread, MemoryStack.stackGet());
            cached = current;
        }
        return current.stack().push();
    }

    /** Test-only: peek the cached thread (null before first call). */
    static Thread cachedThread() {
        return cached.thread();
    }

    /** Test-only: peek the cached stack (null before first call). */
    static MemoryStack cachedStack() {
        return cached.stack();
    }
}
