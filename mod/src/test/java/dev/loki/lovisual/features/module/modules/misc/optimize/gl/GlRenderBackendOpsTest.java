package dev.loki.lovisual.features.module.modules.misc.optimize.gl;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link GlRenderBackendOps} — the GL backend implementation of
 * {@link RenderBackendOps}. Pure-logic pieces only (no GL device lookup).
 */
public final class GlRenderBackendOpsTest {

    @AfterEach
    void reset() {
        OptimizeToggles.reset();
        GlRenderBackendOps.resetForTesting();
    }

    @Test
    void currentReturnsGlInstance() {
        RenderBackendOps ops = RenderBackendOps.current();
        assertTrue(ops instanceof GlRenderBackendOps,
                "Stage 3-prep: current() returns the GL singleton");
    }

    @Test
    void lazyClearDisabledWhenMicroOptsOff() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        RenderBackendOps ops = RenderBackendOps.current();
        assertFalse(ops.lazyClearSupported());
        assertFalse(ops.skipBindFramebufferZero(0));
        assertFalse(ops.skipBindFramebufferZero(5));
    }

    @Test
    void lazyClearSkipsBindZeroWhenMicroOptsOn() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        RenderBackendOps ops = RenderBackendOps.current();
        assertTrue(ops.lazyClearSupported());
        assertTrue(ops.skipBindFramebufferZero(0));
        assertFalse(ops.skipBindFramebufferZero(5),
                "Non-zero framebuffer should not be skipped");
    }

    @Test
    void tboCacheDisabledWhenMicroOptsOff() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        RenderBackendOps ops = RenderBackendOps.current();
        assertFalse(ops.tboCacheSupported());
        // Even after marking attached, lookup should be a no-op (returns false because not supported).
        ops.trackBoundTboTexture(42);
        assertFalse(ops.tboAlreadyAttached(0x8229, 100));
    }

    @Test
    void tboCacheAttachedWhenSameFormatBuffer() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        RenderBackendOps ops = RenderBackendOps.current();
        ops.trackBoundTboTexture(42);
        assertFalse(ops.tboAlreadyAttached(0x8229, 100),
                "First lookup should be a miss");
        ops.markTboAttached(0x8229, 100);
        assertTrue(ops.tboAlreadyAttached(0x8229, 100),
                "Same (format, buffer) pair should be cached as attached");
    }

    @Test
    void tboCacheMissOnDifferentFormatOrBuffer() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        RenderBackendOps ops = RenderBackendOps.current();
        ops.trackBoundTboTexture(42);
        ops.markTboAttached(0x8229, 100);
        assertFalse(ops.tboAlreadyAttached(0x8230, 100),
                "Different format should be a miss");
        assertFalse(ops.tboAlreadyAttached(0x8229, 101),
                "Different buffer should be a miss");
    }

    @Test
    void tboCacheSeparatesPerBoundTexture() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        RenderBackendOps ops = RenderBackendOps.current();
        ops.trackBoundTboTexture(42);
        ops.markTboAttached(0x8229, 100);
        ops.trackBoundTboTexture(43);
        assertFalse(ops.tboAlreadyAttached(0x8229, 100),
                "Different bound texture should be a miss — cache is per-texture");
        ops.trackBoundTboTexture(42);
        assertTrue(ops.tboAlreadyAttached(0x8229, 100),
                "Switching back to the first texture should still hit the cache");
    }

    @Test
    void forgetTboCacheClearsEverything() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        RenderBackendOps ops = RenderBackendOps.current();
        ops.trackBoundTboTexture(42);
        ops.markTboAttached(0x8229, 100);
        ops.trackBoundTboTexture(43);
        ops.markTboAttached(0x8230, 200);
        ops.forgetTboCache();
        ops.trackBoundTboTexture(42);
        assertFalse(ops.tboAlreadyAttached(0x8229, 100));
        ops.trackBoundTboTexture(43);
        assertFalse(ops.tboAlreadyAttached(0x8230, 200));
    }

    @Test
    void renderTypesCacheLookup() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        RenderBackendOps ops = RenderBackendOps.current();
        RenderBackendOps.Identifier id = new RenderBackendOps.Identifier("minecraft", "textures/entity/skin.png");
        assertNull(ops.renderTypeFor(id), "Fresh cache should miss");
        Object type = new Object();
        ops.cacheRenderType(id, type);
        assertSame(type, ops.renderTypeFor(id),
                "Cache should return the same instance we stored");
    }

    @Test
    void renderTypesCacheDisabledWhenMicroOptsOff() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        RenderBackendOps ops = RenderBackendOps.current();
        assertFalse(ops.renderTypesCacheSupported());
        RenderBackendOps.Identifier id = new RenderBackendOps.Identifier("minecraft", "test");
        // cacheRenderType is a no-op when disabled.
        ops.cacheRenderType(id, new Object());
        assertNull(ops.renderTypeFor(id));
    }

    @Test
    void identifierEqualityAndHash() {
        RenderBackendOps.Identifier a = new RenderBackendOps.Identifier("mc", "path");
        RenderBackendOps.Identifier b = new RenderBackendOps.Identifier("mc", "path");
        RenderBackendOps.Identifier c = new RenderBackendOps.Identifier("mc", "other");
        RenderBackendOps.Identifier d = new RenderBackendOps.Identifier("other", "path");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertEquals("mc:path", a.toString());
    }

    // --- FBO share (Stage 3 step 2) ---

    @Test
    void fboShareDisabledWhenToggleOff() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        RenderBackendOps ops = RenderBackendOps.current();
        assertFalse(ops.fboShareSupported());
        // Toggle off short-circuits before SharedDepthFbos — safe without GL.
        assertFalse(ops.clearDepthInPlace(1, 1.0));
    }

    @Test
    void fboShareSupportedWhenToggleOn() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, true);
        RenderBackendOps ops = RenderBackendOps.current();
        assertTrue(ops.fboShareSupported());
        // No mapping for this depth id — clear() falls through to vanilla.
        assertFalse(ops.clearDepthInPlace(1, 1.0));
    }
}
