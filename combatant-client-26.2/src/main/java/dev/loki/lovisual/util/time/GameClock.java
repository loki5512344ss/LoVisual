/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.time;

import net.minecraft.client.Minecraft;

/**
 * Игровое время (тики мира) в миллисекундах.
 * Вне мира (меню/загрузка) — откат на стенное время, чтобы не было NPE.
 */
public final class GameClock {

    public static final long TICK_MS = 50L;

    private GameClock() {
    }

    public static long millis() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            return mc.level.getGameTime() * TICK_MS;
        }
        return System.currentTimeMillis();
    }
}
