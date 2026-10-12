package dev.loki.lovisual.features.module.modules.misc.optimize.entity;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EntityLodTest {

    @AfterEach
    void resetToggles() {
        OptimizeToggles.reset();
    }

    @Test
    void farReturnsFalseWhenLodDisabled() {
        OptimizeToggles.apply(
                false, 35,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertFalse(EntityLod.far(10_000_000));
        assertFalse(EntityLod.farPlayer(10_000_000));
    }

    @Test
    void farComparesSquaredDistance() {
        OptimizeToggles.apply(
                true, 35,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        // d=35 -> d²=1225: 1225 is NOT greater than 1225, so not far.
        assertFalse(EntityLod.far(35 * 35));
        // d² just above threshold: should be far.
        assertTrue(EntityLod.far(35 * 35 + 1));
        // very close: not far.
        assertFalse(EntityLod.far(0));
        assertFalse(EntityLod.far(1));
    }

    @Test
    void farThresholdSqMatchesDistance() {
        OptimizeToggles.apply(
                true, 16,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertEquals(16 * 16, EntityLod.farThresholdSq(), 0.0001);
        assertFalse(EntityLod.far(16 * 16));
        assertTrue(EntityLod.far(16 * 16 + 1));
    }

    @Test
    void farPlayerMatchesFarInStage1() {
        OptimizeToggles.apply(
                true, 35,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        // Stage 1 collapses player LOD into entity LOD — same toggle, same threshold.
        for (double dSq : new double[]{0, 100, 1224, 1226, 10_000}) {
            assertEquals(EntityLod.far(dSq), EntityLod.farPlayer(dSq),
                    "far and farPlayer disagree at dSq=" + dSq);
        }
    }

    @Test
    void farRespectsRuntimeToggleChanges() {
        OptimizeToggles.apply(
                true, 35,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertTrue(EntityLod.far(10_000));
        // Toggle off mid-test — same distance should now be "not far".
        OptimizeToggles.apply(
                false, 35,
                false, false, false,
                false, 0,
                false, false, false, false, false);
        assertFalse(EntityLod.far(10_000));
    }
}
