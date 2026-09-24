
package dev.loki.lovisual.util.media.session;

import java.util.Locale;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.util.media.info.MediaInfo;
import dev.loki.lovisual.util.media.info.RepeatMode;
public final class MediaSessionService {

    private static final MediaSessionService INSTANCE = new MediaSessionService();

    private final MediaSessionClock clock;
    private final MediaSessionArtwork artwork;
    private final MediaSessionTracker tracker;
    private final MediaSessionPoller poller;
    private Snapshot snapshot;
    private MediaSessionService() {
        MediaSessionClock createdClock = new MediaSessionClock();
        MediaSessionArtwork createdArtwork = new MediaSessionArtwork();
        MediaSessionTracker createdTracker = new MediaSessionTracker(createdClock, createdArtwork);
        MediaSessionPoller createdPoller = new MediaSessionPoller();
        this.clock = createdClock;
        this.artwork = createdArtwork;
        this.tracker = createdTracker;
        this.poller = createdPoller;
        this.snapshot = Snapshot.empty(isMediaAvailable());
    }

    public static MediaSessionService get() {
        return INSTANCE;
    }

    public static boolean isMediaAvailable() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.startsWith("windows") || os.equals("linux");
    }

    private static String safe(String value) {
        if (value == null) return "";
        String v = value.trim();
        return v.isEmpty() ? "" : v;
    }

    public synchronized void init() {
        poller.init();
    }

    public synchronized void shutdown() {
        poller.shutdown();
    }

    public synchronized Snapshot snapshot() {
        refresh();
        return snapshot;
    }

    public synchronized void onResourceReload() {
        tracker.clearOnResourceReload();
        snapshot = Snapshot.empty(isMediaAvailable());
    }

    public synchronized void playPause() {
        IMediaSession s = tracker.currentSession();
        if (s == null) return;
        MediaInfo m = tracker.resolveCurrentMedia();
        if (m == null) return;
        try {
            if (m.playing()) { clock.applyLocalPlaybackState(false, m); s.pause(); }
            else { clock.applyLocalPlaybackState(true, m); s.play(); }
        } catch (Throwable ignored) {}
    }

    public synchronized void play() {
        IMediaSession s = tracker.currentSession();
        if (s == null) return;
        MediaInfo m = tracker.resolveCurrentMedia();
        if (m == null) return;
        try { clock.applyLocalPlaybackState(true, m); s.play(); } catch (Throwable ignored) {}
    }

    public synchronized void pause() {
        IMediaSession s = tracker.currentSession();
        if (s == null) return;
        MediaInfo m = tracker.resolveCurrentMedia();
        if (m == null) return;
        try { clock.applyLocalPlaybackState(false, m); s.pause(); } catch (Throwable ignored) {}
    }

    public synchronized void stop() {
        IMediaSession s = tracker.currentSession();
        if (s == null) return;
        MediaInfo m = tracker.resolveCurrentMedia();
        if (m == null) return;
        try { clock.applyLocalPlaybackState(false, m); s.stop(); } catch (Throwable ignored) {}
    }

    public synchronized void previous() {
        IMediaSession s = tracker.currentSession();
        if (s == null) return;
        try { s.previous(); clock.resetPredictedAfterSkip(); } catch (Throwable ignored) {}
    }

    public synchronized void next() {
        IMediaSession s = tracker.currentSession();
        if (s == null) return;
        try { s.next(); clock.resetPredictedAfterSkip(); } catch (Throwable ignored) {}
    }

    public synchronized boolean supportsShuffle() {
        IMediaSession s = tracker.currentSession();
        return s != null && s.supportsShuffle();
    }

    public synchronized boolean isShuffleActive() {
        IMediaSession s = tracker.currentSession();
        return s != null && s.isShuffleActive();
    }

    public synchronized void setShuffle(boolean active) {
        IMediaSession s = tracker.currentSession();
        if (s == null || !s.supportsShuffle()) return;
        try { s.setShuffle(active); } catch (Throwable ignored) {}
    }

    public synchronized void toggleShuffle() {
        IMediaSession s = tracker.currentSession();
        if (s == null || !s.supportsShuffle()) return;
        try { s.setShuffle(!s.isShuffleActive()); } catch (Throwable ignored) {}
    }

    public synchronized boolean supportsRepeat() {
        IMediaSession s = tracker.currentSession();
        return s != null && s.supportsRepeat();
    }

    public synchronized RepeatMode getRepeatMode() {
        IMediaSession s = tracker.currentSession();
        if (s == null || !s.supportsRepeat()) return RepeatMode.UNKNOWN;
        try { return s.getRepeatMode(); } catch (Throwable ignored) { return RepeatMode.UNKNOWN; }
    }

    public synchronized void setRepeatMode(RepeatMode mode) {
        IMediaSession s = tracker.currentSession();
        if (s == null || mode == null || !s.supportsRepeat()) return;
        try { s.setRepeatMode(mode); } catch (Throwable ignored) {}
    }

    public synchronized RepeatMode cycleRepeatMode() {
        RepeatMode cur = getRepeatMode();
        RepeatMode next = switch (cur) { case OFF, UNKNOWN -> RepeatMode.ALL; case ALL -> RepeatMode.ONE; case ONE -> RepeatMode.OFF; };
        setRepeatMode(next); return next;
    }

    public synchronized boolean supportsSeek() {
        IMediaSession s = tracker.currentSession();
        return s != null && s.supportsSeek();
    }

    public synchronized void seekTo(long positionSeconds) {
        IMediaSession s = tracker.currentSession();
        if (s == null || !s.supportsSeek()) return;
        try { s.seekTo(positionSeconds); MediaInfo m = tracker.resolveCurrentMedia(); if (m != null) clock.rememberLocalSeek(positionSeconds, m); } catch (Throwable ignored) {}
    }

    private void refresh() {
        boolean mediaAvailable = isMediaAvailable();
        IMediaSession session = tracker.resolveSession(poller.sessions());
        MediaInfo media = tracker.currentMedia();
        if (session != null && media == null) {
            media = tracker.resolveCurrentMedia();
            tracker.setCurrentMedia(media);
        }
        boolean hasSession = session != null && media != null;
        long predictedMs = hasSession ? clock.getPredictedPositionMs(media) : 0L;
        long duration = hasSession ? clock.duration() : 0L;
        snapshot = new Snapshot(mediaAvailable, session, media, tracker.artworkTexture(), predictedMs, duration, hasSession);
    }

    public record Snapshot(boolean mediaAvailable, IMediaSession session, MediaInfo media, Identifier artworkTexture, long predictedPositionMs, long durationSeconds, boolean hasSession) {
        public static Snapshot empty(boolean mediaAvailable) { return new Snapshot(mediaAvailable, null, null, null, 0L, 0L, false); }
        public long predictedPositionSeconds() { return predictedPositionMs / 1000L; }
        public boolean isPlaying() { return media != null && media.playing(); }
        public String owner() { return session != null ? safe(session.getOwner()) : ""; }
        public String title() { return media != null ? safe(media.title()) : ""; }
        public String artist() { return media != null ? safe(media.artist()) : ""; }
        public boolean supportsShuffle() { return session != null && session.supportsShuffle(); }
        public boolean isShuffleActive() { return session != null && session.isShuffleActive(); }
        public boolean supportsRepeat() { return session != null && session.supportsRepeat(); }
        public RepeatMode repeatMode() {
            if (session == null || !session.supportsRepeat()) return RepeatMode.UNKNOWN;
            try { return session.getRepeatMode(); } catch (Throwable ignored) { return RepeatMode.UNKNOWN; }
        }
        public boolean supportsSeek() { return session != null && session.supportsSeek(); }
    }
}
