/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation.hand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Математика отставания предмета от поворота камеры. */
final class SwayStateTest {

    private static final float DT = 0.5f;

    @Test
    void withoutInputStaysZero() {
        SwayState state = new SwayState();
        for (int i = 0; i < 60; i++) {
            state.update(0.0f, 0.0f, DT);
        }
        assertEquals(0.0f, state.yawLag(), 1.0e-6f);
        assertEquals(0.0f, state.pitchLag(), 1.0e-6f);
        assertTrue(state.isSettled());
    }

    @Test
    void yawImpulseProducesPositiveLag() {
        SwayState state = new SwayState();
        state.update(4.0f, 0.0f, DT);
        assertTrue(state.yawLag() > 0.0f, "движение мыши вправо должно давать положительное отставание");
        assertEquals(0.0f, state.pitchLag(), 1.0e-6f);
    }

    @Test
    void lagDecaysBackToZeroWhenMouseStill() {
        SwayState state = new SwayState();
        state.update(4.0f, -2.0f, DT);
        for (int i = 0; i < 400; i++) {
            state.update(0.0f, 0.0f, DT);
        }
        assertTrue(state.isSettled(), "без ввода отставание должно полностью затухнуть");
    }

    @Test
    void lagIsClamped() {
        SwayState state = new SwayState();
        for (int i = 0; i < 40; i++) {
            state.update(180.0f, -120.0f, DT);
        }
        assertTrue(state.yawLag() <= SwayState.MAX_YAW_LAG_DEG + 1.0e-4f);
        assertTrue(state.pitchLag() >= -SwayState.MAX_PITCH_LAG_DEG - 1.0e-4f);
    }

    @Test
    void mirroredInputsGiveMirroredLag() {
        SwayState left = new SwayState();
        SwayState right = new SwayState();
        for (int i = 0; i < 10; i++) {
            left.update(-3.0f, 1.5f, DT);
            right.update(3.0f, -1.5f, DT);
        }
        assertEquals(left.yawLag(), -right.yawLag(), 1.0e-5f);
        assertEquals(left.pitchLag(), -right.pitchLag(), 1.0e-5f);
    }

    @Test
    void oppositeInputPullsBackThroughZero() {
        SwayState state = new SwayState();
        for (int i = 0; i < 10; i++) {
            state.update(3.0f, 0.0f, DT);
        }
        float peak = state.yawLag();
        for (int i = 0; i < 10; i++) {
            state.update(-3.0f, 0.0f, DT);
        }
        assertTrue(state.yawLag() < peak, "обратное движение должно гасить отставание");
    }

    @Test
    void nonPositiveDeltaIgnored() {
        SwayState state = new SwayState();
        state.update(2.0f, 1.0f, DT);
        float yaw = state.yawLag();
        state.update(2.0f, 1.0f, 0.0f);
        state.update(2.0f, 1.0f, -1.0f);
        assertEquals(yaw, state.yawLag(), 1.0e-6f);
        assertFalse(state.isSettled());
    }

    @Test
    void brokenInputsAreSafe() {
        SwayState state = new SwayState();
        state.update(Float.NaN, Float.POSITIVE_INFINITY, DT);
        state.update(1.0f, 1.0f, Float.NaN);
        assertEquals(0.0f, state.yawLag(), 1.0e-6f);
        assertEquals(0.0f, state.pitchLag(), 1.0e-6f);
    }

    @Test
    void wrapDegreesKeepsResultInHalfCircle() {
        assertEquals(-20.0f, SwayState.wrapDegrees(340.0f), 1.0e-4f);
        assertEquals(20.0f, SwayState.wrapDegrees(-340.0f), 1.0e-4f);
        assertEquals(-180.0f, SwayState.wrapDegrees(180.0f), 1.0e-4f);
        assertEquals(0.0f, SwayState.wrapDegrees(360.0f), 1.0e-4f);
        assertEquals(5.0f, SwayState.wrapDegrees(365.0f), 1.0e-4f);
    }

    @Test
    void resetClearsEverything() {
        SwayState state = new SwayState();
        state.update(4.0f, 2.0f, DT);
        state.reset();
        assertTrue(state.isSettled());
    }
}
