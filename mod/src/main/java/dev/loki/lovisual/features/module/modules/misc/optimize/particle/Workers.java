package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

/**
 * Fixed-size daemon pool for parallel particle extraction. The number of
 * helpers is {@code max(1, min(8, cores-2))} — leaving two cores for the
 * render thread and the GC. {@link #run(int, IntConsumer)} splits the work
 * into {@code blocks} units (caller decides the block size), dispatches them
 * across helpers AND the calling thread (the caller works too — it only ever
 * waits for blocks a helper has already claimed), and returns the first
 * failure or {@code null} on success.
 *
 * <p>Used by the (Stage 3) {@code QuadParticleGroup} mixin to parallelise
 * particle render-state extraction when the particle count exceeds 1024
 * (Stage 1 ports the pool; the wiring is a later stage).
 *
 * <p>Pure java.util.concurrent, no Minecraft dependencies, fully testable.
 */
public final class Workers {

    public static final int COUNT = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 2));

    /** Minimum logical CPU count required for AUTO mode to enable parallel particles. */
    public static final int PARALLEL_MIN_CORES = 4;

    /**
     * Effective parallel-particles flag resolved from module + property overrides.
     * Written once per frame by {@code Optimize.pushState()}, read by the future
     * particle-extraction mixin to decide whether to fan out across {@link #stream(IntConsumer)}.
     */
    private static volatile boolean parallelParticles;

    /**
     * Resolve the parallel-particles flag. {@code forced} is the tri-state from
     * {@code -Dlovisual.opt.parallel_particles} (TRUE/FALSE) or {@code null} for AUTO; {@code moduleEnabled}
     * is the {@code Optimize.parallel_particles} switch (default {@code true}); AUTO engages when the
     * machine exposes {@link #PARALLEL_MIN_CORES} or more logical CPUs — mirrors the reference's
     * {@code CORES >= 4} rule from {@code mod/docs/optimize-layer-design.md}, no benchmark gate.
     */
    public static void configureParallelParticles(Boolean forced, boolean moduleEnabled) {
        parallelParticles = resolveParallelParticles(forced, moduleEnabled, Runtime.getRuntime().availableProcessors());
    }

    /** Test-friendly variant that lets the caller stub the CPU count. */
    public static boolean resolveParallelParticles(Boolean forced, boolean moduleEnabled, int cores) {
        if (forced != null) {
            return forced;
        }
        if (!moduleEnabled) {
            return false;
        }
        return cores >= PARALLEL_MIN_CORES;
    }

    /** True when the particle-extraction mixin should split across the pool. */
    public static boolean shouldParallelParticles() {
        return parallelParticles;
    }

    /** Reset to a safe default — used by {@code Optimize.onDisable} and by tests. */
    public static void resetParallelParticles() {
        parallelParticles = false;
    }

    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(COUNT, r -> {
        Thread t = new Thread(r, "lovisual-opt-worker-" + THREAD_ID.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private Workers() {
    }

    public static int poolSize() {
        return COUNT;
    }

    /**
     * Run {@code task(0..blocks-1)} across the pool plus the calling thread.
     * Returns the first failure, or {@code null} if all blocks succeeded.
     */
    public static Throwable run(int blocks, IntConsumer task) {
        if (blocks <= 0) {
            return null;
        }
        Job job = new Job(blocks, task);
        for (int h = Math.min(COUNT, blocks - 1); h > 0; h--) {
            POOL.execute(job::run);
        }
        // The caller works too; it only ever waits for blocks a helper has already claimed.
        job.run();
        while (job.completed.get() < blocks) {
            Thread.onSpinWait();
        }
        return job.error.get();
    }

    /**
     * Open a task stream: helpers start right away and take items as the
     * caller publishes them, so work overlaps with preparing the rest.
     * Used by the parallel model builder (Stage 3).
     */
    public static Stream stream(IntConsumer task) {
        Stream stream = new Stream(task);
        for (int h = COUNT; h > 0; h--) {
            POOL.execute(stream::work);
        }
        return stream;
    }

    /** Test-only: shut the pool down so the JVM can exit cleanly. */
    public static void shutdown() {
        POOL.shutdownNow();
    }

    /** Items {@code 0..count-1} are fully set up and may be taken by helpers. */
    public static final class Stream {
        final IntConsumer task;
        final AtomicInteger next = new AtomicInteger();
        final AtomicInteger completed = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        volatile int ready;
        volatile int total = -1;

        Stream(IntConsumer task) {
            this.task = task;
        }

        public void publish(int count) {
            ready = count;
        }

        /** The caller helps with what is left and waits for the rest; first failure or null. */
        public Throwable finish(int count) {
            ready = count;
            total = count;
            work();
            while (completed.get() < count) {
                Thread.onSpinWait();
            }
            return error.get();
        }

        void work() {
            while (true) {
                int n = next.get();
                if (n < ready) {
                    if (next.compareAndSet(n, n + 1)) {
                        try {
                            if (error.get() == null) {
                                task.accept(n);
                            }
                        } catch (Throwable t) {
                            error.compareAndSet(null, t);
                        } finally {
                            completed.incrementAndGet();
                        }
                    }
                    continue;
                }
                int t = total;
                if (t >= 0 && n >= t) {
                    return;
                }
                Thread.onSpinWait();
            }
        }
    }

    private static final class Job {
        final int blocks;
        final IntConsumer task;
        final AtomicInteger next = new AtomicInteger();
        final AtomicInteger completed = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        Job(int blocks, IntConsumer task) {
            this.blocks = blocks;
            this.task = task;
        }

        void run() {
            int b;
            while ((b = next.getAndIncrement()) < blocks) {
                try {
                    if (error.get() == null) {
                        task.accept(b);
                    }
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    completed.incrementAndGet();
                }
            }
        }
    }
}
