package dev.loki.lovisual.util.media.impl;

import dev.loki.lovisual.util.media.session.IMediaSession;
import dev.loki.lovisual.util.media.info.MediaPlayerInfo;

import java.util.List;

public final class DummyMediaPlayerInfo implements MediaPlayerInfo {
    public static final DummyMediaPlayerInfo INSTANCE = new DummyMediaPlayerInfo();

    private DummyMediaPlayerInfo() {
    }

    @Override
    public List<IMediaSession> getMediaSessions() {
        return List.of();
    }
}
