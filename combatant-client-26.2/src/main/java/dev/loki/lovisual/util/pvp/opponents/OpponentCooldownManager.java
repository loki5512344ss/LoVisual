/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.pvp.opponents;

import net.minecraft.world.item.Item;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.util.pvp.cooldown.ItemCooldownSnapshot;
import dev.loki.lovisual.util.pvp.cooldown.ItemUseCooldowns;

import java.util.Map;
import java.util.UUID;

/**
 * Opponent-facing facade for the generic owner-based item-use cooldown engine.
 * <p>
 * Kept for old callers and readability in opponent systems, but the state is no longer
 * separate from the local cooldown implementation.
 */
public enum OpponentCooldownManager {
    ;

    public static void start(UUID playerId, Item item) {
        recordUse(playerId, item);
    }

    public static void recordUse(UUID playerId, Item item) {
        ItemUseCooldowns.recordUse(playerId, item);
    }

    public static void recordUse(UUID playerId, Item item, ItemCooldownRulesValue.Rule rule) {
        ItemUseCooldowns.recordUse(playerId, item, rule);
    }

    public static boolean isCooling(UUID playerId, Item item) {
        return ItemUseCooldowns.isCooling(playerId, item);
    }

    public static float getProgress(UUID playerId, Item item) {
        return snapshot(playerId, item).cooldownProgress();
    }

    public static ItemCooldownSnapshot snapshot(UUID playerId, Item item) {
        return ItemUseCooldowns.snapshot(playerId, item);
    }

    public static Map<Item, ItemCooldownSnapshot> snapshots(UUID playerId) {
        return ItemUseCooldowns.snapshots(playerId);
    }

    public static void clear(UUID playerId) {
        ItemUseCooldowns.clear(playerId);
    }
}
