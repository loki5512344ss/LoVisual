package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import java.util.ArrayList;
import java.util.List;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLineSplitter;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source.LyricsFinder;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.mode.LyricModes;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import net.minecraft.client.Minecraft;

/**
 * Playback state machine for {@link KineticLyrics}: polls the active media session, tracks the
 * current song, keeps the raw / split lyric queues and the read index, decides which lines become
 * due as the audio clock advances and hands the actual text to {@link LyricStage} to spawn.
 * Handles track switches (async lyrics fetch), seek/rewind resyncs and the pause guard. Extracted
 * so the module class stays under the file-size cap.
 */
final class LyricPlayback {

    /** Прыжок вперёд больше этого — seek: переиндексировать очередь, не спавнить пропущенные строки залпом. */
    private static final long RESYNC_FORWARD_MS = 1500L;
    /** Регрессия больше этого — пауза/откат: строки после индекса уже не догнать, нужен resync. */
    private static final long RESYNC_BACKWARD_MS = 750L;

    private static final int MAX_RETRIES = 6;
    private static final long RETRY_STEP_MS = 8_000L;

    private final KineticLyrics owner;
    private final LyricStage stage;

    private final List<LyricLine> rawLyrics = new ArrayList<>();
    private final List<LyricLine> lyricsQueue = new ArrayList<>();
    private int currentLyricIndex = 0;

    private String lastTrackKey = "";
    private String currentTrackTitle = "";
    private String currentTrackArtist = "";
    private long lastAudioTimeMs = -1L;
    private boolean isPlaying = false;
    private boolean fetching = false;
    private int retries = 0;
    private long retryAtMs = 0L;

    LyricPlayback(KineticLyrics owner, LyricStage stage) {
        this.owner = owner;
        this.stage = stage;
    }

    String trackTitle() {
        return currentTrackTitle;
    }
    String trackArtist() {
        return currentTrackArtist;
    }
    int currentLyricIndex() {
        return currentLyricIndex;
    }
    int queueSize() {
        return lyricsQueue.size();
    }
    long lastAudioTimeMs() {
        return lastAudioTimeMs;
    }

    /** Clear all playback state: called on module enable / disable. */
    void reset() {
        stage.clear();
        rawLyrics.clear();
        lyricsQueue.clear();
        lastTrackKey = "";
        currentTrackTitle = "";
        currentTrackArtist = "";
        currentLyricIndex = 0;
        lastAudioTimeMs = -1L;
        isPlaying = false;
        fetching = false;
        retries = 0;
        retryAtMs = 0L;
    }

    /** Poll the media session once: switch tracks, resync on seek/pause and spawn due lines. */
    void sync() {
        MediaSessionService.Snapshot snapshot = MediaSessionService.get().snapshot();
        if (!snapshot.hasSession() || snapshot.title().isBlank()) {
            if (isPlaying) {
                isPlaying = false;
                stage.clear();
            }
            return;
        }

        String title = snapshot.title();
        String artist = snapshot.artist();
        String key = (title + " - " + artist).toLowerCase();

        if (!key.equalsIgnoreCase(lastTrackKey)) {
            loadTrack(title, artist, key, snapshot.durationSeconds());
            return;
        }

        if (lyricsQueue.isEmpty() && !fetching && retryAtMs > 0L && System.currentTimeMillis() >= retryAtMs) {
            retryAtMs = 0L;
            requestLyrics(title, artist, key, snapshot.durationSeconds());
        }

        if (!isPlaying && !lyricsQueue.isEmpty()) {
            isPlaying = true;
        }
        if (lyricsQueue.isEmpty()) return;

        // Позиция плеера (MPRIS/SMTC) — это внутренний таймер декодера: он
        // опережает слышимый вывод на латентность аудио-тракта (0.3-1с+, до
        // нескольких секунд через Bluetooth), а LRC-таймкоды привязаны к
        // слышимому звуку. Дефолтный сдвиг назад компенсирует это (решение
        // совпадает с lrcsnc: у автора ~0.75с задержки для mpd).
        long audioTimeMs = Math.max(0L, snapshot.predictedPositionMs() + owner.timeOffsetMs.get());
        resyncIfNeeded(audioTimeMs);
        lastAudioTimeMs = audioTimeMs;

        advance(audioTimeMs);
    }

