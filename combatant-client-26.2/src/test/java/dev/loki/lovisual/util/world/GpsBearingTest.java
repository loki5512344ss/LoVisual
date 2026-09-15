/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GpsBearingTest {

    private static final float EPS = 1.0e-3f;

    @Test
    void bearingPointsSouthWhenTargetAheadOnZ() {
        // +Z (юг) при dx=0 -> азимут 0, совпадает с конвенцией getYRot()
        assertEquals(0.0f, GpsBearing.bearingDegrees(0.0, 10.0), EPS);
    }

    @Test
    void bearingPointsWestWhenTargetOnNegativeX() {
        assertEquals(90.0f, GpsBearing.bearingDegrees(-10.0, 0.0), EPS);
    }

    @Test
    void arrowRotationIsZeroWhenFacingTarget() {
        assertEquals(0.0f, GpsBearing.arrowRotationDegrees(45.0f, 45.0f), EPS);
    }

    @Test
    void arrowRotationWrapsToShortestPath() {
        // цель за спиной (разница 180) должна остаться в диапазоне (-180, 180]
        float result = GpsBearing.arrowRotationDegrees(170.0f, -170.0f);
        assertEquals(-20.0f, result, EPS);
    }

    @Test
    void wrapDegreesStaysInHalfOpenRange() {
        assertEquals(-179.0f, GpsBearing.wrapDegrees(181.0f), EPS);
        assertEquals(179.0f, GpsBearing.wrapDegrees(-181.0f), EPS);
        assertEquals(0.0f, GpsBearing.wrapDegrees(360.0f), EPS);
        assertEquals(90.0f, GpsBearing.wrapDegrees(90.0f), EPS);
    }

    @Test
    void horizontalDistanceIsPythagorean() {
        assertEquals(5.0, GpsBearing.horizontalDistance(3.0, 4.0), 1.0e-9);
    }
}
