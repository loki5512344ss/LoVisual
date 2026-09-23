/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

final class MemoryFormat {

    private MemoryFormat() {
    }

    static long toMiB(long bytes) {
        return bytes / 1024L / 1024L;
    }

    static void updateMemoryStrings(Memory host, long usedMiB, long maxMiB, int percent) {
        if (usedMiB == host.lastUsedMiB && maxMiB == host.lastMaxMiB && percent == host.lastPercent) {
            return;
        }
        host.lastUsedMiB = usedMiB;
        host.lastMaxMiB = maxMiB;
        host.lastPercent = percent;

        host.valueBuilder.setLength(0);
        host.valueBuilder.append(usedMiB).append('/').append(maxMiB);
        host.cachedValue = host.valueBuilder.toString();
        host.cachedExtra = "[" + percent + "%]";
    }
}
