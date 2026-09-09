/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AutoRespawnTest {

    @Test
    void deathAloneDoesNotTriggerImmediately() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, 500L));
    }

    @Test
    void triggersOnceAfterDelay() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, 500L));
        assertFalse(gate.shouldRequestRespawn(true, 1200L, 500L));
        assertTrue(gate.shouldRequestRespawn(true, 1500L, 500L));
    }

    @Test
    void requestIsSentOnlyOncePerDeath() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, 0L));
        gate.onRequestSent();

        // Player is still locally dead until the server responds.
        assertFalse(gate.shouldRequestRespawn(true, 1100L, 0L));
        assertFalse(gate.shouldRequestRespawn(true, 5000L, 0L));
    }

    @Test
    void comingBackToLifeRearmsTheGate() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, 0L));
        gate.onRequestSent();
        assertFalse(gate.shouldRequestRespawn(true, 2000L, 0L));

        assertFalse(gate.shouldRequestRespawn(false, 3000L, 0L));
        assertFalse(gate.shouldRequestRespawn(true, 3100L, 500L));
        assertTrue(gate.shouldRequestRespawn(true, 3600L, 500L));
    }

    @Test
    void totemReviveWithoutRequestStartsFreshDeath() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, 2000L));
        assertFalse(gate.shouldRequestRespawn(false, 1200L, 2000L));

        assertFalse(gate.shouldRequestRespawn(true, 5000L, 2000L));
        assertTrue(gate.shouldRequestRespawn(true, 7000L, 2000L));
    }

    @Test
    void negativeDelayTreatedAsZero() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, -5L));
        assertTrue(gate.shouldRequestRespawn(true, 1001L, -5L));
    }

    @Test
    void missingPlayerResetsState() {
        AutoRespawn.RespawnGate gate = new AutoRespawn.RespawnGate();
        assertFalse(gate.shouldRequestRespawn(true, 1000L, 0L));
        gate.onRequestSent();

        gate.reset();
        assertFalse(gate.shouldRequestRespawn(true, 2000L, 0L));
        assertTrue(gate.shouldRequestRespawn(true, 2001L, 0L));
    }
}
