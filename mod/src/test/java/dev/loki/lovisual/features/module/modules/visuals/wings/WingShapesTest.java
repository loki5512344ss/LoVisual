/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.wings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WingShapesTest {

    @Test
    void everyTypeResolvesToNonEmptyShape() {
        for (String type : WingShapes.TYPES) {
            WingShapes.Point[] shape = WingShapes.shape(type);
            assertNotNull(shape, type);
            assertTrue(shape.length >= 10, type);
        }
        // unknown type falls back to Angelic
        assertSame(WingShapes.shape("Angelic"), WingShapes.shape("Nope"));
    }

    @Test
    void allShapesUseFullPointRanges() {
        for (String type : WingShapes.TYPES) {
            for (WingShapes.Point p : WingShapes.shape(type)) {
                assertTrue(p.alphaMul() > 0f && p.alphaMul() <= 1f, type + " " + p);
                assertTrue(p.x() >= 0f, type + " " + p);
            }
        }
    }

    @Test
    void ribIndicesFitShape() {
        for (String type : WingShapes.TYPES) {
            WingShapes.Point[] shape = WingShapes.shape(type);
            for (int idx : WingShapes.ribs(shape)) {
                assertTrue(idx < shape.length, type + " rib " + idx);
            }
        }
    }

    @Test
    void glidePoseScalesPitchWithProgress() {
        WingShapes.Pose start = WingShapes.glidePose(0f, 0f);
        WingShapes.Pose end = WingShapes.glidePose(0f, 1f);
        assertEquals(0f, start.pitchRotation(), 0.001f);
        assertEquals(-90f, end.pitchRotation(), 0.001f);
    }
}
