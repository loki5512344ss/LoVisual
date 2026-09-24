package dev.loki.lovisual.features.module.modules.misc.protect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.features.module.modules.misc.protect.NoServerPack;

public final class NoServerPackTest {

    @Test
    void declinesInDeclineModeWhenEnabled() {
        assertTrue(NoServerPack.shouldDeclinePush(true, NoServerPack.Action.DECLINE.id));
    }

    @Test
    void declinesInRemoveModeWhenEnabled() {
        assertTrue(NoServerPack.shouldDeclinePush(true, NoServerPack.Action.REMOVE.id));
    }

    @Test
    void neverDeclinesInAskMode() {
        assertFalse(NoServerPack.shouldDeclinePush(true, NoServerPack.Action.ASK.id));
        assertFalse(NoServerPack.shouldDeclinePush(false, NoServerPack.Action.ASK.id));
    }

    @Test
    void disabledModuleNeverDeclines() {
        assertFalse(NoServerPack.shouldDeclinePush(false, NoServerPack.Action.DECLINE.id));
        assertFalse(NoServerPack.shouldDeclinePush(false, NoServerPack.Action.REMOVE.id));
    }

    @Test
    void unknownModeFallsBackToDecline() {
        assertEquals(NoServerPack.Action.DECLINE, NoServerPack.Action.byId("bogus"));
        assertEquals(NoServerPack.Action.DECLINE, NoServerPack.Action.byId(null));
    }

    @Test
    void modeIdsResolveRoundTrip() {
        for (NoServerPack.Action action : NoServerPack.Action.values()) {
            assertEquals(action, NoServerPack.Action.byId(action.id));
        }
    }
}
