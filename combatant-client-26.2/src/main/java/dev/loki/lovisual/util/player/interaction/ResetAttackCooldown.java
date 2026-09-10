/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.interaction;

import net.minecraft.world.entity.player.Player;

public enum ResetAttackCooldown {
    ;

    public static void resetAttackCooldown(Player player) {
        player.resetOnlyAttackStrengthTicker();
    }
}

