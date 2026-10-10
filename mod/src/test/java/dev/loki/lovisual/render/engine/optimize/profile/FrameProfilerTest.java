package dev.loki.lovisual.render.engine.optimize.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link FrameProfiler} — the per-section frame profiler.
 *
 * <p>Stage 2 of {@code mod/docs/optimize-layer-design.md}. The profiler is
 * render-thread only; tests run on the JUnit main thread so the same thread
 * owns both {@code beginFrame}/{@code endFrame} and the section lifecycle.
 */
public final class FrameProfilerTest {

    @AfterEach
    void reset() {
        FrameProfiler.reset();
    }

    @Test
    void snapshotStartsAtZero() {
        FrameProfiler.Snapshot s = FrameProfiler.snapshot();
        assertEquals(0, s.entityMs(), 1e-9);
        assertEquals(0, s.particleMs(), 1e-9);
        assertEquals(0, s.hudMs(), 1e-9);
        assertEquals(0, s.effectMs(), 1e-9);
        assertEquals(0, s.textMs(), 1e-9);
        assertEquals(0, s.frameMs(), 1e-9);
    }

    @Test
    void sectionTimeIsRecordedAfterClose() throws InterruptedException {
        FrameProfiler.beginFrame();
        try (FrameProfiler.Section s = FrameProfiler.section(FrameBudget.SubSystem.PARTICLES)) {
            Thread.sleep(5);
        }
        FrameProfiler.endFrame();

        FrameProfiler.Snapshot snap = FrameProfiler.snapshot();
        assertTrue(snap.particleMs() > 0,
                "Particle section should record >0 ms, got " + snap.particleMs());
        assertTrue(snap.particleMs() < 50,
                "5ms sleep should not produce huge EMA, got " + snap.particleMs());
    }

    @Test
    void frameMsReflectsBeginEndGap() throws InterruptedException {
        FrameProfiler.beginFrame();
        Thread.sleep(10);
        FrameProfiler.endFrame();
        double frameMs = FrameProfiler.snapshot().frameMs();
        assertTrue(frameMs >= 10,
                "Frame time should be >= 10ms, got " + frameMs);
        assertTrue(frameMs < 200,
                "Frame time should be reasonable (<200ms), got " + frameMs);
    }

    @Test
    void nestedSectionsAreClosed() throws InterruptedException {
        FrameProfiler.beginFrame();
        try (FrameProfiler.Section outer = FrameProfiler.section(FrameBudget.SubSystem.ENTITIES)) {
            Thread.sleep(2);
            // Opening a new section implicitly closes the previous one.
            try (FrameProfiler.Section inner = FrameProfiler.section(FrameBudget.SubSystem.HUD)) {
                Thread.sleep(2);
            }
        }
        FrameProfiler.endFrame();
        FrameProfiler.Snapshot s = FrameProfiler.snapshot();
        assertTrue(s.entityMs() > 0, "Entity section should be recorded");
        assertTrue(s.hudMs() > 0, "HUD section should be recorded");
    }

    @Test
    void idleSectionDecaysAfter2Seconds() throws InterruptedException {
        FrameProfiler.beginFrame();
        try (FrameProfiler.Section s = FrameProfiler.section(FrameBudget.SubSystem.TEXT)) {
            Thread.sleep(2);
        }
        FrameProfiler.endFrame();
        assertTrue(FrameProfiler.snapshot().textMs() > 0, "Text section should be recorded first");

        // Wait long enough for the decay threshold (2000 ms) to trigger.
        Thread.sleep(2200);
        FrameProfiler.beginFrame();
        FrameProfiler.endFrame();
        assertEquals(0, FrameProfiler.snapshot().textMs(), 1e-9,
                "Text section should decay to 0 after 2s idle");
    }

    @Test
    void emaSmoothsSingleOutlier() throws InterruptedException {
        FrameProfiler.beginFrame();
        try (FrameProfiler.Section s = FrameProfiler.section(FrameBudget.SubSystem.HUD)) {
            Thread.sleep(1);
        }
        FrameProfiler.endFrame();
        // Repeat several short frames; the EMA should converge towards the short value.
        for (int i = 0; i < 10; i++) {
            FrameProfiler.beginFrame();
            try (FrameProfiler.Section s = FrameProfiler.section(FrameBudget.SubSystem.HUD)) {
                Thread.sleep(1);
            }
            FrameProfiler.endFrame();
        }
        double hudMs = FrameProfiler.snapshot().hudMs();
        assertTrue(hudMs > 0 && hudMs < 10,
                "EMA of HUD section should be small after 10 short frames, got " + hudMs);
    }

    @Test
    void resetClearsAllState() throws InterruptedException {
        FrameProfiler.beginFrame();
        try (FrameProfiler.Section s = FrameProfiler.section(FrameBudget.SubSystem.EFFECTS)) {
            Thread.sleep(3);
        }
        FrameProfiler.endFrame();
        FrameProfiler.reset();
        FrameProfiler.Snapshot snap = FrameProfiler.snapshot();
        assertEquals(0, snap.effectMs(), 1e-9);
        assertEquals(0, snap.frameMs(), 1e-9);
    }

    @Test
    void sectionCloseIsIdempotent() throws InterruptedException {
        FrameProfiler.beginFrame();
        FrameProfiler.Section s = FrameProfiler.section(FrameBudget.SubSystem.PARTICLES);
        Thread.sleep(2);
        s.close();
        s.close();  // Should not throw or double-count.
        FrameProfiler.endFrame();
        assertTrue(FrameProfiler.snapshot().particleMs() > 0);
    }
}
