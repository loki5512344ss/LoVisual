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

final class TapeMouseTest {

    @Test
    void firstCallIsDueImmediately() {
        TapeMouse.ClickPacer pacer = new TapeMouse.ClickPacer();
        assertTrue(pacer.isDue(1000L, 1000L));
    }

    @Test
    void clicksAreSpacedByInterval() {
        TapeMouse.ClickPacer pacer = new TapeMouse.ClickPacer();
        assertTrue(pacer.isDue(1000L, 500L));
        pacer.mark(1000L);

        assertFalse(pacer.isDue(1499L, 500L));
        assertTrue(pacer.isDue(1500L, 500L));
        assertTrue(pacer.isDue(4000L, 500L));
    }

    @Test
    void changingIntervalIsRespected() {
        TapeMouse.ClickPacer pacer = new TapeMouse.ClickPacer();
        pacer.mark(1000L);

        assertFalse(pacer.isDue(2499L, 2000L));
        assertTrue(pacer.isDue(3000L, 2000L));
        pacer.mark(3000L);

        assertTrue(pacer.isDue(3100L, 100L));
    }

    @Test
    void resetAllowsImmediateClickAgain() {
        TapeMouse.ClickPacer pacer = new TapeMouse.ClickPacer();
        pacer.mark(1000L);
        assertFalse(pacer.isDue(1100L, 5000L));

        pacer.reset();
        assertTrue(pacer.isDue(1100L, 5000L));
    }

    @Test
    void nonPositiveIntervalStillPacesEveryCall() {
        TapeMouse.ClickPacer pacer = new TapeMouse.ClickPacer();
        assertTrue(pacer.isDue(0L, 0L));
        pacer.mark(0L);
        assertFalse(pacer.isDue(0L, 0L));
        assertTrue(pacer.isDue(1L, 0L));
        assertTrue(pacer.isDue(1L, -100L));
    }
}
