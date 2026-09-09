/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import dev.loki.lovisual.features.module.modules.visuals.items.AlwaysVisibleHand;

public final class AlwaysVisibleHandTest {

    @Test
    void bothModeForcesBothHandsVisible() {
        assertArrayEquals(new boolean[]{true, true},
                AlwaysVisibleHand.resolveSelection(AlwaysVisibleHand.SELECTION_BOTH, false, false));
        assertArrayEquals(new boolean[]{true, true},
                AlwaysVisibleHand.resolveSelection(AlwaysVisibleHand.SELECTION_BOTH, true, false));
    }

    @Test
    void mainModeKeepsMainHandAndPreservesVanillaOffHand() {
        assertArrayEquals(new boolean[]{true, false},
                AlwaysVisibleHand.resolveSelection(AlwaysVisibleHand.SELECTION_MAIN, false, false));
        assertArrayEquals(new boolean[]{true, true},
                AlwaysVisibleHand.resolveSelection(AlwaysVisibleHand.SELECTION_MAIN, false, true));
    }

    @Test
    void offModeKeepsOffHandAndPreservesVanillaMainHand() {
        assertArrayEquals(new boolean[]{false, true},
                AlwaysVisibleHand.resolveSelection(AlwaysVisibleHand.SELECTION_OFF, false, true));
        assertArrayEquals(new boolean[]{true, true},
                AlwaysVisibleHand.resolveSelection(AlwaysVisibleHand.SELECTION_OFF, true, true));
    }

    @Test
    void unknownModeFallsBackToVanillaDecision() {
        assertArrayEquals(new boolean[]{false, true},
                AlwaysVisibleHand.resolveSelection("Something", false, true));
    }
}
