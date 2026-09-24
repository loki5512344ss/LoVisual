package dev.loki.lovisual.util.player.interaction;

import net.minecraft.world.entity.player.Player;

public enum ResetAttackCooldown {
    ;

    public static void resetAttackCooldown(Player player) {
        player.resetOnlyAttackStrengthTicker();
    }
}

