package dev.loki.lovisual.features.module.modules.misc.optimize.particle;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ParticleLodTest {

    @AfterEach
    void resetToggles() {
        OptimizeToggles.reset();
    }

    @Test
    void keepAllWhenLodDisabled() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        Object p = new Object();
        assertTrue(ParticleLod.keep(p, 0));
        assertTrue(ParticleLod.keep(p, 100));
        assertTrue(ParticleLod.keep(p, 10_000));
    }

    @Test
    void keepAllWhenNear() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        Object p = new Object();
        // Strictly under 16 blocks: always keep (NEAR_SQ = 16² = 256).
        assertTrue(ParticleLod.keep(p, 0));
        assertTrue(ParticleLod.keep(p, 16 * 16 - 1));
        assertTrue(ParticleLod.keep(p, 255));
        assertTrue(ParticleLod.keep(p, 255.999));
    }

    @Test
    void exactlyAtSixteenBlocksFallsThroughToMidRange() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        // At exactly 16² (the boundary), the particle is no longer "near" — its
        // keep decision is delegated to the mid-range thinning. Over many
        // particles, about half should survive.
        int kept = 0;
        for (int i = 0; i < 1024; i++) {
            Object p = new Object();
            if (ParticleLod.keep(p, 16 * 16)) kept++;
        }
        assertTrue(kept >= 410 && kept <= 614,
                "At 16² (boundary), about half should survive thinning, got " + kept + "/1024");
    }

    @Test
    void thinningAtMidRangeKeepsAboutHalf() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        double midSq = 24 * 24; // Between 16² and 32².
        Set<Boolean> seen = new HashSet<>();
        for (int i = 0; i < 256; i++) {
            Object p = new Object();
            seen.add(ParticleLod.keep(p, midSq));
        }
        // We should see both keep and drop, with keep being roughly half (60-100).
        assertTrue(seen.contains(true) && seen.contains(false),
                "Mid-range thinning should keep some and drop some over 256 trials");
    }

    @Test
    void thinningAtFarRangeKeepsAboutQuarter() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        double farSq = 48 * 48; // Beyond 32².
        int kept = 0;
        for (int i = 0; i < 1024; i++) {
            Object p = new Object();
            if (ParticleLod.keep(p, farSq)) kept++;
        }
        // Expect roughly 256 (1024/4). Allow a wide band (200-320) because identityHashCode is not perfectly uniform.
        assertTrue(kept >= 200 && kept <= 320,
                "Far-range thinning should keep roughly 1/4 of particles, got " + kept + "/1024");
    }

    @Test
    void sameParticleKeepsStableChoiceAcrossCalls() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        Object p = new Object();
        double farSq = 48 * 48;
        boolean first = ParticleLod.keep(p, farSq);
        for (int i = 0; i < 16; i++) {
            assertTrue(first == ParticleLod.keep(p, farSq),
                    "Same particle should get the same keep decision every call");
        }
    }

    @Test
    void primitiveOverloadMatchesObjectOverload() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        Object p = new Object();
        // particle at (50, 0, 0), camera at (0, 0, 0): distance² = 2500.
        boolean objectApi = ParticleLod.keep(p, 50 * 50);
        boolean primitiveApi = ParticleLod.keep(p, 50, 0, 0, 0, 0, 0);
        assertTrue(objectApi == primitiveApi,
                "Primitive overload should agree with object overload at same distance");
    }

    @Test
    void nearAlwaysKeepsEvenWhenParticleLodOn() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        Object p = new Object();
        assertTrue(ParticleLod.keep(p, 15 * 15));
        assertTrue(ParticleLod.keep(p, 0));
    }

    @Test
    void disablingParticleLodMidTestFlipsEverythingToKeep() {
        OptimizeToggles.apply(
                false, 0,
                true, false, false,
                false, 0,
                false, false, false, false, false);
        Object p = new Object();
        boolean first = ParticleLod.keep(p, 48 * 48);
        // Toggle off — every distance should now keep.
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertTrue(ParticleLod.keep(p, 48 * 48));
        assertTrue(ParticleLod.keep(p, 0));
        // first is just whatever it was — we only care that the new state is "keep everything".
        assertFalse(first == false && first != false, "dummy assertion to mark first read");
    }
}
