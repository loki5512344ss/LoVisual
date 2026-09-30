package dev.loki.lovisual.features.platform.serverpolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate the whole mod obeys: {@link ServerPolicyState#allows} is consulted
 * by module enablement, ClickGui locks and the suggest-warning path. State is
 * static, so every test resets it in {@code @BeforeEach}.
 */
public final class ServerPolicyStateTest {

    private static ServerPolicy lockPolicy() {
        return new ServerPolicy(ServerPolicy.Mode.LOCK, "Правила сервера",
                List.of("esp", "freecam"), "lobby-1");
    }

    @BeforeEach
    void reset() {
        ServerPolicyState.clear();
    }

    @Test
    void startsUnlockedWithNoPolicy() {
        assertNull(ServerPolicyState.current());
        assertTrue(ServerPolicyState.allows("esp"));
        assertFalse(ServerPolicyState.isLocked("esp"));
        assertFalse(ServerPolicyState.suggestWarn("esp"));
        assertEquals("", ServerPolicyState.reason());
        assertEquals(0L, ServerPolicyState.receivedAtMs());
    }

    @Test
    void lockPolicyBlocksOnlyDeniedModules() {
        ServerPolicyState.apply(lockPolicy());

        assertFalse(ServerPolicyState.allows("esp"));
        assertFalse(ServerPolicyState.allows("freecam"));
        assertTrue(ServerPolicyState.isLocked("esp"));
        assertTrue(ServerPolicyState.allows("hitboxes"));
        assertFalse(ServerPolicyState.isLocked("hitboxes"));
        assertEquals("Правила сервера", ServerPolicyState.reason());
        assertEquals("lobby-1", ServerPolicyState.current().server());
        assertTrue(ServerPolicyState.receivedAtMs() > 0L);
    }

    @Test
    void suggestPolicyWarnsButStillAllows() {
        ServerPolicyState.apply(new ServerPolicy(ServerPolicy.Mode.SUGGEST, "рекомендация",
                List.of("esp"), ""));

        assertTrue(ServerPolicyState.allows("esp"));
        assertFalse(ServerPolicyState.isLocked("esp"));
        assertTrue(ServerPolicyState.suggestWarn("esp"));
        assertFalse(ServerPolicyState.suggestWarn("freecam"));
    }

    @Test
    void applyingNewPolicyReplacesTheOldOne() {
        ServerPolicyState.apply(lockPolicy());
        ServerPolicyState.apply(new ServerPolicy(ServerPolicy.Mode.SUGGEST, "новая", List.of(), "bedwars"));

        assertTrue(ServerPolicyState.allows("esp"));
        assertEquals("новая", ServerPolicyState.reason());
        assertEquals("bedwars", ServerPolicyState.current().server());
    }

    @Test
    void clearRestoresFullAccess() {
        ServerPolicyState.apply(lockPolicy());
        ServerPolicyState.clear();

        assertTrue(ServerPolicyState.allows("esp"));
        assertFalse(ServerPolicyState.isLocked("esp"));
        assertNull(ServerPolicyState.current());
        assertEquals(0L, ServerPolicyState.receivedAtMs());
    }
}
