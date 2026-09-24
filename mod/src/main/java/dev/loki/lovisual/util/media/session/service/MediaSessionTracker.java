package dev.loki.lovisual.util.media.session;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import dev.loki.lovisual.util.logging.DebugLog;
import java.util.List;
import dev.loki.lovisual.util.media.info.MediaInfo;
import dev.loki.lovisual.util.media.session.IMediaSession;

final class MediaSessionTracker {

    private static final long SESSION_GRACE_MS = 1500L;

    private final MediaSessionClock clock;
    private final MediaSessionArtwork artwork;
    private IMediaSession currentSession;
    private MediaInfo currentMedia;
    private String lastTrackKey;
    private long lastSessionSeenMs;
    private long lastDebugLogMs;
    private String lastDebugLogKey;

    MediaSessionTracker(MediaSessionClock clock, MediaSessionArtwork artwork) {
        this.clock = clock;
        this.artwork = artwork;
    }

    IMediaSession currentSession() {
        return currentSession;
    }

    MediaInfo currentMedia() {
        return currentMedia;
    }

    Identifier artworkTexture() {
        return artwork.texture();
    }

    void setCurrentMedia(MediaInfo media) {
        currentMedia = media;
    }

    MediaInfo resolveCurrentMedia() {
        if (currentSession == null) {
            return null;
        }
        try {
            return currentSession.getMedia();
        } catch (Throwable ignored) {
            return currentMedia;
        }
    }

    IMediaSession resolveSession(List<IMediaSession> sessions) {
        long now = Util.getMillis();
        if (sessions.isEmpty()) {
            if (currentSession != null && now - lastSessionSeenMs < SESSION_GRACE_MS) {
                return currentSession;
            }
            lastTrackKey = null;
            artwork.clear();
            currentSession = null;
            currentMedia = null;
            return null;
        }
        IMediaSession best = null;
        MediaInfo bestMedia = null;
        for (IMediaSession session : sessions) {
            if (session == null) {
                continue;
            }
            MediaInfo media;
            try {
                media = session.getMedia();
            } catch (Throwable ignored) {
                continue;
            }
            if (best == null) {
                best = session;
                bestMedia = media;
            }
            if (media.playing()) {
                best = session;
                bestMedia = media;
                break;
            }
        }
        if (best == null) {
            return null;
        }
        currentSession = best;
        lastSessionSeenMs = now;
        currentMedia = bestMedia;
        if (bestMedia == null) {
            return best;
        }
        String key = buildTrackKey(best, bestMedia);
        boolean keyChanged = !key.equals(lastTrackKey);
        boolean forceReset = clock.shouldForceTrackReset(bestMedia);
        if (keyChanged || forceReset) {
            clock.resetPredictedClock(bestMedia);
            if (keyChanged) {
                lastTrackKey = key;
            }
        }
        artwork.maybeUpdate(bestMedia, keyChanged || forceReset);
        debugLogMedia(best, bestMedia);
        return best;
    }

    void clearOnResourceReload() {
        artwork.clear();
        lastTrackKey = null;
        currentSession = null;
        currentMedia = null;
        lastSessionSeenMs = 0L;
        lastDebugLogKey = null;
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        return v.isEmpty() ? "" : v;
    }

    private String buildTrackKey(IMediaSession session, MediaInfo media) {
        String owner = session != null ? safe(session.getOwner()) : "";
        String title = media != null ? safe(media.title()) : "";
        String artist = media != null ? safe(media.artist()) : "";
        String durationKey = "";
        if (media != null) {
            long dur = Math.max(0L, media.duration());
            if (dur > 0L) {
                durationKey = Long.toString(dur);
            }
        }
        return owner + "\u0000" + title + "\u0000" + artist + "\u0000" + durationKey;
    }

    private void debugLogMedia(IMediaSession session, MediaInfo media) {
        if (!DebugLog.isEnabled()) {
            return;
        }
        long now = Util.getMillis();
        if (now - lastDebugLogMs < 1000L) {
            return;
        }
        lastDebugLogMs = now;
        String owner = session != null ? safe(session.getOwner()) : "";
        String title = media != null ? safe(media.title()) : "";
        String artist = media != null ? safe(media.artist()) : "";
        long pos = media != null ? media.position() : -1L;
        long dur = media != null ? media.duration() : -1L;
        boolean playing = media != null && media.playing();
        String debugKey = owner + "\u0000" + title + "\u0000" + artist
                + "\u0000" + dur + "\u0000" + playing;
        if (debugKey.equals(lastDebugLogKey)) {
            return;
        }
        lastDebugLogKey = debugKey;
        DebugLog.info(
                "[MediaSessionService] owner=%s title=%s artist=%s pos=%d dur=%d playing=%s",
                owner, title, artist, pos, dur, playing);
    }
}
