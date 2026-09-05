/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.protection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure decision logic for the LockSlot module: whether dropping an item
 * from a given hotbar slot must be blocked.
 */
public final class SlotLockPolicy {

    public static final int HOTBAR_SIZE = 9;

    private SlotLockPolicy() {
    }

    /**
     * Default setting state: all hotbar slots unlocked.
     * Keys are ordered "slot1".."slot9".
     */
    public static Map<String, Boolean> defaultSlotStates() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            out.put(slotKey(i), false);
        }
        return out;
    }

    /**
     * Setting key for a zero-based hotbar slot: 0 -> "slot1", 8 -> "slot9".
     */
    public static String slotKey(int hotbarSlot) {
        return "slot" + (hotbarSlot + 1);
    }

    /**
     * True when the slot is explicitly locked by the given states map.
     * Missing or out-of-range slots are treated as unlocked.
     */
    public static boolean isSlotLocked(Map<String, Boolean> slotStates, int hotbarSlot) {
        if (slotStates == null || slotStates.isEmpty()) return false;
        if (hotbarSlot < 0 || hotbarSlot >= HOTBAR_SIZE) return false;
        Boolean locked = slotStates.get(slotKey(hotbarSlot));
        return locked != null && locked;
    }

    /**
     * Full drop decision for a hotbar slot.
     *
     * @param slotStates    locked-slot setting states keyed by {@link #slotKey(int)}
     * @param selectedSlot  zero-based hotbar slot the drop comes from
     * @param playersNearby whether other players are within the guard radius
     * @param onlyNearby    the "only_players_nearby" setting value
     */
    public static boolean isDropBlocked(
            Map<String, Boolean> slotStates,
            int selectedSlot,
            boolean playersNearby,
            boolean onlyNearby
    ) {
        if (!isSlotLocked(slotStates, selectedSlot)) return false;
        return !onlyNearby || playersNearby;
    }
}
