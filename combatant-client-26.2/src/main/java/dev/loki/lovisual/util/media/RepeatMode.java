/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * This file belongs to LoVisual's MediaPlayerInfo integration, based on
 * https://github.com/Redstonecrafter0/MediaPlayerInfo.
 * Copyright (c) Redstonecrafter0 and contributors.
 *
 * Licensed under the GNU Affero General Public License v3.0.
 * See THIRD_PARTY_NOTICES.md for details.
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
