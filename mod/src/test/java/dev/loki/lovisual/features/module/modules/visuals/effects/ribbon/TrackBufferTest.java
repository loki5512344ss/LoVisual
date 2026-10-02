package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация следа: что должен делать буфер точек, независимо от реализации. */
class TrackBufferTest {

    @Test
    void firstSampleIsAlwaysKept() {
        TrackBuffer b = new TrackBuffer();
        assertTrue(b.add(1, 2, 3, 100L, 500L));
        assertEquals(1, b.size());
    }

    @Test
    void jitterBelowMinStepIsIgnoredButMovementIsKept() {
        TrackBuffer b = new TrackBuffer();
        b.add(0, 0, 0, 0L, 1000L);
        assertFalse(b.add(TrackBuffer.MIN_STEP * 0.5, 0, 0, 16L, 1000L));
        assertTrue(b.add(TrackBuffer.MIN_STEP * 2, 0, 0, 32L, 1000L));
        assertEquals(2, b.size());
    }

    @Test
    void pointsOlderThanLifetimeAreDropped() {
        TrackBuffer b = new TrackBuffer();
        b.add(0, 0, 0, 0L, 500L);
        b.add(1, 0, 0, 300L, 500L);
        b.add(2, 0, 0, 700L, 500L);
        assertEquals(2, b.size());
        assertEquals(1.0, b.points().get(0).x(), 0.0);
    }

    @Test
    void trimCanEmptyTheBuffer() {
        TrackBuffer b = new TrackBuffer();
        b.add(0, 0, 0, 0L, 500L);
        b.trim(10_000L, 500L);
        assertTrue(b.isEmpty());
    }

    @Test
    void lastSeenTracksEveryCallEvenWhenNoPointWasAdded() {
        TrackBuffer b = new TrackBuffer();
        b.add(0, 0, 0, 0L, 1000L);
        b.add(0, 0, 0, 250L, 1000L);
        assertEquals(250L, b.lastSeen());
        assertEquals(1, b.size());
    }

    @Test
    void pointsAreOrderedTailToHead() {
        TrackBuffer b = new TrackBuffer();
        for (int i = 0; i < 5; i++) b.add(i, 0, 0, i * 10L, 1000L);
        long prev = -1;
        for (RibbonPoint p : b.points()) {
            assertTrue(p.ms() > prev);
            prev = p.ms();
        }
    }

    // ---- непрерывность следа (шлейф крыла): телепорт и пауза рвут ленту, шум не копится

    @Test
    void aBigJumpStartsANewRibbonInsteadOfDrawingALongStreak() {
        TrackBuffer b = new TrackBuffer();
        b.addContinuous(0, 0, 0, 0L, 5000L, 1f, 0.35, 8.0, 300L);
        b.addContinuous(1, 0, 0, 50L, 5000L, 1f, 0.35, 8.0, 300L);
        b.addContinuous(500, 0, 0, 100L, 5000L, 1f, 0.35, 8.0, 300L);
        assertEquals(1, b.size());
        assertEquals(500.0, b.points().get(0).x(), 0.0);
    }

    @Test
    void aLongPauseStartsANewRibbon() {
        TrackBuffer b = new TrackBuffer();
        b.addContinuous(0, 0, 0, 0L, 60_000L, 1f, 0.35, 8.0, 300L);
        b.addContinuous(1, 0, 0, 100L, 60_000L, 1f, 0.35, 8.0, 300L);
        b.addContinuous(2, 0, 0, 1000L, 60_000L, 1f, 0.35, 8.0, 300L);
        assertEquals(1, b.size());
    }

    @Test
    void stepsSmallerThanTheMinimumAreNotRecorded() {
        TrackBuffer b = new TrackBuffer();
        b.addContinuous(0, 0, 0, 0L, 5000L, 1f, 0.35, 8.0, 300L);
        assertFalse(b.addContinuous(0.2, 0, 0, 50L, 5000L, 1f, 0.35, 8.0, 300L));
        assertTrue(b.addContinuous(0.4, 0, 0, 100L, 5000L, 1f, 0.35, 8.0, 300L));
    }

    @Test
    void everyPointRemembersTheSpeedItWasTakenAt() {
        TrackBuffer b = new TrackBuffer();
        b.addContinuous(0, 0, 0, 0L, 5000L, 0.5f, 0.35, 8.0, 300L);
        b.addContinuous(1, 0, 0, 50L, 5000L, 1.5f, 0.35, 8.0, 300L);
        assertEquals(0.5f, b.points().get(0).speed(), 0f);
        assertEquals(1.5f, b.points().get(1).speed(), 0f);
    }

    @Test
    void theRibbonHoldsAtMostTwoHundredFiftySixPoints() {
        TrackBuffer b = new TrackBuffer();
        for (int i = 0; i < 400; i++) b.addContinuous(i * 0.5, 0, 0, i * 10L, 600_000L, 1f, 0.35, 8.0, 300L);
        assertEquals(256, b.size());
        assertEquals(399 * 0.5, b.points().get(255).x(), 1e-9);
    }
}
