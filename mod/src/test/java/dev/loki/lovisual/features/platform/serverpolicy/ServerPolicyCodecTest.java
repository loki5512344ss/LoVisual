package dev.loki.lovisual.features.platform.serverpolicy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract of the S→C half of the {@code lovisual:policy} channel: the proxy
 * pushes one JSON object, everything else (garbage, non-policy messages) must
 * degrade to {@code null} so a broken payload never locks the client.
 */
public final class ServerPolicyCodecTest {

    @Test
    void parsesFullPolicy() {
        ServerPolicy policy = ServerPolicyCodec.parse("""
                {"type":"policy","mode":"lock","reason":"Правила сервера",
                 "deny":["esp","freecam"],"server":"lobby-1"}""");
        assertNotNull(policy);
        assertEquals(ServerPolicy.Mode.LOCK, policy.mode());
        assertTrue(policy.isLocked());
        assertEquals("Правила сервера", policy.reason());
        assertEquals(List.of("esp", "freecam"), policy.deny());
        assertEquals("lobby-1", policy.server());
        assertTrue(policy.denies("esp"));
        assertFalse(policy.denies("hitboxes"));
    }

    @Test
    void suggestModeParses() {
        ServerPolicy policy = ServerPolicyCodec.parse(
                "{\"type\":\"policy\",\"mode\":\"suggest\",\"reason\":\"только подсказка\",\"deny\":[\"esp\"]}");
        assertNotNull(policy);
        assertEquals(ServerPolicy.Mode.SUGGEST, policy.mode());
        assertFalse(policy.isLocked());
    }

    @Test
    void unknownOrMissingModeFallsBackToLock() {
        ServerPolicy unknown = ServerPolicyCodec.parse(
                "{\"type\":\"policy\",\"mode\":\"banana\",\"deny\":[\"esp\"]}");
        assertNotNull(unknown);
        assertEquals(ServerPolicy.Mode.LOCK, unknown.mode());

        ServerPolicy missing = ServerPolicyCodec.parse("{\"type\":\"policy\",\"deny\":[\"esp\"]}");
        assertNotNull(missing);
        assertEquals(ServerPolicy.Mode.LOCK, missing.mode());
    }

    @Test
    void missingDenyOrServerBecomesEmpty() {
        ServerPolicy policy = ServerPolicyCodec.parse("{\"type\":\"policy\",\"mode\":\"lock\"}");
        assertNotNull(policy);
        assertTrue(policy.deny().isEmpty());
        assertEquals("", policy.server());
        assertEquals("", policy.reason());
        assertFalse(policy.denies("esp"));
    }

    @Test
    void nonPolicyPayloadsReturnNull() {
        assertNull(ServerPolicyCodec.parse(null));
        assertNull(ServerPolicyCodec.parse(""));
        assertNull(ServerPolicyCodec.parse("   "));
        assertNull(ServerPolicyCodec.parse("garbage"));
        assertNull(ServerPolicyCodec.parse("[1,2,3]"));
        assertNull(ServerPolicyCodec.parse("\"policy\""));
        assertNull(ServerPolicyCodec.parse("{\"type\":\"request\"}"));
        assertNull(ServerPolicyCodec.parse("{\"deny\":[\"esp\"]}"));
        assertNull(ServerPolicyCodec.parse("{\"type\":42}"));
    }

    @Test
    void requestJsonCarriesTheTypeTheProxyMatchesOn() {
        com.google.gson.JsonObject request = com.google.gson.JsonParser
                .parseString(ServerPolicyCodec.REQUEST_JSON).getAsJsonObject();
        assertEquals("request", request.get("type").getAsString());
    }
}
