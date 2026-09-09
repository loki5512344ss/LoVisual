/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class AutoAcceptTest {

    private static final Set<String> PATTERNS = Set.of("просит телепортироваться к себе", "has requested to teleport");

    @Test
    void extractsSenderBeforeRussianPattern() {
        String sender = AutoAccept.TeleportRequestParser.extractSender(
                "Steve123 просит телепортироваться к себе", PATTERNS);
        assertEquals("Steve123", sender);
    }

    @Test
    void extractsSenderBeforeEnglishPattern() {
        String sender = AutoAccept.TeleportRequestParser.extractSender(
                "Herobrine has requested to teleport to you", PATTERNS);
        assertEquals("Herobrine", sender);
    }

    @Test
    void ignoresDecorationsBeforeNickname() {
        String sender = AutoAccept.TeleportRequestParser.extractSender(
                "» [Клан] Night_Wolf просит телепортироваться к себе", PATTERNS);
        assertEquals("Night_Wolf", sender);
    }

    @Test
    void unrelatedMessageYieldsNull() {
        assertNull(AutoAccept.TeleportRequestParser.extractSender("Player joined the game", PATTERNS));
    }

    @Test
    void blankOrNullInputsYieldNull() {
        assertNull(AutoAccept.TeleportRequestParser.extractSender(null, PATTERNS));
        assertNull(AutoAccept.TeleportRequestParser.extractSender("", PATTERNS));
        assertNull(AutoAccept.TeleportRequestParser.extractSender("Steve просит телепортироваться к себе", Set.of()));
        assertNull(AutoAccept.TeleportRequestParser.extractSender("Steve просит телепортироваться к себе", null));
    }

    @Test
    void patternAtStartOfMessageYieldsNull() {
        // No nickname-like token before the pattern means no sender.
        assertNull(AutoAccept.TeleportRequestParser.extractSender("просит телепортироваться к себе", PATTERNS));
    }

    @Test
    void earliestPatternWins() {
        Set<String> both = Set.of("has requested", "просит телепортироваться к себе");
        String sender = AutoAccept.TeleportRequestParser.extractSender(
                "Alex has requested to teleport (просит телепортироваться к себе)", both);
        assertEquals("Alex", sender);
    }

    @Test
    void shortTokensAreNotNicknames() {
        assertNull(AutoAccept.TeleportRequestParser.lastNicknameIn("ab – "));
        assertEquals("abc", AutoAccept.TeleportRequestParser.lastNicknameIn("abc – "));
    }
}
