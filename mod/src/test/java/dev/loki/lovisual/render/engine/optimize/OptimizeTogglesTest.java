package dev.loki.lovisual.render.engine.optimize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link OptimizeToggles} snapshot holder. Mirrors the existing
 * {@code OptimizeState} tests in shape: reset → assert default false → apply →
 * assert each knob tracks independently → reset → assert default again.
 */
public final class OptimizeTogglesTest {

    @Test
    void stateIsCleanUntilApplied() {
        OptimizeToggles.reset();
        assertFalse(OptimizeToggles.entityLod());
        assertFalse(OptimizeToggles.particleLod());
        assertFalse(OptimizeToggles.spawnerCull());
        assertFalse(OptimizeToggles.spawnerTickCache());
        assertFalse(OptimizeToggles.blockEntityCache());
        assertFalse(OptimizeToggles.preparedTextCache());
        assertFalse(OptimizeToggles.microOpts());
        assertEquals(0, OptimizeToggles.entityLodDistance());
        assertEquals(0, OptimizeToggles.spawnerDistance());
    }

    @Test
    void applyTracksEachKnobIndependently() {
        OptimizeToggles.apply(
                true,  35,
                false, true,  false,
                true,  16,
                true,
                false,
                true,
                true);
        assertTrue(OptimizeToggles.entityLod());
        assertEquals(35, OptimizeToggles.entityLodDistance());
        assertFalse(OptimizeToggles.particleLod());
        assertTrue(OptimizeToggles.particleLight());
        assertFalse(OptimizeToggles.particlePhysics());
        assertTrue(OptimizeToggles.spawnerCull());
        assertEquals(16, OptimizeToggles.spawnerDistance());
        assertTrue(OptimizeToggles.spawnerTickCache());
        assertFalse(OptimizeToggles.blockEntityCache());
        assertTrue(OptimizeToggles.preparedTextCache());
        assertTrue(OptimizeToggles.microOpts());

        OptimizeToggles.reset();
        assertFalse(OptimizeToggles.entityLod());
        assertEquals(0, OptimizeToggles.entityLodDistance());
        assertFalse(OptimizeToggles.spawnerCull());
        assertFalse(OptimizeToggles.microOpts());
    }

    @Test
    void applyWithAllOffMatchesReset() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false,
                false,
                false,
                false);
        assertFalse(OptimizeToggles.entityLod());
        assertFalse(OptimizeToggles.particleLod());
        assertFalse(OptimizeToggles.spawnerCull());
        assertFalse(OptimizeToggles.spawnerTickCache());
        assertFalse(OptimizeToggles.blockEntityCache());
        assertFalse(OptimizeToggles.preparedTextCache());
        assertFalse(OptimizeToggles.microOpts());
        assertEquals(0, OptimizeToggles.entityLodDistance());
        assertEquals(0, OptimizeToggles.spawnerDistance());
    }
}
