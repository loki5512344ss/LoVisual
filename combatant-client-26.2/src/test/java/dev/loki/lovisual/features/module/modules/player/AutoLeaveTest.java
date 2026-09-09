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

public final class AutoLeaveTest {

    @Test
    void zeroDelayLeavesImmediatelyOnTriggerEdge() {
        AutoLeave.LeavePolicy policy = new AutoLeave.LeavePolicy();
        assertTrue(policy.onTick(true, 1000L, 0L));
    }

    @Test
    void delayWaitsBeforeLeaving() {
        AutoLeave.LeavePolicy policy = new AutoLeave.LeavePolicy();
        assertFalse(policy.onTick(true, 1000L, 2000L));
        assertFalse(policy.onTick(true, 2500L, 2000L));
        assertTrue(policy.onTick(true, 3001L, 2000L));
    }

    @Test
    void quietTickRearmsTheGate() {
        AutoLeave.LeavePolicy policy = new AutoLeave.LeavePolicy();
        assertFalse(policy.onTick(true, 1000L, 1000L));
        assertFalse(policy.onTick(false, 1500L, 1000L));
        assertFalse(policy.onTick(true, 2000L, 1000L));
        assertTrue(policy.onTick(true, 3000L, 1000L));
    }

    @Test
    void warningIsConsumedOnce() {
        AutoLeave.LeavePolicy policy = new AutoLeave.LeavePolicy();
        assertTrue(policy.consumeWarning());
        assertFalse(policy.consumeWarning());
        policy.reset();
        assertTrue(policy.consumeWarning());
    }

    @Test
    void healthThresholdIsInclusive() {
        assertTrue(AutoLeave.LeavePolicy.isHealthLow(6.0f, 6.0f));
        assertTrue(AutoLeave.LeavePolicy.isHealthLow(5.5f, 6.0f));
        assertFalse(AutoLeave.LeavePolicy.isHealthLow(6.5f, 6.0f));
    }

    @Test
    void playerCountRequiresAtLeastOne() {
        assertTrue(AutoLeave.LeavePolicy.isPlayerCountReached(1, 1));
        assertTrue(AutoLeave.LeavePolicy.isPlayerCountReached(3, 2));
        assertFalse(AutoLeave.LeavePolicy.isPlayerCountReached(1, 2));
        assertFalse(AutoLeave.LeavePolicy.isPlayerCountReached(0, 1));
    }
}
