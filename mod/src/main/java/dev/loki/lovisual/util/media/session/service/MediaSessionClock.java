package dev.loki.lovisual.util.media.session;

import net.minecraft.util.Util;
import dev.loki.lovisual.util.media.info.MediaInfo;

final class MediaSessionClock {

    private long lastRawPosition;
    private long lastRawDuration;
    private boolean lastRawPlaying;
    private long lastRawUpdateMs;

    long duration() {
        return lastRawDuration;
    }

    long getPredictedPositionMs(MediaInfo media) {
        if (media == null) {
            return 0L;
        }
        long now = Util.getMillis();
        long rawPos = Math.max(0L, media.position());
        long rawDur = Math.max(0L, media.duration());
        long[] normalized = normalizeTime(rawPos, rawDur);
        rawPos = normalized[0];
        rawDur = normalized[1];
        boolean rawPlaying = media.playing();
        boolean changed = rawPos != lastRawPosition
                || rawDur != lastRawDuration || rawPlaying != lastRawPlaying;
        if (changed) {
            lastRawPosition = rawPos;
            lastRawDuration = rawDur;
            lastRawPlaying = rawPlaying;
            lastRawUpdateMs = now;
        }
        long predictedMs = lastRawPosition * 1000L;
        if (lastRawPlaying) {
            predictedMs += Math.max(0L, now - lastRawUpdateMs);
        }
        if (lastRawDuration > 0L && predictedMs > lastRawDuration * 1000L) {
            predictedMs = lastRawDuration * 1000L;
        }
        return predictedMs;
    }

    void resetPredictedAfterSkip() {
        long now = Util.getMillis();
        lastRawPosition = 0L;
        lastRawDuration = 0L;
        lastRawPlaying = true;
        lastRawUpdateMs = now;
    }

    void applyLocalPlaybackState(boolean playing, MediaInfo media) {
        if (media == null) {
            return;
        }
        long now = Util.getMillis();
        long predicted = getPredictedPositionMs(media) / 1000L;
        long rawDur = Math.max(0L, media.duration());
        long[] normalized = normalizeTime(predicted, rawDur);
        lastRawPosition = normalized[0];
        lastRawDuration = normalized[1];
        lastRawPlaying = playing;
        lastRawUpdateMs = now;
    }

    void rememberLocalSeek(long positionSeconds, MediaInfo media) {
        long now = Util.getMillis();
        long rawDur = Math.max(0L, media.duration());
        long[] normalized = normalizeTime(Math.max(0L, positionSeconds), rawDur);
        lastRawPosition = normalized[0];
        lastRawDuration = normalized[1];
        lastRawPlaying = media != null && media.playing();
        lastRawUpdateMs = now;
    }

    void resetPredictedClock(MediaInfo media) {
        long now = Util.getMillis();
        if (media == null) {
            lastRawPosition = 0L;
            lastRawDuration = 0L;
            lastRawPlaying = false;
            lastRawUpdateMs = now;
            return;
        }
        long rawPos = Math.max(0L, media.position());
        long rawDur = Math.max(0L, media.duration());
        long[] normalized = normalizeTime(rawPos, rawDur);
        long pos = normalized[0];
        long dur = normalized[1];
        if (dur > 0L && pos > dur + 5L) {
            pos = 0L;
        }
        lastRawPosition = pos;
        lastRawDuration = dur;
        lastRawPlaying = media.playing();
        lastRawUpdateMs = now;
    }

    boolean shouldForceTrackReset(MediaInfo media) {
        if (media == null) {
            return false;
        }
        long rawPos = Math.max(0L, media.position());
        if (rawPos > lastRawPosition) {
            return false;
        }
        long backJump = lastRawPosition - rawPos;
        return rawPos <= 2L && backJump >= 8L;
    }

    long[] normalizeTime(long position, long duration) {
        long pos = Math.max(0L, position);
        long dur = Math.max(0L, duration);
        if (dur == 0L) {
            return new long[]{pos, 0L};
        }
        for (int i = 0; i < 6; i++) {
            if (pos > dur * 100L && pos >= 1000L) {
                pos /= 1000L;
                continue;
            }
            if (pos > 0L && dur > pos * 100L && dur >= 1000L) {
                dur /= 1000L;
                continue;
            }
            break;
        }
        return new long[]{pos, dur};
    }
}
