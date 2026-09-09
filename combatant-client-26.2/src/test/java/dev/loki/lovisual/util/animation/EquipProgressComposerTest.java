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
import dev.loki.lovisual.features.module.modules.visuals.camera.ViewModel;
import dev.loki.lovisual.features.module.modules.visuals.items.HoldMyItems;

/**
 * Совместная работа HoldMyItems (кривая) и ViewModel (занижение):
 * конечная позиция определяется занижением, кривая добавляет перелёт.
 */
public final class EquipProgressComposerTest {

    private static final float EPS = 1e-4f;
    private static final float C1 = 1.6f;

    @Test
    void bothDisabledIsIdentity() {
        assertEquals(0.42f, EquipProgressComposer.compose(0.42f, false, 0, 0, false, 0), EPS);
    }

    @Test
    void loweringAloneScalesEndpoints() {
        assertEquals(0.0f, EquipProgressComposer.compose(0.0f, false, 0, 0, true, 0.5f), EPS);
        assertEquals(0.5f, EquipProgressComposer.compose(1.0f, false, 0, 0, true, 0.5f), EPS);
    }

    @Test
    void curveAloneKeepsEndpoints() {
        assertEquals(0.0f, EquipProgressComposer.compose(0.0f, true, C1, 1.0f, false, 0), EPS);
        assertEquals(1.0f, EquipProgressComposer.compose(1.0f, true, C1, 1.0f, false, 0), EPS);
    }

    @Test
    void togetherRestingPointIsLowered() {
        //equip завершён (progress=1): финальная позиция = занижение, а не 1
        float rest = EquipProgressComposer.compose(1.0f, true, C1, 1.0f, true, 0.5f);
        assertEquals(0.5f, rest, EPS);
    }

    @Test
    void togetherOvershootExceedsRestingPoint() {
        float max = 0.0f;
        for (int i = 0; i <= 100; i++) {
            max = Math.max(max, EquipProgressComposer.compose(i / 100.0f, true, C1, 1.0f, true, 0.5f));
        }
        assertTrue(max > 0.5f, "перелёт кривой должен поднимать предмет выше финальной позиции: " + max);
        assertTrue(max < 0.5f * 1.2f, "перелёт умеренный: " + max);
    }

    @Test
    void curveDisabledButLoweringOn() {
        assertEquals(0.25f, EquipProgressComposer.compose(0.5f, false, 0, 0, true, 0.5f), EPS);
    }

    @Test
    void loweringDisabledButCurveOn() {
        float x = 0.7f;
        assertEquals(
                EaseCurves.shapeEquipProgress(x, C1, 1.0f),
                EquipProgressComposer.compose(x, true, C1, 1.0f, false, 0),
                EPS
        );
    }
}
