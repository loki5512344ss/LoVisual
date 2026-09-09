/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class UsageHintsTest {

    @Test
    void extractsPlaceholdersFromUsage() {
        List<String> parts = UsageHints.placeholders("%toggle <module> [on|off|toggle]");
        assertEquals(List.of("module", "on|off|toggle"), parts);
    }

    @Test
    void placeholderAtPosition() {
        String usage = "%friend [list|add|remove|toggle|clear] [player]";
        assertEquals("list|add|remove|toggle|clear", UsageHints.placeholder(usage, 1));
        assertEquals("player", UsageHints.placeholder(usage, 2));
        assertNull(UsageHints.placeholder(usage, 3));
    }

    @Test
    void usageWithoutPlaceholdersYieldsNothing() {
        assertNull(UsageHints.placeholder("%ping", 1));
        assertNull(UsageHints.placeholder(null, 1));
        assertNull(UsageHints.placeholder("", 1));
    }

    @Test
    void unclosedPlaceholderIsStillFound() {
        assertEquals(List.of("module"), UsageHints.placeholders("%bind <module"));
    }

    @Test
    void mixedAngleAndSquareBrackets() {
        assertEquals(List.of("a", "b"), UsageHints.placeholders("%x <a> [b]"));
    }
}
