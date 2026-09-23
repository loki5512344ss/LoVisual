/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

final class PingFormat {

    private PingFormat() {
    }

    static String getPingValue(Ping host, int ping) {
        if (ping == host.lastPing) return host.cachedValue;
        host.lastPing = ping;
        host.cachedValue = ping >= 0 ? Integer.toString(ping) : "--";
        return host.cachedValue;
    }
}
