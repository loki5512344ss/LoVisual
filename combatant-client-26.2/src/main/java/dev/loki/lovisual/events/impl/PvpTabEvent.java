/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import dev.loki.lovisual.events.Event;

/**
 * Fired when the server updates tab-list header/footer text.
 */
public class PvpTabEvent extends Event {
    public final String header;
    public final String footer;
    public final long timeMs;

    public PvpTabEvent(String header, String footer) {
        this(header, footer, System.currentTimeMillis());
    }

    public PvpTabEvent(String header, String footer, long timeMs) {
        this.header = header == null ? "" : header;
        this.footer = footer == null ? "" : footer;
        this.timeMs = timeMs;
    }

    public String combinedText() {
        if (header.isBlank()) return footer;
        if (footer.isBlank()) return header;
        return header + "\n" + footer;
    }
}
