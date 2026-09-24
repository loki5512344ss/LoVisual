package dev.loki.lovisual.events.impl.pvp;

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
