/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.pvp.client;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.pvp.PvpCooldowns;
import dev.loki.lovisual.util.pvp.client.CooldownManager;
import dev.loki.lovisual.util.pvp.client.PendingUseTracker;

/**
 * Глобальное хранилище кулдаунов и флагов PvP.
 */
public enum CooldownsState {
    ;
    public static final CooldownManager MANAGER = new CooldownManager();
    public static final PendingUseTracker PENDING = new PendingUseTracker();
    public static final long OPPONENT_GRACE_MS = 10_000L;

    public static boolean shouldTrackOpponents() {
        var mod = Modules.get(
                PvpCooldowns.class
        );
        if (mod == null || !mod.isSystemEnabled()) return false;
        return MANAGER.isPvpGraceActive(OPPONENT_GRACE_MS);
    }
}
