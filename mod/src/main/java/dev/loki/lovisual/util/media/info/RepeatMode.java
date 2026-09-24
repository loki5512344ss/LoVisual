package dev.loki.lovisual.util.media.info;

import java.util.Locale;
import dev.loki.lovisual.util.media.info.MediaPlayerInfo;

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
