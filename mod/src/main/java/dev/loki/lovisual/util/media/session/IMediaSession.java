package dev.loki.lovisual.util.media.session;
import dev.loki.lovisual.util.media.info.MediaInfo;
import dev.loki.lovisual.util.media.info.MediaPlayerInfo;
import dev.loki.lovisual.util.media.info.RepeatMode;

/**
 * Modified Java port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 */
public interface IMediaSession {
    String getOwner();

    MediaInfo getMedia();

    void play();

    void pause();

    void playPause();

    void stop();

    void next();

    void previous();

    default boolean supportsShuffle() {
        return false;
    }

    default boolean isShuffleActive() {
        return false;
    }

    default void setShuffle(boolean active) {
    }

    default boolean supportsRepeat() {
        return false;
    }

    default RepeatMode getRepeatMode() {
        return RepeatMode.OFF;
    }

    default void setRepeatMode(RepeatMode mode) {
    }

    default boolean supportsSeek() {
        return false;
    }

    default void seekTo(long positionSeconds) {
    }
}
