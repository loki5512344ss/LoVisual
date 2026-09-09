/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CommandPrefixTest {

    @Test
    void percentIsTheActivePrefix() {
        assertEquals('%', CommandPrefix.CHAR);
        assertTrue(CommandPrefix.isCommandLike("%help"));
        assertTrue(CommandPrefix.isCommandLike("   %toggle keepinventory on"));
    }

    @Test
    void legacyAtIsStillAccepted() {
        assertTrue(CommandPrefix.isCommandLike("@help"));
        assertEquals('@', CommandPrefix.leadingPrefix("@help"));
    }

    @Test
    void plainChatIsNotACommand() {
        assertFalse(CommandPrefix.isCommandLike("hello world"));
        assertFalse(CommandPrefix.isCommandLike("/vanilla command"));
        assertFalse(CommandPrefix.isCommandLike(""));
        assertFalse(CommandPrefix.isCommandLike(null));
        assertEquals(0, CommandPrefix.leadingPrefix("50% off"));
    }

    @Test
    void stripRemovesPrefixAndWhitespace() {
        assertEquals("help", CommandPrefix.strip("  %help "));
        assertEquals("toggle mod on", CommandPrefix.strip("@toggle mod on"));
        assertEquals("plain text", CommandPrefix.strip("plain text"));
        assertNull(CommandPrefix.strip(null));
        assertEquals("", CommandPrefix.strip("   "));
    }
}
