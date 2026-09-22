/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.features.module.modules.visuals.items.held.HoldMyItems;

/**
 * Математика ease-out-back для HoldMyItems.
 */
public final class EaseCurvesTest {

    private static final float EPS = 1e-4f;
    private static final float DEFAULT_C1 = 1.70158f;

    @Test
    void easeOutBackStartsAtZeroAndEndsAtOne() {
        assertEquals(0.0f, EaseCurves.easeOutBack(0.0f, DEFAULT_C1), EPS);
        assertEquals(1.0f, EaseCurves.easeOutBack(1.0f, DEFAULT_C1), EPS);
    }

    @Test
    void easeOutBackOvershootsPastOne() {
        float max = 0.0f;
        for (int i = 0; i <= 100; i++) {
            max = Math.max(max, EaseCurves.easeOutBack(i / 100.0f, DEFAULT_C1));
        }
        assertTrue(max > 1.0f && max < 1.2f, "overshoot должен быть заметным, но умеренным: " + max);
    }

    @Test
    void shapeKeepsEndpoints() {
        assertEquals(0.0f, EaseCurves.shapeEquipProgress(0.0f, DEFAULT_C1, 1.0f), EPS);
        assertEquals(1.0f, EaseCurves.shapeEquipProgress(1.0f, DEFAULT_C1, 1.0f), EPS);
    }

    @Test
    void zeroIntensityFallsBackToLinear() {
        for (int i = 0; i <= 10; i++) {
            float x = i / 10.0f;
            assertEquals(x, EaseCurves.shapeEquipProgress(x, DEFAULT_C1, 0.0f), EPS);
        }
    }

    @Test
    void fullIntensityMatchesCurve() {
        float x = 0.7f;
        assertEquals(
                EaseCurves.easeOutBack(x, DEFAULT_C1),
                EaseCurves.shapeEquipProgress(x, DEFAULT_C1, 1.0f),
                EPS
        );
    }

    @Test
    void intensityClamps() {
        float x = 0.5f;
        assertEquals(
                EaseCurves.shapeEquipProgress(x, DEFAULT_C1, 0.0f),
                EaseCurves.shapeEquipProgress(x, DEFAULT_C1, -5.0f),
                EPS
        );
        assertEquals(
                EaseCurves.shapeEquipProgress(x, DEFAULT_C1, 1.0f),
                EaseCurves.shapeEquipProgress(x, DEFAULT_C1, 5.0f),
                EPS
        );
    }
}
