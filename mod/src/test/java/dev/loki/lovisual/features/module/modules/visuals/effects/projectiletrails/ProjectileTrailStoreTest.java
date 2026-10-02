package dev.loki.lovisual.features.module.modules.visuals.effects.projectiletrails;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Спецификация ProjectileTrails: какие снаряды получают след и когда след исчезает. */
class ProjectileTrailStoreTest {

    @Test
    void knownProjectilesMapToSettingKeys() {
        assertEquals("arrow", TrailStore.kindOfPath("arrow"));
        assertEquals("arrow", TrailStore.kindOfPath("spectral_arrow"));
        assertEquals("trident", TrailStore.kindOfPath("trident"));
        assertEquals("pearl", TrailStore.kindOfPath("ender_pearl"));
        assertEquals("snowball", TrailStore.kindOfPath("snowball"));
        assertEquals("egg", TrailStore.kindOfPath("egg"));
        assertEquals("potion", TrailStore.kindOfPath("splash_potion"));
        assertEquals("potion", TrailStore.kindOfPath("lingering_potion"));
        assertEquals("wind_charge", TrailStore.kindOfPath("wind_charge"));
        assertEquals("wind_charge", TrailStore.kindOfPath("breeze_wind_charge"));
        assertEquals("fireball", TrailStore.kindOfPath("fireball"));
        assertEquals("fireball", TrailStore.kindOfPath("small_fireball"));
        assertEquals("firework", TrailStore.kindOfPath("firework_rocket"));
    }

    @Test
    void unrelatedEntitiesGetNoTrail() {
        assertNull(TrailStore.kindOfPath("zombie"));
        assertNull(TrailStore.kindOfPath("item"));
        assertNull(TrailStore.kindOfPath("experience_bottle"));
    }

    @Test
    void movingProjectileBuildsATrailThatThenFadesAway() {
        TrailStore store = new TrailStore();
        for (int i = 0; i < 5; i++) store.sample(7, i * 0.5, 0, 0, i * 50L, 400L);
        assertEquals(5, store.tracks().get(7).size());
        store.prune(1000L, 400L, id -> true);
        assertTrue(store.tracks().isEmpty() || store.tracks().get(7).size() <= 5);
    }

    @Test
    void trailOfAnExistingProjectileIsKeptEvenIfItStandsStill() {
        TrailStore store = new TrailStore();
        store.sample(1, 0, 0, 0, 0L, 400L);
        store.prune(100L, 400L, id -> true);
        assertEquals(1, store.tracks().size());
    }

    @Test
    void trailOfAGoneProjectileSmoulderesThenIsRemoved() {
        TrailStore store = new TrailStore();
        store.sample(1, 0, 0, 0, 0L, 400L);
        store.sample(1, 1, 0, 0, 100L, 400L);
        store.prune(300L, 400L, id -> false);
        assertEquals(1, store.tracks().size(), "still fading");
        store.prune(900L, 400L, id -> false);
        assertTrue(store.tracks().isEmpty());
    }

    @Test
    void tracksOfDifferentProjectilesAreIndependent() {
        TrailStore store = new TrailStore();
        store.sample(1, 0, 0, 0, 0L, 400L);
        store.sample(2, 5, 5, 5, 0L, 400L);
        assertEquals(2, store.tracks().size());
        assertNotSame(store.tracks().get(1), store.tracks().get(2));
    }
}
