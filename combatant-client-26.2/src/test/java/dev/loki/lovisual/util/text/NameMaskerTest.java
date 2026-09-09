/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class NameMaskerTest {

    @Test
    void masksNicknameTokenDeterministically() {
        NameMasker masker = new NameMasker("Player", List.of());
        String first = masker.mask("gg Notch_123 gg");
        String second = masker.mask("gg Notch_123 gg");

        assertNotEquals("gg Notch_123 gg", first);
        assertEquals(first, second);
        assertEquals("gg Player1 gg", first);
    }

    @Test
    void differentNamesGetDifferentOrdinals() {
        NameMasker masker = new NameMasker("Player", List.of());
        String masked = masker.mask("Notch and Herobrine");

        assertEquals("Player1 and Player2", masked);
    }

    @Test
    void shortTokensAndNumbersAreKept() {
        NameMasker masker = new NameMasker("Player", List.of());

        assertEquals("ab 12345 x9", masker.mask("ab 12345 x9"));
    }

    @Test
    void wordsInsideTokensAreNotSplit() {
        NameMasker masker = new NameMasker("Player", List.of());

        // The nickname-like token wins over word matching because it is longer.
        String masked = masker.mask("Steve2016");
        assertEquals("Player1", masked);
    }

    @Test
    void serverWordsAreMaskedCaseInsensitively() {
        NameMasker masker = new NameMasker("Server", List.of("anarchy"));

        assertEquals("Welcome to Server-2!", masker.mask("Welcome to Anarchy-2!"));
        assertEquals("Server", masker.mask("ANARCHY"));
    }

    @Test
    void wordMatchTakesPriorityOverTokenMasking() {
        NameMasker masker = new NameMasker("Server", List.of("anarchy"));

        // "anarchy" matches as a word first, so masking stops there.
        assertEquals("Server", masker.mask("anarchy"));
    }

    @Test
    void nullAndEmptyPassThrough() {
        NameMasker masker = new NameMasker("Player", List.of());

        assertEquals(null, masker.mask(null));
        assertEquals("", masker.mask(""));
    }

    @Test
    void blankReplacementFallsBackToPlayer() {
        NameMasker masker = new NameMasker("  ", List.of());

        assertEquals("Player1", masker.mask("Notch"));
    }
}
