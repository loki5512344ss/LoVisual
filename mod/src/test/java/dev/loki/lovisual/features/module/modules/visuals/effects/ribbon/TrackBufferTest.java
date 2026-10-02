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
}
