package dev.loki.lovisual.util.protection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SlotLockPolicyTest {

    private static Map<String, Boolean> states(int... lockedZeroBasedSlots) {
        Map<String, Boolean> out = SlotLockPolicy.defaultSlotStates();
        for (int slot : lockedZeroBasedSlots) {
            out.put(SlotLockPolicy.slotKey(slot), true);
        }
        return out;
    }

    @Test
    void defaultsUnlockAllNineSlots() {
        Map<String, Boolean> defaults = SlotLockPolicy.defaultSlotStates();
        assertEquals(9, defaults.size());
        for (int slot = 0; slot < 9; slot++) {
            assertFalse(SlotLockPolicy.isSlotLocked(defaults, slot), "slot " + slot + " must be unlocked");
        }
    }

    @Test
    void slotKeyIsOneBased() {
        assertEquals("slot1", SlotLockPolicy.slotKey(0));
        assertEquals("slot5", SlotLockPolicy.slotKey(4));
        assertEquals("slot9", SlotLockPolicy.slotKey(8));
    }

    @Test
    void lockedSlotIsBlockedWhenNearbyCheckDisabled() {
        assertTrue(SlotLockPolicy.isDropBlocked(states(4), 4, false, false));
        assertTrue(SlotLockPolicy.isDropBlocked(states(4), 4, true, false));
    }

    @Test
    void lockedSlotRespectsOnlyPlayersNearby() {
        assertTrue(SlotLockPolicy.isDropBlocked(states(0), 0, true, true));
        assertFalse(SlotLockPolicy.isDropBlocked(states(0), 0, false, true));
    }

    @Test
    void unlockedSlotIsNeverBlocked() {
        assertFalse(SlotLockPolicy.isDropBlocked(states(1, 2), 0, true, false));
        assertFalse(SlotLockPolicy.isDropBlocked(states(1, 2), 0, false, false));
    }

    @Test
    void hotbarEdgesAreCovered() {
        Map<String, Boolean> edgeStates = states(0, 8);
        assertTrue(SlotLockPolicy.isDropBlocked(edgeStates, 0, false, false));
        assertTrue(SlotLockPolicy.isDropBlocked(edgeStates, 8, false, false));
        assertFalse(SlotLockPolicy.isDropBlocked(edgeStates, 4, false, false));
    }

    @Test
    void outOfRangeSlotsAreNeverBlocked() {
        Map<String, Boolean> all = states(0, 1, 2, 3, 4, 5, 6, 7, 8);
        assertFalse(SlotLockPolicy.isDropBlocked(all, -1, true, false));
        assertFalse(SlotLockPolicy.isDropBlocked(all, 9, true, false));
        assertFalse(SlotLockPolicy.isSlotLocked(all, -1));
        assertFalse(SlotLockPolicy.isSlotLocked(all, 9));
    }

    @Test
    void missingOrNullStatesAreNeverBlocked() {
        assertFalse(SlotLockPolicy.isDropBlocked(null, 0, true, false));
        assertFalse(SlotLockPolicy.isDropBlocked(new HashMap<>(), 0, true, false));
        assertFalse(SlotLockPolicy.isSlotLocked(null, 0));
    }

    @Test
    void unknownOrNonBooleanEntriesAreIgnored() {
        Map<String, Boolean> odd = new HashMap<>();
        odd.put("slot", true);
        odd.put("slot10", true);
        odd.put("slot0", true);
        assertFalse(SlotLockPolicy.isSlotLocked(odd, 0));
        assertFalse(SlotLockPolicy.isDropBlocked(odd, 0, true, false));
    }

    @Test
    void nearbyCheckDoesNotAffectUnlockedSlots() {
        assertFalse(SlotLockPolicy.isDropBlocked(states(8), 3, true, true));
    }
}
