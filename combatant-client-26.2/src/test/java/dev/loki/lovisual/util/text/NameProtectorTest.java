/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameProtectorTest {

    @Test
    void replacesNameCaseInsensitively() {
        NameProtector p = new NameProtector();
        p.put("Notch", "Steve");

        assertEquals("hi Steve!", p.replace("hi NOTCH!"));
    }

    @Test
    void onlyMatchesWholeWords() {
        NameProtector p = new NameProtector();
        p.put("Notch", "Steve");

        assertEquals("Notchless is a biome-like word", p.replace("Notchless is a biome-like word"));
        assertEquals("xNotch Notchy", p.replace("xNotch Notchy"));
    }

    @Test
    void longerNamesWinOverSubstrings() {
        NameProtector p = new NameProtector();
        p.put("Cool", "X");
        p.put("CoolGuy", "Y");

        assertEquals("I am Y", p.replace("I am CoolGuy"));
        assertEquals("X beans", p.replace("Cool beans"));
    }

    @Test
    void underscoresCountAsWordCharacters() {
        NameProtector p = new NameProtector();
        p.put("lob", "cat");

        // "lobo" и "_lob_" не матчатся (word-символы вокруг), isolated "lob" — матчится
        assertEquals("lobo_here _lob_ cat", p.replace("lobo_here _lob_ lob"));
    }

    @Test
    void multipleNamesInOneString() {
        NameProtector p = new NameProtector();
        p.put("Alice", "**");
        p.put("Bob", "**");

        assertEquals("** and **", p.replace("Alice and Bob"));
    }

    @Test
    void blankNamesIgnoredAndEmptyWhenNone() {
        NameProtector p = new NameProtector();
        p.put(null, "x");
        p.put("  ", "x");

        assertTrue(p.isEmpty());
        assertEquals("anything", p.replace("anything"));
    }

    @Test
    void emptyAliasDeletesName() {
        NameProtector p = new NameProtector();
        p.put("Notch", "");

        assertEquals("hi !", p.replace("hi Notch!"));
    }

    @Test
    void nullAndEmptyTextPassThrough() {
        NameProtector p = new NameProtector();
        p.put("Notch", "X");

        assertEquals(null, p.replace(null));
        assertEquals("", p.replace(""));
    }

    @Test
    void exactReplacementOnlyForWholeString() {
        NameProtector p = new NameProtector();
        p.put("Notch", "X");

        assertEquals("X", p.replaceExact("Notch"));
        assertEquals("Notchy", p.replaceExact("Notchy"));
    }

    @Test
    void clearRemovesAllAliases() {
        NameProtector p = new NameProtector();
        p.put("Notch", "X");
        p.clear();

        assertTrue(p.isEmpty());
        assertEquals("Notch", p.replace("Notch"));
    }

    @Test
    void duplicateNameOverwritesAlias() {
        NameProtector p = new NameProtector();
        p.put("Notch", "X");
        p.put("Notch", "Y");

        assertEquals("Y", p.replace("Notch"));
    }
}
