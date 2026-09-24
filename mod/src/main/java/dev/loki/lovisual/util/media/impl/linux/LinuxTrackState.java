package dev.loki.lovisual.util.media.impl.linux;

import org.freedesktop.dbus.ObjectPath;
import dev.loki.lovisual.util.media.impl.linux.LinuxMetadataReader.BaseState;
import dev.loki.lovisual.util.media.impl.linux.dbus.Player;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Package-private per-owner track/seek bookkeeping for {@link LinuxMediaSession}:
 * remembers the last seen track, pending-track and nudge markers, and the
 * seek-override (a just-issued seek whose absolute position the player has not
 * reported yet) so the visible position does not jump backwards between refresh
 * ticks. All state is keyed by the D-Bus owner name and survives across the
 * short-lived {@link LinuxMediaSession} instances the poller creates.
 */
final class LinuxTrackState {
    private static final long SEEK_OVERRIDE_TOLERANCE_SECONDS = 2L;
    private static final long SEEK_OVERRIDE_TTL_MS = 15_000L;

    private static final ConcurrentHashMap<String, String> LAST_TRACK_ID_BY_OWNER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> LAST_FALLBACK_KEY_BY_OWNER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> PENDING_TRACK_BY_OWNER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> LAST_NUDGED_TRACK_ID_BY_OWNER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SeekOverrideState> SEEK_OVERRIDE_BY_OWNER = new ConcurrentHashMap<>();

    private LinuxTrackState() {
    }

    static void rememberSeekOverride(String owner, String trackId, long targetPosition, boolean playing, long duration) {
        SEEK_OVERRIDE_BY_OWNER.put(owner,
                new SeekOverrideState(trackId, targetPosition, System.currentTimeMillis(), playing, duration));
    }

    static void clearSeekOverride(String owner) {
        SEEK_OVERRIDE_BY_OWNER.remove(owner);
    }

    static long resolveVisiblePosition(String owner,
                                       String trackId,
                                       long rawPosition,
                                       boolean playing,
                                       long duration,
                                       boolean pending) {
        if (pending) return 0L;

        long now = System.currentTimeMillis();
        SeekOverrideState override = SEEK_OVERRIDE_BY_OWNER.get(owner);
        if (override == null) return clampPosition(rawPosition, duration);
        if (override.trackId != null && trackId != null && !override.trackId.equals(trackId)) {
            clearSeekOverride(owner);
            return clampPosition(rawPosition, duration);
        }
        if (now - override.appliedAtMs > SEEK_OVERRIDE_TTL_MS) {
            clearSeekOverride(owner);
            return clampPosition(rawPosition, duration);
        }

        long expected = clampPosition(override.expectedPosition(now), duration);
        long clampedRaw = clampPosition(rawPosition, duration);
        long diff = Math.abs(clampedRaw - expected);
        if (diff <= SEEK_OVERRIDE_TOLERANCE_SECONDS) {
            clearSeekOverride(owner);
            return clampedRaw;
        }
        if (clampedRaw > expected + SEEK_OVERRIDE_TOLERANCE_SECONDS) {
            clearSeekOverride(owner);
            return clampedRaw;
        }
        if (!playing && clampedRaw >= expected) {
            clearSeekOverride(owner);
            return clampedRaw;
        }
        return expected;
    }

    private static long clampPosition(long position, long duration) {
        if (position < 0L) return 0L;
        if (duration > 0L) return Math.min(position, duration);
        return position;
    }

    static String buildFallbackKey(String title, String artist, long duration) {
        if (title.isBlank() && artist.isBlank() && duration <= 0L) return null;
        return title + "\u0000" + artist + "\u0000" + duration;
    }

    static boolean trackChanged(String owner, String trackId, String fallbackKey) {
        if (trackId != null) {
            String prev = LAST_TRACK_ID_BY_OWNER.put(owner, trackId);
            if (fallbackKey != null) {
                LAST_FALLBACK_KEY_BY_OWNER.put(owner, fallbackKey);
            }
            return prev != null && !prev.equals(trackId);
        }
        if (fallbackKey == null) return false;
        String prev = LAST_FALLBACK_KEY_BY_OWNER.put(owner, fallbackKey);
        return prev != null && !prev.equals(fallbackKey);
    }

    /**
     * Advances the pending/nudge bookkeeping for one refresh and returns the
     * visible position for {@code state}, replaying a just-issued seek when the
     * player has not caught up. Nudges a freshly-started track back to zero.
     */
    static long resolveFramePosition(String owner, Player dbus, BaseState state, boolean trackChanged) {
        long positionRaw = state.positionRaw();
        if (trackChanged) {
            PENDING_TRACK_BY_OWNER.put(owner, true);
            clearSeekOverride(owner);
        }
        if (Boolean.TRUE.equals(PENDING_TRACK_BY_OWNER.get(owner)) && positionRaw <= 2L) {
            PENDING_TRACK_BY_OWNER.remove(owner);
        }
        if (trackChanged && state.trackId() != null && positionRaw > 5L) {
            String lastNudged = LAST_NUDGED_TRACK_ID_BY_OWNER.get(owner);
            if (!Objects.equals(lastNudged, state.trackId()) && LinuxMetadataReader.getCanSeek(owner)) {
                try {
                    String source = "org.mpris.MediaPlayer2." + owner;
                    dbus.SetPosition(new ObjectPath(source, state.trackId()), 0L);
                    positionRaw = 0L;
                    LAST_NUDGED_TRACK_ID_BY_OWNER.put(owner, state.trackId());
                    rememberSeekOverride(owner, state.trackId(), 0L, state.playing(), state.duration());
                } catch (Exception ignored) {
                }
            }
        }

        boolean pending = Boolean.TRUE.equals(PENDING_TRACK_BY_OWNER.get(owner));
        return resolveVisiblePosition(owner, state.trackId(), positionRaw, state.playing(), state.duration(), pending);
    }

    private record SeekOverrideState(String trackId,
                                     long targetPositionSeconds,
                                     long appliedAtMs,
                                     boolean playing,
                                     long duration) {
        long expectedPosition(long nowMs) {
            long elapsedSeconds = playing ? Math.max(0L, nowMs - appliedAtMs) / 1000L : 0L;
            long position = targetPositionSeconds + elapsedSeconds;
            return duration > 0L ? Math.min(position, duration) : position;
        }
    }
}
