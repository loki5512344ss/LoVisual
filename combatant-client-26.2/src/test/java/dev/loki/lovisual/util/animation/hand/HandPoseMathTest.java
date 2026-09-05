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

/** Сборка позы руки из эффектов. */
final class HandPoseMathTest {

    private static final float EPS = 1.0e-6f;

    @Test
    void swayZeroStrengthIsZero() {
        assertEquals(HandPoseMath.ZERO, HandPoseMath.sway(6.0f, 4.0f, 0.0f));
    }

    @Test
    void swayStrengthScalesOutput() {
        HandPoseMath.Pose unit = HandPoseMath.sway(3.0f, 2.0f, 1.0f);
        HandPoseMath.Pose doubled = HandPoseMath.sway(3.0f, 2.0f, 2.0f);
        assertEquals(unit.dx() * 2.0f, doubled.dx(), EPS);
        assertEquals(unit.ry() * 2.0f, doubled.ry(), EPS);
    }

    @Test
    void swaySignsFollowCameraTurn() {
        // Поворот вправо (yaw+): предмет отстаёт влево на экране (+X) и доворачивается назад (ry-).
        HandPoseMath.Pose yaw = HandPoseMath.sway(2.0f, 0.0f, 1.0f);
        assertTrue(yaw.dx() > 0.0f);
        assertTrue(yaw.ry() < 0.0f);
        assertTrue(yaw.rz() > 0.0f);
        // Взгляд вниз (pitch+): предмет отстаёт вверх (+Y) и задирается (rx-).
        HandPoseMath.Pose pitch = HandPoseMath.sway(0.0f, 2.0f, 1.0f);
        assertTrue(pitch.dy() > 0.0f);
        assertTrue(pitch.rx() < 0.0f);
        assertEquals(0.0f, pitch.dx(), EPS);
    }

    @Test
    void swayMirrorsForOppositeLag() {
        HandPoseMath.Pose positive = HandPoseMath.sway(2.0f, 1.0f, 1.0f);
        HandPoseMath.Pose negative = HandPoseMath.sway(-2.0f, -1.0f, 1.0f);
        assertEquals(-positive.dx(), negative.dx(), EPS);
        assertEquals(-positive.dy(), negative.dy(), EPS);
        assertEquals(-positive.rx(), negative.rx(), EPS);
        assertEquals(-positive.ry(), negative.ry(), EPS);
        assertEquals(-positive.rz(), negative.rz(), EPS);
    }

    @Test
    void bobZeroAmplitudeIsZero() {
        assertEquals(HandPoseMath.ZERO, HandPoseMath.bob(3.0f, 0.0f, 1.0f));
        assertEquals(HandPoseMath.ZERO, HandPoseMath.bob(3.0f, 0.8f, 0.0f));
    }

    @Test
    void bobIsBoundedAndScales() {
        for (float phase = 0.0f; phase < 20.0f; phase += 0.1f) {
            HandPoseMath.Pose pose = HandPoseMath.bob(phase, 1.0f, 1.0f);
            assertTrue(Math.abs(pose.dx()) <= 0.045f + EPS);
            assertTrue(Math.abs(pose.dy()) <= 0.035f + EPS);
            assertTrue(Math.abs(pose.rz()) <= 2.5f + EPS);
            assertTrue(pose.dy() <= 0.0f, "вертикаль боба не должна поднимать предмет");
        }
        HandPoseMath.Pose half = HandPoseMath.bob(0.7f, 0.5f, 1.0f);
        HandPoseMath.Pose full = HandPoseMath.bob(0.7f, 1.0f, 1.0f);
        assertEquals(full.dx() * 0.5f, half.dx(), EPS);
    }

    @Test
    void driftPullsEachArmTowardCenter() {
        HandPoseMath.Pose right = HandPoseMath.drift(1.0f, 1.0f, true);
        assertTrue(right.dx() > 0.0f, "правая рука тянется влево на экране (+X)");
        HandPoseMath.Pose left = HandPoseMath.drift(1.0f, 1.0f, false);
        assertTrue(left.dx() < 0.0f, "левая рука тянется вправо на экране (-X)");
        assertEquals(-right.dx(), left.dx(), EPS);
        assertEquals(-right.ry(), left.ry(), EPS);
        assertEquals(-right.rz(), left.rz(), EPS);
    }

    @Test
    void driftZeroProgressAndStrengthGiveZero() {
        assertEquals(HandPoseMath.ZERO, HandPoseMath.drift(0.0f, 1.0f, true));
        assertEquals(HandPoseMath.ZERO, HandPoseMath.drift(1.0f, 0.0f, false));
    }

    @Test
    void joinDropAtFullProgressIsZero() {
        assertEquals(HandPoseMath.ZERO, HandPoseMath.joinDrop(1.0f));
    }

    @Test
    void joinDropEasesOutMonotonically() {
        float previousDrop = Float.POSITIVE_INFINITY;
        for (float p = 0.0f; p <= 1.0f; p += 0.05f) {
            float drop = -HandPoseMath.joinDrop(p).dy();
            assertTrue(drop >= 0.0f && drop <= 0.85f + EPS);
            assertTrue(drop <= previousDrop, "подъём должен замедляться к концу");
            previousDrop = drop;
        }
        assertEquals(0.85f, -HandPoseMath.joinDrop(0.0f).dy(), EPS);
    }

    @Test
    void addCombinesPosesAndZeroIsIdentity() {
        HandPoseMath.Pose a = HandPoseMath.sway(2.0f, 1.0f, 1.0f);
        HandPoseMath.Pose b = HandPoseMath.bob(0.5f, 1.0f, 1.0f);
        HandPoseMath.Pose sum = HandPoseMath.add(a, b);
        assertEquals(a.dx() + b.dx(), sum.dx(), EPS);
        assertEquals(a.rx() + b.rx(), sum.rx(), EPS);
        assertEquals(a, HandPoseMath.add(a, HandPoseMath.ZERO));
        assertEquals(a, HandPoseMath.add(HandPoseMath.ZERO, a));
    }

    @Test
    void significanceThresholdWorks() {
        assertFalse(HandPoseMath.ZERO.isSignificant(1.0e-4f));
        assertTrue(HandPoseMath.sway(1.0f, 0.0f, 1.0f).isSignificant(1.0e-4f));
        assertFalse(HandPoseMath.drift(1.0f, 1.0f, true).isSignificant(20.0f));
    }
}
