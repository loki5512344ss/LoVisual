package dev.loki.lovisual.util.media.impl.linux;

import org.freedesktop.dbus.ObjectPath;
import dev.loki.lovisual.util.media.impl.linux.LinuxMetadataReader.BaseState;
import dev.loki.lovisual.util.media.impl.linux.dbus.Player;
import dev.loki.lovisual.util.media.session.IMediaSession;
import dev.loki.lovisual.util.media.info.MediaInfo;
import dev.loki.lovisual.util.media.info.RepeatMode;

/**
 * Modified Java port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 *
 * <p>Thin MPR2 session facade: playback controls delegate straight to the D-Bus
 * {@link Player}; property reads live in {@link LinuxMetadataReader} and the
 * per-owner track/seek bookkeeping in {@link LinuxTrackState}.</p>
 */
public final class LinuxMediaSession implements IMediaSession {
    private final Player dbus;
    private final String owner;
    private final MediaInfo media;
    private String trackId;

    LinuxMediaSession(Player dbus, String owner) {
        this.dbus = dbus;
        this.owner = owner;
        this.media = generateMediaInfo();
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public MediaInfo getMedia() {
        return media;
    }

    @Override
    public void play() {
        dbus.Play();
    }

    @Override
    public void pause() {
        dbus.Pause();
    }

    @Override
    public void playPause() {
        dbus.PlayPause();
    }

    @Override
    public void stop() {
        dbus.Stop();
    }

    @Override
    public void next() {
        dbus.Next();
    }

    @Override
    public void previous() {
        dbus.Previous();
    }

    @Override
    public boolean supportsShuffle() {
        return LinuxMetadataReader.getShuffleValue(owner) != null;
    }

    @Override
    public boolean isShuffleActive() {
        Boolean value = LinuxMetadataReader.getShuffleValue(owner);
        return value != null && value;
    }

    @Override
    public void setShuffle(boolean active) {
        LinuxMediaPlayerInfo.INSTANCE.setProperty(owner, "Shuffle", active);
    }

    @Override
    public boolean supportsRepeat() {
        return LinuxMetadataReader.getLoopStatus(owner) != null;
    }

    @Override
    public RepeatMode getRepeatMode() {
        return RepeatMode.fromMpris(LinuxMetadataReader.getLoopStatus(owner));
    }

    @Override
    public void setRepeatMode(RepeatMode mode) {
        String value = switch (mode) {
            case OFF, UNKNOWN -> "None";
            case ONE -> "Track";
            case ALL -> "Playlist";
        };
        LinuxMediaPlayerInfo.INSTANCE.setProperty(owner, "LoopStatus", value);
    }

    @Override
    public boolean supportsSeek() {
        return LinuxMetadataReader.getCanSeek(owner);
    }

    @Override
    public void seekTo(long positionSeconds) {
        if (!LinuxMetadataReader.getCanSeek(owner)) return;
        long targetSeconds = Math.max(0L, positionSeconds);
        long targetUs = targetSeconds * 1_000_000L;
        BaseState state = LinuxMetadataReader.readBaseState(owner);
        String id = state.trackId() != null ? state.trackId() : trackId;
        trackId = id;

        try {
            if (id != null) {
                String source = "org.mpris.MediaPlayer2." + owner;
                dbus.SetPosition(new ObjectPath(source, id), targetUs);
                LinuxTrackState.rememberSeekOverride(owner, id, targetSeconds, state.playing(), state.duration());
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            long currentUs = Math.max(0L, LinuxTrackState.resolveVisiblePosition(
                    owner, state.trackId(), state.positionRaw(), state.playing(), state.duration(), false)) * 1_000_000L;
            dbus.Seek(targetUs - currentUs);
            LinuxTrackState.rememberSeekOverride(owner, id, targetSeconds, state.playing(), state.duration());
        } catch (Exception ignored) {
        }
    }

    private MediaInfo generateMediaInfo() {
        BaseState state = LinuxMetadataReader.readBaseState(owner);
        String fallbackKey = LinuxTrackState.buildFallbackKey(state.title(), state.artist(), state.duration());
        boolean trackChanged = LinuxTrackState.trackChanged(owner, state.trackId(), fallbackKey);
        long position = LinuxTrackState.resolveFramePosition(owner, dbus, state, trackChanged);
        trackId = state.trackId();
        byte[] artwork = LinuxMetadataReader.readArtworkBytes(state.metadata());
        return new MediaInfo(state.title(), state.artist(), artwork, position, state.duration(), state.playing());
    }
}
