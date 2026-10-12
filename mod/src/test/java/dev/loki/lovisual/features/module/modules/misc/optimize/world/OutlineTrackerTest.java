package dev.loki.lovisual.features.module.modules.misc.optimize.world;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link OutlineTracker} — per-frame state for the outline-skip
 * optimization. Pure logic, no Minecraft dependencies.
 */
public final class OutlineTrackerTest {

    @AfterEach
    void reset() {
        OptimizeToggles.reset();
        OutlineTracker.reset();
    }

    @Test
    void disabledWhenMicroOptsOff() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertFalse(OutlineTracker.enabled());
        // Even when glowing was set this frame, shouldSkipOutlineRender returns false
        // when the toggle is off — the vanilla path runs unchanged.
        OutlineTracker.markGlowing();
        assertFalse(OutlineTracker.shouldSkipOutlineRender());
    }

    @Test
    void skipWhenEnabledAndNoGlowing() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        OutlineTracker.beginFrame();
        assertTrue(OutlineTracker.shouldSkipOutlineRender(),
                "Without glowing entities, doEntityOutline should be skipped");
    }

    @Test
    void doNotSkipWhenGlowingThisFrame() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        OutlineTracker.beginFrame();
        OutlineTracker.markGlowing();
        assertFalse(OutlineTracker.shouldSkipOutlineRender(),
                "Glowing entity this frame — should not skip doEntityOutline");
        assertTrue(OutlineTracker.glowingThisFrame());
    }

    @Test
    void beginFrameClearsGlowingFlag() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true, false);
        OutlineTracker.markGlowing();
        assertTrue(OutlineTracker.glowingThisFrame());
        OutlineTracker.beginFrame();
        assertFalse(OutlineTracker.glowingThisFrame());
    }

    @Test
    void dirtyStartsTrue() {
        OptimizeToggles.reset();
        OutlineTracker.reset();
        assertTrue(OutlineTracker.dirtyRaw());
        assertTrue(OutlineTracker.needsClear());
    }

    @Test
    void markClearedClearsDirty() {
        OutlineTracker.markCleared();
        assertFalse(OutlineTracker.dirtyRaw());
    }

    @Test
    void resetRestoresDefaults() {
        OutlineTracker.markGlowing();
        OutlineTracker.markCleared();
        OutlineTracker.reset();
        assertFalse(OutlineTracker.glowingThisFrameRaw());
        assertTrue(OutlineTracker.dirtyRaw());
    }

    @Test
    void needsClearFollowsDirtyAndGlowing() {
        // dirty=true (default) → needsClear true
        OutlineTracker.markCleared();
        // dirty=false, glowing=false → needsClear false (we'd need a glow to trigger a clear)
        assertFalse(OutlineTracker.needsClear());
        OutlineTracker.markGlowing();
        // dirty=true again, glowing=true → needsClear true
        assertTrue(OutlineTracker.needsClear());
    }
}
