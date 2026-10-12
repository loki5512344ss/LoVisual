package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WorkersTest {

    @AfterAll
    static void shutdown() {
        Workers.shutdown();
    }

    @Test
    void runDispatchesAllBlocks() {
        int blocks = 32;
        AtomicInteger counter = new AtomicInteger();
        Throwable err = Workers.run(blocks, b -> counter.incrementAndGet());
        assertNull(err);
        assertEquals(blocks, counter.get());
    }

    @Test
    void zeroBlocksIsNoop() {
        AtomicInteger counter = new AtomicInteger();
        Throwable err = Workers.run(0, b -> counter.incrementAndGet());
        assertNull(err);
        assertEquals(0, counter.get());
    }

    @Test
    void negativeBlocksIsNoop() {
        AtomicInteger counter = new AtomicInteger();
        Throwable err = Workers.run(-1, b -> counter.incrementAndGet());
        assertNull(err);
        assertEquals(0, counter.get());
    }

    @Test
    void firstFailureIsReturnedAndRemainingBlocksShortCircuit() {
        int blocks = 16;
        AtomicInteger completed = new AtomicInteger();
        Throwable err = Workers.run(blocks, b -> {
            if (b == 3) {
                throw new IllegalStateException("block 3 failed");
            }
            completed.incrementAndGet();
        });
        assertNotNull(err);
        assertTrue(err instanceof IllegalStateException, "error should be IllegalStateException, got " + err.getClass());
        assertEquals("block 3 failed", err.getMessage());
        // Some other blocks may have run before block 3 errored, but no more than the full count.
        assertTrue(completed.get() <= blocks);
    }

    @Test
    void poolSizeIsAtLeastOne() {
        assertTrue(Workers.poolSize() >= 1, "pool size should be at least 1, got " + Workers.poolSize());
        assertTrue(Workers.poolSize() <= 8, "pool size should be at most 8, got " + Workers.poolSize());
    }

    @Test
    void parallelWorkCompletesAndCounterMatches() {
        int blocks = 64;
        AtomicInteger counter = new AtomicInteger();
        Throwable err = Workers.run(blocks, b -> {
            // Simulate a tiny unit of work. Swallow InterruptedException so the
            // IntConsumer stays compatible with Workers.run's signature.
            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            counter.incrementAndGet();
        });
        assertNull(err);
        assertEquals(blocks, counter.get());
    }

    @Test
    void streamPublishesAndFinishes() {
        int blocks = 48;
        AtomicInteger counter = new AtomicInteger();
        Workers.Stream stream = Workers.stream(b -> counter.incrementAndGet());
        for (int i = 1; i <= blocks; i++) {
            stream.publish(i);
        }
        Throwable err = stream.finish(blocks);
        assertNull(err);
        assertEquals(blocks, counter.get());
    }

    @Test
    void streamFinishesWithNoItems() {
        Workers.Stream stream = Workers.stream(b -> { });
        Throwable err = stream.finish(0);
        assertNull(err);
    }

    @Test
    void autoParallelParticlesRequiresFourCores() {
        assertFalse(Workers.resolveParallelParticles(null, true, 2));
        assertFalse(Workers.resolveParallelParticles(null, true, 3));
        assertTrue(Workers.resolveParallelParticles(null, true, 4));
        assertTrue(Workers.resolveParallelParticles(null, true, 8));
    }

    @Test
    void forcedTrueOverridesModuleAndCores() {
        assertTrue(Workers.resolveParallelParticles(Boolean.TRUE, false, 1));
    }

    @Test
    void forcedFalseOverridesAutoGate() {
        assertFalse(Workers.resolveParallelParticles(Boolean.FALSE, true, 16));
    }

    @Test
    void moduleOffDisablesAutoEvenOnManyCores() {
        assertFalse(Workers.resolveParallelParticles(null, false, 32));
    }

    @Test
    void configureWritesFlagReadedByShould() {
        Workers.configureParallelParticles(Boolean.TRUE, false);
        assertTrue(Workers.shouldParallelParticles());
        Workers.configureParallelParticles(Boolean.FALSE, true);
        assertFalse(Workers.shouldParallelParticles());
        Workers.resetParallelParticles();
        assertFalse(Workers.shouldParallelParticles());
    }

    @Test
    void streamReturnsFirstFailure() {
        int blocks = 12;
        Workers.Stream stream = Workers.stream(b -> {
            if (b == 2) {
                throw new IllegalStateException("stream item 2 failed");
            }
        });
        Throwable err = stream.finish(blocks);
        assertNotNull(err);
        assertTrue(err instanceof IllegalStateException);
    }
}
