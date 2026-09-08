/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UseTrackerTest {

    @Test
    void defaultOptionsTrackInvisibilityOnly() {
        var options = UseTracker.UseEffects.defaultOptions();
        assertEquals(6, options.size());
        assertTrue(options.get("invisibility"));
        assertEquals(false, options.get("speed"));
        assertEquals(false, options.get("strength"));
    }

    @Test
    void totemPopEventIdMatchesVanilla() {
        // Entity event 35 is the vanilla totem-of-undying activation event.
        assertEquals(35, UseTracker.TOTEM_POP_EVENT_ID);
    }

    @Test
    void knownKeysResolveToHumanNames() {
        assertEquals("Invisibility", UseTracker.UseEffects.displayName("invisibility"));
        assertEquals("Fire Resistance", UseTracker.UseEffects.displayName("fire_resistance"));
        assertEquals("an effect", UseTracker.UseEffects.displayName("unknown"));
    }
}
