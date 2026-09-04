/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.media;

import java.util.Locale;

/**
 * Modified Java port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 */
public enum RepeatMode {
    OFF,
    ONE,
    ALL,
    UNKNOWN;

    public static RepeatMode fromMpris(String value) {
        if (value == null) return UNKNOWN;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "none" -> OFF;
            case "track" -> ONE;
            case "playlist" -> ALL;
            default -> UNKNOWN;
        };
    }
}
