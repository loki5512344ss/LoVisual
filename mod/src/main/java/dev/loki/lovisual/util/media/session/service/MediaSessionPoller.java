package dev.loki.lovisual.util.media.session;

import java.util.List;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.media.info.MediaPlayerInfo;
import dev.loki.lovisual.util.media.session.IMediaSession;

final class MediaSessionPoller {

    private static final long POLL_INTERVAL_MS = 300L;

    private final MediaPlayerInfo mediaPlayerInfo = MediaPlayerInfo.system();
    private volatile List<IMediaSession> mediaSessions = List.of();
    private volatile boolean pollingRunning;
    private Thread pollingThread;

    List<IMediaSession> sessions() {
        return mediaSessions;
    }

    synchronized void init() {
        if (pollingThread != null && pollingThread.isAlive()) {
            return;
        }
        pollingRunning = true;
        pollingThread = new Thread(this::pollMediaSessions, "LoVisualMediaInfoPollingThread");
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    synchronized void shutdown() {
        pollingRunning = false;
        Thread thread = pollingThread;
        if (thread == null) {
            return;
        }
        try {
            thread.join(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pollingThread = null;
        mediaSessions = List.of();
    }

    private void pollMediaSessions() {
        while (pollingRunning) {
            try {
                mediaSessions = List.copyOf(mediaPlayerInfo.getMediaSessions());
            } catch (Throwable t) {
                DebugLog.errorOnce("media-session-poll",
                        "[MediaSessionService] Failed to fetch media sessions", t);
                mediaSessions = List.of();
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pollingRunning = false;
            }
        }
    }
}
