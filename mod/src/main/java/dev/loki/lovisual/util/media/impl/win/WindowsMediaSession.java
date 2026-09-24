package dev.loki.lovisual.util.media.impl.win;

import dev.loki.lovisual.util.media.session.IMediaSession;
import dev.loki.lovisual.util.media.info.MediaInfo;
import dev.loki.lovisual.util.media.info.RepeatMode;
import dev.loki.lovisual.util.media.info.MediaPlayerInfo;

/**
 * Modified Java/native port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 */
public final class WindowsMediaSession implements IMediaSession {
    private final MediaInfo media;
    private final String owner;
    private final String sessionId;
    private final boolean supportsShuffleSnapshot;
    private final boolean shuffleActiveSnapshot;
    private final boolean supportsRepeatSnapshot;
    private final int repeatModeSnapshot;
    private final boolean supportsSeekSnapshot;

    public WindowsMediaSession(MediaInfo media,
                               String owner,
                               String sessionId,
                               boolean supportsShuffleSnapshot,
                               boolean shuffleActiveSnapshot,
                               boolean supportsRepeatSnapshot,
                               int repeatModeSnapshot,
                               boolean supportsSeekSnapshot) {
        this.media = media;
        this.owner = owner;
        this.sessionId = sessionId;
        this.supportsShuffleSnapshot = supportsShuffleSnapshot;
        this.shuffleActiveSnapshot = shuffleActiveSnapshot;
        this.supportsRepeatSnapshot = supportsRepeatSnapshot;
        this.repeatModeSnapshot = repeatModeSnapshot;
        this.supportsSeekSnapshot = supportsSeekSnapshot;
    }

    @Override
    public MediaInfo getMedia() {
        return media;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public native void play();

    @Override
    public native void pause();

    @Override
    public native void playPause();

    @Override
    public native void stop();

    @Override
    public native void next();

    @Override
    public native void previous();

    private native boolean nativeSupportsShuffle();

    private native boolean nativeIsShuffleActive();

    private native void nativeSetShuffle(boolean active);

    private native boolean nativeSupportsRepeat();

    private native int nativeGetRepeatMode();

    private native void nativeSetRepeatMode(int mode);

    private native boolean nativeSupportsSeek();

    private native void nativeSeekTo(long positionSeconds);

    @Override
    public boolean supportsShuffle() {
        return supportsShuffleSnapshot;
    }

    @Override
    public boolean isShuffleActive() {
        return shuffleActiveSnapshot;
    }

    @Override
    public void setShuffle(boolean active) {
        nativeSetShuffle(active);
    }

    @Override
    public boolean supportsRepeat() {
        return supportsRepeatSnapshot;
    }

    @Override
    public RepeatMode getRepeatMode() {
        return switch (repeatModeSnapshot) {
            case 1 -> RepeatMode.ONE;
            case 2 -> RepeatMode.ALL;
            case 0 -> RepeatMode.OFF;
            default -> RepeatMode.UNKNOWN;
        };
    }

    @Override
    public void setRepeatMode(RepeatMode mode) {
        int value = switch (mode) {
            case ONE -> 1;
            case ALL -> 2;
            case OFF, UNKNOWN -> 0;
        };
        nativeSetRepeatMode(value);
    }

    @Override
    public boolean supportsSeek() {
        return supportsSeekSnapshot;
    }

    @Override
    public void seekTo(long positionSeconds) {
        nativeSeekTo(positionSeconds);
    }
}
