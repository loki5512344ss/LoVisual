package dev.loki.lovisual.policy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyMessageTest {

    @Test
    void encodePolicyWritesAllFields() {
        PolicyConfig cfg = new PolicyConfig(true, PolicyMode.SUGGEST, "server rules",
                List.of("esp", "freecam"));
        String json = PolicyMessage.encodePolicy(cfg, "lobby-1");
        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("policy", parsed.get("type").getAsString());
        assertEquals("suggest", parsed.get("mode").getAsString());
        assertEquals("server rules", parsed.get("reason").getAsString());
        assertEquals("lobby-1", parsed.get("server").getAsString());
        assertEquals(2, parsed.getAsJsonArray("deny").size());
        assertEquals("esp", parsed.getAsJsonArray("deny").get(0).getAsString());
    }

    @Test
    void encodePolicyEscapesReasonAndDefaultsServerToEmpty() {
        PolicyConfig cfg = new PolicyConfig(true, PolicyMode.LOCK, "say \"hi\"\nline2", List.of());
        JsonObject parsed = JsonParser.parseString(PolicyMessage.encodePolicy(cfg, null)).getAsJsonObject();
        assertEquals("say \"hi\"\nline2", parsed.get("reason").getAsString());
        assertEquals("", parsed.get("server").getAsString());
        assertEquals(0, parsed.getAsJsonArray("deny").size());
    }

    @Test
    void isRequestAcceptsOnlyTypedRequests() {
        assertTrue(PolicyMessage.isRequest("{\"type\":\"request\"}"));
        assertTrue(PolicyMessage.isRequest(" { \"type\" : \"request\" } "));
        assertFalse(PolicyMessage.isRequest("{\"type\":\"policy\"}"));
        assertFalse(PolicyMessage.isRequest("{\"type\":42}"));
        assertFalse(PolicyMessage.isRequest("garbage"));
        assertFalse(PolicyMessage.isRequest(""));
        assertFalse(PolicyMessage.isRequest(null));
    }
}
