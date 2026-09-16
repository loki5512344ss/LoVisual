/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.mctiers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MCTiersTierTest {

    @Test
    void labelsMatchKnownTiers() {
        assertEquals("HT1", new MCTiersTier(1, false).label());
        assertEquals("LT2", new MCTiersTier(2, false).label());
        assertEquals("LT3", new MCTiersTier(3, false).label());
        assertEquals("LT4", new MCTiersTier(4, false).label());
        assertEquals("T5", new MCTiersTier(5, false).label());
    }

    @Test
    void colorsAreDistinctPerTier() {
        int t1 = new MCTiersTier(1, false).colorArgb();
        int t2 = new MCTiersTier(2, false).colorArgb();
        int t3 = new MCTiersTier(3, false).colorArgb();
        int t4 = new MCTiersTier(4, false).colorArgb();
        assertEquals(4, java.util.Set.of(t1, t2, t3, t4).size());
    }

    @Test
    void modeApiKeyMapsFixTypeToApiMode() {
        assertEquals("vanilla", MCTiersTier.modeApiKey("Vanilla"));
        assertEquals("uhc", MCTiersTier.modeApiKey("UHC"));
        assertEquals("pot", MCTiersTier.modeApiKey("Pot"));
        assertEquals("neth_pot", MCTiersTier.modeApiKey("NethOP"));
        assertEquals("smp", MCTiersTier.modeApiKey("SMP"));
        assertEquals("sword", MCTiersTier.modeApiKey("Sword"));
        assertEquals("axe", MCTiersTier.modeApiKey("Axe"));
        assertEquals("mace", MCTiersTier.modeApiKey("Mace"));
    }

    @Test
    void modeApiKeyDefaultsToVanillaForUnknownOrNull() {
        assertEquals("vanilla", MCTiersTier.modeApiKey(null));
        assertEquals("vanilla", MCTiersTier.modeApiKey("Nonsense"));
    }
}
