package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация хранилища следов крыльев: по ленте на каждое крыло каждого игрока, старое исчезает. */
class WingTrailStoreTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void everyWingOfEveryPlayerHasItsOwnRibbon() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.LEFT, 0, 0, 0, 0L, 1000L);
        s.sample(A, WingTrailStore.RIGHT, 2, 0, 0, 0L, 1000L);
        s.sample(B, WingTrailStore.LEFT, 9, 0, 0, 0L, 1000L);
        assertEquals(3, s.snapshot().size());
        assertEquals(2, s.trackCount());
    }

    @Test
    void leftAndRightHistoriesDoNotMix() {
        WingTrailStore s = new WingTrailStore();
        for (int i = 0; i < 4; i++) s.sample(A, WingTrailStore.LEFT, i * 0.5, 0, 0, i * 20L, 1000L);
        s.sample(A, WingTrailStore.RIGHT, 5, 0, 0, 0L, 1000L);
        var sizes = s.snapshot().stream().map(t -> t.size()).sorted().toList();
        assertEquals(java.util.List.of(1, 4), sizes);
    }

    @Test
    void ribbonOfAPlayerWhoStoppedGlidingFadesThenDisappears() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.LEFT, 0, 0, 0, 0L, 500L);
        s.sample(A, WingTrailStore.RIGHT, 2, 0, 0, 0L, 500L);
        s.prune(400L, 500L);
        assertEquals(2, s.snapshot().size(), "still visible while fading");
        s.prune(900L, 500L);
        assertTrue(s.snapshot().isEmpty());
        assertEquals(0, s.trackCount());
    }

    @Test
    void oneWingCanOutliveTheOther() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.LEFT, 0, 0, 0, 0L, 500L);
        s.sample(A, WingTrailStore.RIGHT, 2, 0, 0, 400L, 500L);
        s.prune(700L, 500L);
        assertEquals(1, s.snapshot().size());
        assertEquals(1, s.trackCount());
    }

    @Test
    void clearForgetsEveryone() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.LEFT, 0, 0, 0, 0L, 1000L);
        s.clear();
        assertTrue(s.snapshot().isEmpty());
    }

    @Test
    void aTeleportOrAPauseStartsAFreshRibbonInsteadOfAStreakAcrossTheMap() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.LEFT, 0, 0, 0, 0L, 5000L);
        s.sample(A, WingTrailStore.LEFT, 1, 0, 0, 50L, 5000L);
        s.sample(A, WingTrailStore.LEFT, 300, 0, 0, 100L, 5000L);
        assertEquals(1, s.snapshot().get(0).size());
        s.sample(A, WingTrailStore.LEFT, 301, 0, 0, 150L, 5000L);
        s.sample(A, WingTrailStore.LEFT, 302, 0, 0, 2000L, 5000L);
        assertEquals(1, s.snapshot().get(0).size(), "a pause of more than 300 ms breaks the ribbon");
    }

    @Test
    void pointsRememberHowFastThePlayerFlew() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.RIGHT, 0, 0, 0, 0L, 5000L, 0.8f);
        s.sample(A, WingTrailStore.RIGHT, 1, 0, 0, 50L, 5000L, 1.6f);
        var pts = s.snapshot().get(0).points();
        assertEquals(0.8f, pts.get(0).speed(), 0f);
        assertEquals(1.6f, pts.get(1).speed(), 0f);
    }

    @Test
    void tinyWobbleBelowAThirdOfABlockIsNotRecorded() {
        WingTrailStore s = new WingTrailStore();
        s.sample(A, WingTrailStore.LEFT, 0, 0, 0, 0L, 5000L);
        s.sample(A, WingTrailStore.LEFT, 0.1, 0, 0, 50L, 5000L);
        s.sample(A, WingTrailStore.LEFT, 0.2, 0, 0, 100L, 5000L);
        assertEquals(1, s.snapshot().get(0).size());
    }
}
