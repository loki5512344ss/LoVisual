/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player.inventory;

import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.util.protection.NotifyThrottle;
import dev.loki.lovisual.util.protection.SlotLockPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

/**
 * Protects hotbar slots 1-9 from accidental drops.
 * Covers the Q drop of the selected hotbar slot and THROW clicks on the
 * player's hotbar slots inside container screens.
 */
@ModuleInfo(
        id = "lockslot",
        displayName = "Lock Slot",
        aliases = {"lockslot"},
        category = ModuleCategory.PLAYER,
        description = "module.lockslot.description"
)
public final class LockSlot extends Module {

    private static final double NEARBY_RADIUS_SQR = 32.0 * 32.0;
    private static final long WARN_INTERVAL_MS = 1500L;

    private final BooleanMapValue slots = group("lockslot_slots", "slots", SlotLockPolicy.defaultSlotStates());
    private final BooleanValue onlyPlayersNearby = bool("lockslot_only_players_nearby", "only_players_nearby", true);

    private final NotifyThrottle warnThrottle = new NotifyThrottle(WARN_INTERVAL_MS);

    /**
     * Path A: Q / Ctrl+Q drop of the selected hotbar slot (LocalPlayer.drop).
     */
    public boolean shouldBlockSelectedDrop() {
        if (!isEnabled()) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        return shouldBlockDrop(player, player.getInventory().getSelectedSlot());
    }

    /**
     * Path B: THROW container clicks targeting a hotbar slot in an open screen.
     */
    public boolean shouldBlockContainerThrow(Player player, int containerId, int slotId) {
        if (!isEnabled() || player == null) return false;
        if (player.containerMenu == null || player.containerMenu.containerId != containerId) return false;
        if (slotId < 0 || slotId >= player.containerMenu.slots.size()) return false;
        Slot slot = player.containerMenu.slots.get(slotId);
        if (slot.container != player.getInventory()) return false;
        return shouldBlockDrop(player, slot.getContainerSlot());
    }

    private boolean shouldBlockDrop(Player player, int hotbarSlot) {
        boolean playersNearby = hasPlayersNearby(player);
        if (!SlotLockPolicy.isDropBlocked(slots.getAll(), hotbarSlot, playersNearby, onlyPlayersNearby.get())) {
            return false;
        }
        notifyBlocked(hotbarSlot);
        return true;
    }

    private boolean hasPlayersNearby(Player self) {
        Level level = self.level();
        if (level == null) return false;
        for (Player other : level.players()) {
            if (other == self || other.isSpectator()) continue;
            if (other.distanceToSqr(self) <= NEARBY_RADIUS_SQR) return true;
        }
        return false;
    }

    private void notifyBlocked(int hotbarSlot) {
        if (!warnThrottle.tryAcquire(Util.getMillis())) return;
        Notifier.warning("Slot " + (hotbarSlot + 1) + " is locked: drop blocked");
    }
}