    // Любая ощутимая регрессия (pause через truncate-в-секунды MPRIS, короткий seek) —
    // иначе индекс остаётся впереди звука и строки молча скипаются до догонки.
    private void resyncIfNeeded(long audioTimeMs) {
        boolean backward = lastAudioTimeMs >= 0 && audioTimeMs < lastAudioTimeMs - RESYNC_BACKWARD_MS;
        boolean forward = lastAudioTimeMs >= 0 && audioTimeMs - lastAudioTimeMs > RESYNC_FORWARD_MS;
        if (backward || forward) {
            resync(audioTimeMs);
        }
    }

    private void loadTrack(String title, String artist, String key, long durationSeconds) {
        // reset() forgets the track key, so it has to run before the new track is recorded
        reset();
        this.currentTrackTitle = title;
        this.currentTrackArtist = artist;
        this.lastTrackKey = key;
        requestLyrics(title, artist, key, durationSeconds);
    }

    private void requestLyrics(String title, String artist, String key, long durationSeconds) {
        fetching = true;
        LyricsFinder.findAsync(title, artist, durationSeconds, owner.debugMode.get()).thenAccept(result ->
                Minecraft.getInstance().execute(() -> {
                    if (!this.lastTrackKey.equalsIgnoreCase(key)) return;
                    fetching = false;

                    if (result.lines().isEmpty()) {
                        scheduleRetry(result.retryLater());
                        return;
                    }
                    stage.clear();
                    rawLyrics.clear();
                    lyricsQueue.clear();
                    rawLyrics.addAll(result.lines());
                    rebuildLyricsQueue();
                    currentLyricIndex = 0;
                    isPlaying = !lyricsQueue.isEmpty();
                })
        );
    }

    /** A busy service is asked again after a growing pause, a few times; a song nobody has is left alone. */
    private void scheduleRetry(boolean retryLater) {
        if (!retryLater || retries >= MAX_RETRIES) return;
        retries++;
        retryAtMs = System.currentTimeMillis() + RETRY_STEP_MS * retries;
    }

    private void rebuildLyricsQueue() {
        lyricsQueue.clear();
        if (owner.splitMode.get() == LyricModes.SplitMode.SMART_SPLIT) {
            lyricsQueue.addAll(LyricLineSplitter.splitLongLines(rawLyrics));
        } else {
            lyricsQueue.addAll(rawLyrics);
        }
    }

    private void resync(long audioTimeMs) {
        stage.clear();
        currentLyricIndex = 0;
        long now = System.currentTimeMillis();

        for (int i = 0; i < lyricsQueue.size(); i++) {
            LyricLine line = lyricsQueue.get(i);
            long nextStartMs = (i + 1 < lyricsQueue.size()) ? lyricsQueue.get(i + 1).timestampMs() : line.timestampMs() + 3500L;
            long durationMs = Math.max(1200L, Math.min(5000L, nextStartMs - line.timestampMs()));

            if (audioTimeMs >= line.timestampMs()) {
                long timeSinceLine = audioTimeMs - line.timestampMs();
                if (timeSinceLine < durationMs) {
                    stage.spawn(line.text(), now - timeSinceLine, durationMs);
                }
                currentLyricIndex = i + 1;
            } else {
                break;
            }
        }
    }

    private void advance(long audioTimeMs) {
        Minecraft mc = Minecraft.getInstance();
        if (!isPlaying || lyricsQueue.isEmpty() || mc.player == null) return;

        long now = System.currentTimeMillis();

        while (currentLyricIndex < lyricsQueue.size()) {
            LyricLine line = lyricsQueue.get(currentLyricIndex);
            long nextStartMs = (currentLyricIndex + 1 < lyricsQueue.size()) ? lyricsQueue.get(currentLyricIndex + 1).timestampMs() : line.timestampMs() + 3500L;
            long durationMs = Math.max(1400L, Math.min(5500L, nextStartMs - line.timestampMs()));

            if (audioTimeMs >= line.timestampMs()) {
                long timeSinceLine = audioTimeMs - line.timestampMs();
                if (timeSinceLine < durationMs) {
                    stage.spawn(line.text(), now - timeSinceLine, durationMs);
                }
                currentLyricIndex++;
            } else {
                break;
            }
        }

        stage.pruneDead(now);
    }
}
