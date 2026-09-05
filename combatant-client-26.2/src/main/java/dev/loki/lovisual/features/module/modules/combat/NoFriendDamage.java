/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat;

import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.Notifier;
import dev.loki.lovisual.features.relations.CategoryService;
import dev.loki.lovisual.util.protection.NotifyThrottle;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Cancels attacks against players marked as friends (including the
 * Bedwars self team) and shows a throttled warning instead.
 */
@ModuleInfo(
        id = "nofrienddamage",
        displayName = "No Friend Damage",
        aliases = {"nofrienddamage", "nfd"},
        category = ModuleCategory.COMBAT,
        description = "module.nofrienddamage.description"
)
public final class NoFriendDamage extends Module {

    private static final long WARN_INTERVAL_MS = 2000L;

    private final NotifyThrottle warnThrottle = new NotifyThrottle(WARN_INTERVAL_MS);

    /**
     * Called from the client attack hook (MultiPlayerGameMode.attack).
     *
     * @return true when the attack must be cancelled
     */
    public boolean shouldBlockAttack(Entity target) {
        if (!isEnabled() || !(target instanceof Player targetPlayer)) return false;
        if (!CategoryService.isFriend(targetPlayer)) return false;
        notifyBlocked(targetPlayer);
        return true;
    }

    private void notifyBlocked(Player targetPlayer) {
        if (!warnThrottle.tryAcquire(Util.getMillis())) return;
        Notifier.warning("Blocked attack on friend: " + targetPlayer.getGameProfile().name());
    }
}
