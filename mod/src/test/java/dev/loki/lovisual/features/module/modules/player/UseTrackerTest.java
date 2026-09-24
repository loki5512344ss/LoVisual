package dev.loki.lovisual.features.module.modules.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.features.module.modules.player.UseTracker;

public final class UseTrackerTest {

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
