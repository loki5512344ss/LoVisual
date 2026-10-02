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
}
