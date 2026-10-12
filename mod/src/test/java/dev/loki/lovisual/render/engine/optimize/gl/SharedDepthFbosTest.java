package dev.loki.lovisual.render.engine.optimize.gl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies {@link SharedDepthFbos} bookkeeping — the headless-safe paths
 * only. The DSA clear path needs a live GL context and is covered by an
 * in-game check (owner runs {@code runClient}); without a cached FBO or a
 * known depth id, {@code clear} returns {@code false} before touching GL.
 */
public final class SharedDepthFbosTest {

    @AfterEach
    void reset() {
        SharedDepthFbos.resetForTesting();
    }

    @Test
    void rememberTracksDepthToFboMapping() {
        SharedDepthFbos.remember(10, 100);
        SharedDepthFbos.remember(11, 101);
        assertEquals(2, SharedDepthFbos.sizeForTesting());
        assertEquals(100, SharedDepthFbos.fboForTesting(10));
        assertEquals(101, SharedDepthFbos.fboForTesting(11));
    }

    @Test
    void rememberSameDepthTwiceKeepsLastFbo() {
        SharedDepthFbos.remember(10, 100);
        SharedDepthFbos.remember(10, 102);
        assertEquals(1, SharedDepthFbos.sizeForTesting());
        assertEquals(102, SharedDepthFbos.fboForTesting(10));
    }

    @Test
    void forgetRemovesOnlyMappingsPointingAtThatFbo() {
        SharedDepthFbos.remember(10, 100);
        SharedDepthFbos.remember(11, 100);
        SharedDepthFbos.remember(12, 101);
        SharedDepthFbos.forget(100);
        assertEquals(1, SharedDepthFbos.sizeForTesting());
        assertEquals(0, SharedDepthFbos.fboForTesting(10));
        assertEquals(0, SharedDepthFbos.fboForTesting(11));
        assertEquals(101, SharedDepthFbos.fboForTesting(12));
    }

    @Test
    void forgetUnknownFboIsNoop() {
        SharedDepthFbos.remember(10, 100);
        SharedDepthFbos.forget(999);
        assertEquals(1, SharedDepthFbos.sizeForTesting());
        assertEquals(100, SharedDepthFbos.fboForTesting(10));
    }

    @Test
    void clearWithoutMappingFallsThroughToVanilla() {
        SharedDepthFbos.remember(10, 100);
        // Unknown depth id: no GL touched, caller keeps the vanilla path.
        assertFalse(SharedDepthFbos.clear(42, 1.0));
        // Empty map after forget: same.
        SharedDepthFbos.forget(100);
        assertFalse(SharedDepthFbos.clear(10, 1.0));
    }

    @Test
    void resetDropsEverything() {
        SharedDepthFbos.remember(10, 100);
        SharedDepthFbos.resetForTesting();
        assertEquals(0, SharedDepthFbos.sizeForTesting());
        assertFalse(SharedDepthFbos.clear(10, 1.0));
    }
}
