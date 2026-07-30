/*
 * This file is part of the Combatant Client distribution.
 * Combatant modifications copyright (c) 2026 pivosos2007.
 *
 * This file belongs to Combatant's MediaPlayerInfo integration, based on
 * https://github.com/Redstonecrafter0/MediaPlayerInfo.
 * Copyright (c) Redstonecrafter0 and contributors.
 *
 * Licensed under the GNU Affero General Public License v3.0.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.util.media;

import dev.loki.lovisual.util.media.impl.DummyMediaPlayerInfo;
import dev.loki.lovisual.util.media.impl.linux.LinuxMediaPlayerInfo;
import dev.loki.lovisual.util.media.impl.win.WindowsMediaPlayerInfo;

import java.util.List;
import java.util.Locale;

/**
 * Modified Java port for Combatant by dev.loki.lovisual.
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
