package dev.loki.lovisual.util.media.impl.linux;

import org.freedesktop.dbus.types.Variant;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Package-private D-Bus metadata/property readers backing {@link LinuxMediaSession}:
 * unwraps {@link Variant} values and turns the raw MPR2 metadata map plus the
 * player's boolean/numeric properties into plain Java values. Pure reads only —
 * no track-change or seek state lives here.
 */
final class LinuxMetadataReader {
    private static final ConcurrentHashMap<String, byte[]> ARTWORK_CACHE_BY_URL = new ConcurrentHashMap<>();

    private LinuxMetadataReader() {
    }

    /** A snapshot of the player's raw state for one refresh tick. */
    record BaseState(Map<String, Object> metadata,
                     boolean playing,
                     String title,
                     String artist,
                     long duration,
                     String trackId,
                     long positionRaw) {
    }

    static BaseState readBaseState(String owner) {
        Map<String, Object> metadata;
        try {
            Object raw = LinuxMediaPlayerInfo.INSTANCE.getProperty(owner, "Metadata");
            metadata = raw instanceof Map<?, ?> map ? castMap(map) : Map.of();
        } catch (Exception ignored) {
            metadata = Map.of();
        }

        boolean playing;
        try {
            playing = "Playing".equals(LinuxMediaPlayerInfo.INSTANCE.getProperty(owner, "PlaybackStatus"));
        } catch (Exception ignored) {
            playing = false;
        }

        return new BaseState(
                metadata,
                playing,
                readTitle(metadata),
                readArtist(metadata),
                readDurationSeconds(metadata),
                extractTrackId(metadata),
                readPositionSeconds(owner)
        );
    }

    static boolean getCanSeek(String owner) {
        try {
            return switch (unwrap(LinuxMediaPlayerInfo.INSTANCE.getProperty(owner, "CanSeek"))) {
                case Boolean value -> value;
                case Number value -> value.intValue() != 0;
                case String value -> value.equalsIgnoreCase("true") || value.equals("1");
                case null, default -> false;
            };
        } catch (Exception ignored) {
            return false;
        }
    }

    static Boolean getShuffleValue(String owner) {
        try {
            Object value = unwrap(LinuxMediaPlayerInfo.INSTANCE.getProperty(owner, "Shuffle"));
            if (value instanceof Boolean bool) return bool;
            if (value instanceof Number number) return number.doubleValue() >= 0.5;
            if (value instanceof String string) {
                try {
                    return Double.parseDouble(string) >= 0.5;
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    static String getLoopStatus(String owner) {
        try {
            Object value = unwrap(LinuxMediaPlayerInfo.INSTANCE.getProperty(owner, "LoopStatus"));
            return value != null ? value.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    static Object unwrap(Object value) {
        return value instanceof Variant<?> variant ? variant.getValue() : value;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static long readPositionSeconds(String owner) {
        Object raw;
        try {
            raw = LinuxMediaPlayerInfo.INSTANCE.getProperty(owner, "Position");
        } catch (Exception ignored) {
            return 0L;
        }
        Long positionUs = toLong(unwrap(raw));
        return positionUs != null ? positionUs / 1_000_000L : 0L;
    }

    private static long readDurationSeconds(Map<String, Object> metadata) {
        Long durationUs = toLong(unwrap(metadata.get("mpris:length")));
        return durationUs != null ? durationUs / 1_000_000L : 0L;
    }

    private static String extractTrackId(Map<String, Object> metadata) {
        Object raw = metadata.get("mpris:trackid");
        Object value = unwrap(raw);
        return value != null ? value.toString() : null;
    }

    private static String readTitle(Map<String, Object> metadata) {
        Object raw = unwrap(metadata.get("xesam:title"));
        return raw instanceof String string ? string : "";
    }

    private static String readArtist(Map<String, Object> metadata) {
        Object raw = unwrap(metadata.get("xesam:artist"));
        if (raw instanceof String string) return string;
        if (raw instanceof Object[] array) return joinObjects(Arrays.asList(array));
        if (raw instanceof List<?> list) return joinObjects(list);
        return "";
    }

    private static String joinObjects(List<?> values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (value == null) continue;
            if (!builder.isEmpty()) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }

    static byte[] readArtworkBytes(Map<String, Object> metadata) {
        Object raw = unwrap(metadata.get("mpris:artUrl"));
        if (!(raw instanceof String url) || url.isBlank()) return new byte[0];

        byte[] cached = ARTWORK_CACHE_BY_URL.get(url);
        if (cached != null) return cached;

        try {
            URL connectionUrl = new URL(url);
            var connection = connectionUrl.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            byte[] bytes;
            try (InputStream stream = connection.getInputStream()) {
                bytes = stream.readAllBytes();
            }
            if (bytes.length > 0) {
                ARTWORK_CACHE_BY_URL.put(url, bytes);
            }
            return bytes;
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
