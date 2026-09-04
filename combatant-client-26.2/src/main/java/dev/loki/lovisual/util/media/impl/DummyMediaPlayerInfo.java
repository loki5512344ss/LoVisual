/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.media.impl;

import dev.loki.lovisual.util.media.IMediaSession;
import dev.loki.lovisual.util.media.MediaPlayerInfo;

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
