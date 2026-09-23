/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.media.info;

import dev.loki.lovisual.util.media.impl.DummyMediaPlayerInfo;
import dev.loki.lovisual.util.media.impl.linux.LinuxMediaPlayerInfo;
import dev.loki.lovisual.util.media.impl.win.WindowsMediaPlayerInfo;

import java.util.List;
import java.util.Locale;
import dev.loki.lovisual.util.media.session.IMediaSession;

/**
 * Modified Java port for LoVisual by dev.loki.lovisual.
 * Based on Redstonecrafter0/MediaPlayerInfo.
 */
public interface MediaPlayerInfo {
    static MediaPlayerInfo system() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            try {
                return WindowsMediaPlayerInfo.INSTANCE;
            } catch (Throwable ignored) {
                return DummyMediaPlayerInfo.INSTANCE;
            }
        }
        if (os.equals("linux")) {
            try {
                return LinuxMediaPlayerInfo.INSTANCE;
            } catch (Throwable ignored) {
                return DummyMediaPlayerInfo.INSTANCE;
            }
        }
        return DummyMediaPlayerInfo.INSTANCE;
    }

    List<IMediaSession> getMediaSessions();
}
