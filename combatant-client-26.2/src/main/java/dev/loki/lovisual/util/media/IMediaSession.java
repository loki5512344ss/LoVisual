/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.media;

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
