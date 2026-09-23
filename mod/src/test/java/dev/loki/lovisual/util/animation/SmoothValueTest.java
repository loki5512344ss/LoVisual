/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for the single-update-point animator. No Minecraft types involved.
 */
public final class SmoothValueTest {

    private static final float EPS = 1.0e-4f;

    @Test
    void reachesTargetWhenMovingUp() {
        SmoothValue value = new SmoothValue(1.5f);
        value.setTarget(true);

        for (int i = 0; i < 200; i++) {
            value.update(0.5f);
        }

        assertEquals(1.0f, value.value(), EPS);
        assertTrue(value.isSettled());
    }

    @Test
    void reachesTargetWhenMovingDown() {
        SmoothValue value = new SmoothValue(1.5f, 1.0f);
        value.setInstant(true);
        value.setTarget(false);

        for (int i = 0; i < 200; i++) {
            value.update(0.5f);
        }

        assertEquals(0.0f, value.value(), EPS);
        assertTrue(value.isSettled());
    }

    @Test
    void movesUpMonotonically() {
        SmoothValue value = new SmoothValue(0.8f);
        value.setTarget(true);

        float previous = value.value();
        for (int i = 0; i < 100; i++) {
            value.update(1.0f / 3.0f);
            assertTrue(value.value() >= previous, "value must never fall while rising");
            assertTrue(value.value() <= 1.0f, "value must stay within 0..1");
            previous = value.value();
        }
        assertEquals(1.0f, value.value(), EPS);
    }

    @Test
    void movesDownMonotonically() {
        SmoothValue value = new SmoothValue(0.8f);
        value.setInstant(true);
        value.setTarget(false);

        float previous = value.value();
        for (int i = 0; i < 100; i++) {
            value.update(1.0f / 3.0f);
            assertTrue(value.value() <= previous, "value must never rise while falling");
            assertTrue(value.value() >= 0.0f, "value must stay within 0..1");
            previous = value.value();
        }
        assertEquals(0.0f, value.value(), EPS);
    }

    @Test
    void setInstantTeleportsValueAndTarget() {
        SmoothValue value = new SmoothValue(1.0f);
        assertEquals(0.0f, value.value(), EPS);

        value.setInstant(true);
        assertEquals(1.0f, value.value(), EPS);
        assertEquals(1.0f, value.target(), EPS);
        assertTrue(value.isSettled());

        value.setInstant(false);
        assertEquals(0.0f, value.value(), EPS);
        assertEquals(0.0f, value.target(), EPS);
        assertTrue(value.isSettled());
    }

    @Test
    void setTargetDoesNotMoveValue() {
        SmoothValue value = new SmoothValue(2.0f);
        value.setTarget(true);
        assertEquals(0.0f, value.value(), EPS, "setTarget must not advance the value");
        assertEquals(1.0f, value.target(), EPS);
    }

    @Test
    void frameRateIndependent() {
        // One long step must land on the same point as many small steps of equal total time.
        float totalTicks = 2.0f;
        float speed = 1.1f;

        SmoothValue coarse = new SmoothValue(speed);
        coarse.setTarget(true);
        coarse.update(totalTicks);

        SmoothValue fine = new SmoothValue(speed);
        fine.setTarget(true);
        int steps = 240;
        for (int i = 0; i < steps; i++) {
            fine.update(totalTicks / steps);
        }

        assertEquals(coarse.value(), fine.value(), 5.0e-3f);
        assertTrue(coarse.value() > 0.5f && coarse.value() < 1.0f);
    }

    @Test
    void zeroAndBrokenDeltasAreIgnored() {
        SmoothValue value = new SmoothValue(1.0f);
        value.setTarget(true);

        value.update(0.0f);
        value.update(-1.0f);
        value.update(Float.NaN);
        assertEquals(0.0f, value.value(), EPS);
        assertFalse(value.isSettled());

        value.update(1.0f);
        assertTrue(value.value() > 0.0f);
    }

    @Test
    void targetFlipsMidFlightReverseDirection() {
        SmoothValue value = new SmoothValue(1.0f);
        value.setTarget(true);
        value.update(1.0f);
        assertTrue(value.value() > 0.0f);

        value.setTarget(false);
        float peak = value.value();
        value.update(1.0f);
        assertTrue(value.value() < peak, "falling must start immediately after the flip");
    }

    @Test
    void rejectsBrokenSpeed() {
        assertThrows(IllegalArgumentException.class, () -> new SmoothValue(0.0f));
        assertThrows(IllegalArgumentException.class, () -> new SmoothValue(-1.0f));
        assertThrows(IllegalArgumentException.class, () -> new SmoothValue(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SmoothValue(Float.POSITIVE_INFINITY));
    }
}
