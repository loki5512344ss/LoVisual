/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeathCoordsTest {

    @Test
    void detectsAliveToDeadTransition() {
        DeathCoords.DeathDetector detector = new DeathCoords.DeathDetector();
        assertFalse(detector.onTick(false));
        assertTrue(detector.onTick(true));
        assertFalse(detector.onTick(true));
        assertFalse(detector.onTick(true));
    }

    @Test
    void secondDeathIsDetectedAgainAfterRevive() {
        DeathCoords.DeathDetector detector = new DeathCoords.DeathDetector();
        detector.onTick(true);
        assertFalse(detector.onTick(false));
        assertTrue(detector.onTick(true));
    }

    @Test
    void resetClearsDeathState() {
        DeathCoords.DeathDetector detector = new DeathCoords.DeathDetector();
        detector.onTick(true);
        detector.reset();
        assertTrue(detector.onTick(true));
    }

    @Test
    void messageIncludesDimensionWhenEnabled() {
        String message = DeathCoords.formatDeathMessage(12.25, 64.0, -789.5, "minecraft:overworld", true);
        assertEquals("You died at 12.25, 64.00, -789.50 (minecraft:overworld)", message);
    }

    @Test
    void messageOmitsDimensionWhenDisabled() {
        String message = DeathCoords.formatDeathMessage(0.5, 1.5, 2.5, "minecraft:the_nether", false);
        assertEquals("You died at 0.50, 1.50, 2.50", message);
    }

    @Test
    void messageFallsBackWithoutDimensionName() {
        assertEquals(
                "You died at 1.00, 2.00, 3.00",
                DeathCoords.formatDeathMessage(1, 2, 3, "", true)
        );
        assertEquals(
                "You died at 1.00, 2.00, 3.00",
                DeathCoords.formatDeathMessage(1, 2, 3, null, true)
        );
    }
}
