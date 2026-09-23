/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.features.module.modules.misc.items.ScrollMovePolicy;

public final class ScrollMovePolicyTest {

    @Test
    void tinyScrollIsIgnored() {
        assertFalse(ScrollMovePolicy.isScrollSignificant(0.0));
        assertFalse(ScrollMovePolicy.isScrollSignificant(0.01));
        assertTrue(ScrollMovePolicy.isScrollSignificant(1.0));
        assertTrue(ScrollMovePolicy.isScrollSignificant(-1.0));
    }

    @Test
    void directionMatchesSign() {
        assertEquals(1, ScrollMovePolicy.direction(2.0));
        assertEquals(-1, ScrollMovePolicy.direction(-0.5));
    }

    @Test
    void hoveredSlotWinsOverHotbarFallback() {
        assertEquals(17, ScrollMovePolicy.resolveTargetSlot(true, 17, 3, 36));
    }

    @Test
    void hotbarFallbackMapsSelectedSlotIntoMenu() {
        // Standard inventory menu: 36 slots, hotbar = last 9 (indices 27..35).
        assertEquals(27, ScrollMovePolicy.resolveTargetSlot(false, -1, 0, 36));
        assertEquals(30, ScrollMovePolicy.resolveTargetSlot(false, -1, 3, 36));
        assertEquals(35, ScrollMovePolicy.resolveTargetSlot(false, -1, 8, 36));
    }

    @Test
    void hotbarIndexWrapsNegativeSlots() {
        assertEquals(35, ScrollMovePolicy.hotbarIndexInMenu(-1, 36));
    }

    @Test
    void emptyMenuYieldsNoSlot() {
        assertEquals(-1, ScrollMovePolicy.hotbarIndexInMenu(0, 0));
        assertEquals(-1, ScrollMovePolicy.hotbarIndexInMenu(0, -5));
    }

    @Test
    void quickMoveUsesLeftClickButton() {
        assertEquals(0, ScrollMovePolicy.quickMoveButton());
    }
}
