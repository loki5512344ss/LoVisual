/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation.hand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Фаза и амплитуда покачивания при ходьбе. */
public final class BobMathTest {

    @Test
    void amplitudeIsZeroWhenIdle() {
        assertEquals(0.0f, BobMath.amplitude(0.0f), 1.0e-6f);
    }

    @Test
    void amplitudeGrowsWithSpeed() {
        float slow = BobMath.amplitude(0.25f);
        float fast = BobMath.amplitude(0.9f);
        assertTrue(slow > 0.0f && slow < fast, "амплитуда должна расти со скоростью");
        assertTrue(slow < 1.0f && fast <= 1.0f);
    }

    @Test
    void amplitudeClampedToOne() {
        assertEquals(1.0f, BobMath.amplitude(2.5f), 1.0e-6f);
    }

    @Test
    void brokenSpeedGivesZeroAmplitude() {
        assertEquals(0.0f, BobMath.amplitude(Float.NaN), 1.0e-6f);
        assertEquals(0.0f, BobMath.amplitude(Float.NEGATIVE_INFINITY), 1.0e-6f);
        assertEquals(0.0f, BobMath.amplitude(-0.5f), 1.0e-6f);
    }

    @Test
    void lateralStartsAtZeroAndIsBounded() {
        assertEquals(0.0f, BobMath.lateral(0.0f), 1.0e-6f);
        for (float phase = 0.0f; phase < 40.0f; phase += 0.125f) {
            float value = BobMath.lateral(phase);
            assertTrue(value >= -1.0f && value <= 1.0f, "латераль вне диапазона в фазе " + phase);
        }
    }

    @Test
    void lateralIsAntisymmetric() {
        for (float phase = -3.0f; phase <= 3.0f; phase += 0.25f) {
            assertEquals(-BobMath.lateral(phase), BobMath.lateral(-phase), 1.0e-5f);
        }
    }

    @Test
    void lateralRepeatsAfterFullCycle() {
        float cycle = (float) (2.0 * Math.PI / BobMath.STEP_RATE);
        assertEquals(BobMath.lateral(0.3f), BobMath.lateral(0.3f + cycle), 1.0e-4f);
    }

    @Test
    void lateralReachesPeakAtExpectedPhase() {
        assertEquals(1.0f, BobMath.lateral(BobMath.lateralPeakPhase()), 1.0e-4f);
    }

    @Test
    void verticalIsNeverPositive() {
        for (float phase = 0.0f; phase < 40.0f; phase += 0.125f) {
            assertTrue(BobMath.vertical(phase) <= 0.0f, "вертикаль должна тянуть вниз");
            assertTrue(BobMath.vertical(phase) >= -1.0f);
        }
    }

    @Test
    void verticalDipsTwicePerCycle() {
        float cycle = (float) (2.0 * Math.PI / BobMath.STEP_RATE);
        int dips = 0;
        float previous = 0.0f;
        for (float t = 0.0f; t <= cycle; t += 0.01f) {
            float value = BobMath.vertical(t);
            if (previous > -0.999f && value <= -0.999f) {
                dips++;
            }
            previous = value;
        }
        assertEquals(2, dips, "за цикл должно быть два проседания (два шага)");
    }

    @Test
    void stepRateMatchesVanillaLimbFrequency() {
        assertEquals(0.6662f, BobMath.STEP_RATE, 1.0e-6f);
    }
}
