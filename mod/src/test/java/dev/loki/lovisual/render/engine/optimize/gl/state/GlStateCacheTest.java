package dev.loki.lovisual.render.engine.optimize.gl.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link GlStateCache} — pure logic, no GL driver. Covers: UNKNOWN
 * starts the state, mark-then-already-bound round trip, independent slots for
 * program and VAO, reset wipes both, and the id-recycle case (mark A, mark B,
 * B is now current).
 */
public final class GlStateCacheTest {

    @BeforeEach
    void resetState() {
        GlStateCache.reset();
    }

    @Test
    void startsUnknown() {
        assertEquals(GlStateCache.UNKNOWN, GlStateCache.program());
        assertEquals(GlStateCache.UNKNOWN, GlStateCache.vao());
        // UNKNOWN must never short-circuit a real id (in case a driver ever returns -1).
        assertFalse(GlStateCache.programAlreadyBound(0));
        assertFalse(GlStateCache.vaoAlreadyBound(0));
    }

    @Test
    void programRoundTrip() {
        GlStateCache.markProgram(42);
        assertTrue(GlStateCache.programAlreadyBound(42));
        assertFalse(GlStateCache.programAlreadyBound(43));
        assertEquals(42, GlStateCache.program());
    }

    @Test
    void vaoRoundTrip() {
        GlStateCache.markVao(7);
        assertTrue(GlStateCache.vaoAlreadyBound(7));
        assertFalse(GlStateCache.vaoAlreadyBound(8));
        assertEquals(7, GlStateCache.vao());
    }

    @Test
    void programAndVaoAreIndependent() {
        GlStateCache.markProgram(11);
        assertEquals(11, GlStateCache.program());
        assertEquals(GlStateCache.UNKNOWN, GlStateCache.vao());

        GlStateCache.markVao(22);
        assertEquals(11, GlStateCache.program());
        assertEquals(22, GlStateCache.vao());
    }

    @Test
    void idReuseUpdatesCache() {
        GlStateCache.markProgram(5);
        GlStateCache.markProgram(9);
        assertTrue(GlStateCache.programAlreadyBound(9));
        assertFalse(GlStateCache.programAlreadyBound(5));
    }

    @Test
    void zeroIsAValidId() {
        GlStateCache.markProgram(0);
        assertEquals(0, GlStateCache.program());
        assertTrue(GlStateCache.programAlreadyBound(0));
    }
}
