/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.util;

/**
 * Single source of truth for the client command prefix. Messages typed in chat
 * starting with this character are handled by the client and never sent to the
 * server. Also covers the legacy '@' so old profiles/links keep working.
 */
public final class CommandPrefix {
    /** The active client command prefix. */
    public static final char CHAR = '%';
    /** Legacy prefix, still accepted for compatibility. */
    public static final char LEGACY = '@';
    /** The prefix as a string (for messages, usage strings and click events). */
    public static final String STRING = String.valueOf(CHAR);

    private CommandPrefix() {
    }

    /** @return true when the first non-space character is the (or a legacy) prefix */
    public static boolean isCommandLike(String text) {
        return leadingPrefix(text) != 0;
    }

    /**
     * @return the prefix char found before any command text, 0 when the input
     *         does not start with a command prefix
     */
    public static char leadingPrefix(String text) {
        if (text == null) return 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) continue;
            return (c == CHAR || c == LEGACY) ? c : 0;
        }
        return 0;
    }

    /** Strips the leading prefix and surrounding whitespace; null-safe. */
    public static String strip(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return trimmed;
        char p = trimmed.charAt(0);
        return (p == CHAR || p == LEGACY) ? trimmed.substring(1).trim() : trimmed;
    }
}
