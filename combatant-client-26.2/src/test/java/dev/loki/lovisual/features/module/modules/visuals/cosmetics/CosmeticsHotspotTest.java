/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.cosmetics;

import dev.loki.lovisual.features.cosmetic.CosmeticSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import dev.loki.lovisual.features.module.modules.visuals.cosmetics.CosmeticsHotspot;

public final class CosmeticsHotspotTest {

    @Test
    void headIsTopZone() {
        CosmeticsHotspot[] zones = CosmeticsHotspot.forRegion(0f, 0f, 100f, 100f, false);
        assertEquals(CosmeticSlot.HEAD, CosmeticsHotspot.hitTest(zones, 50, 5));
    }

    @Test
    void legsIsBottomZone() {
        CosmeticsHotspot[] zones = CosmeticsHotspot.forRegion(0f, 0f, 100f, 100f, false);
        assertEquals(CosmeticSlot.LEGS, CosmeticsHotspot.hitTest(zones, 50, 95));
    }

    @Test
    void shouldersZoneBecomesBackWhenBackViewToggled() {
        CosmeticsHotspot[] front = CosmeticsHotspot.forRegion(0f, 0f, 100f, 100f, false);
        CosmeticsHotspot[] back = CosmeticsHotspot.forRegion(0f, 0f, 100f, 100f, true);

        assertEquals(CosmeticSlot.SHOULDERS, CosmeticsHotspot.hitTest(front, 50, 25));
        assertEquals(CosmeticSlot.BACK, CosmeticsHotspot.hitTest(back, 50, 25));
    }

    @Test
    void outsideRegionMissesAllZones() {
        CosmeticsHotspot[] zones = CosmeticsHotspot.forRegion(10f, 10f, 100f, 100f, false);
        assertNull(CosmeticsHotspot.hitTest(zones, 5, 5));
    }
}
