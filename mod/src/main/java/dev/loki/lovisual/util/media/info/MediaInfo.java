package dev.loki.lovisual.util.media.info;

import java.util.Arrays;
import java.util.Objects;
import dev.loki.lovisual.util.media.info.MediaPlayerInfo;

/**
 * Modified Java port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 */
public record MediaInfo(String title, String artist, byte[] artworkPng, long position, long duration, boolean playing) {
    private static final byte[] EMPTY_ARTWORK = new byte[0];

    public MediaInfo(String title, String artist, byte[] artworkPng, long position, long duration, boolean playing) {
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.artworkPng = artworkPng != null ? artworkPng : EMPTY_ARTWORK;
        this.position = position;
        this.duration = duration;
        this.playing = playing;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MediaInfo mediaInfo)) return false;
        return position == mediaInfo.position
                && duration == mediaInfo.duration
                && playing == mediaInfo.playing
                && Objects.equals(title, mediaInfo.title)
                && Objects.equals(artist, mediaInfo.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist, position, duration, playing);
    }

    @Override
    public String toString() {
        return "MediaInfo(title='" + title + "', artist='" + artist + "', position=" + position
                + ", duration=" + duration + ", playing=" + playing + ")";
    }

    public byte[] copyArtworkPng() {
        return Arrays.copyOf(artworkPng, artworkPng.length);
    }
}
