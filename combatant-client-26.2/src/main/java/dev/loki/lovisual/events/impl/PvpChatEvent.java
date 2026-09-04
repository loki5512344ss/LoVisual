/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import dev.loki.lovisual.events.Event;

/**
 * Fired when a PvP-related chat or game message is received.
 */
public class PvpChatEvent extends Event {
    public final String message;
    public final Source source;
    public final long timeMs;
    public PvpChatEvent(String message, Source source) {
        this(message, source, System.currentTimeMillis());
    }

    public PvpChatEvent(String message, Source source, long timeMs) {
        this.message = message;
        this.source = source;
        this.timeMs = timeMs;
    }

    public enum Source {
        GAME_MESSAGE,
        CHAT_MESSAGE
    }
}
