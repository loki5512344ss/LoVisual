package dev.loki.lovisual.policy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The wire format of the {@code lovisual:policy} channel: UTF-8 JSON, one
 * message per plugin-message packet. The mod only ever parses requests
 * ({@link #isRequest}); the plugin only ever encodes policies
 * ({@link #encodePolicy}).
 */
public final class PolicyMessage {

    /** The registry name of the plugin channel shared with the mod. */
    public static final String CHANNEL = "lovisual:policy";

    private PolicyMessage() {
    }

    /** Encodes a S→C policy payload. A null server name is sent as "". */
    public static String encodePolicy(PolicyConfig config, String server) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "policy");
        json.addProperty("mode", config.mode() == PolicyMode.SUGGEST ? "suggest" : "lock");
        json.addProperty("reason", config.reason());
        json.addProperty("server", server == null ? "" : server);
        JsonArray deny = new JsonArray();
        for (String id : config.deny()) {
            deny.add(id);
        }
        json.add("deny", deny);
        return json.toString();
    }

    /** True when the given C→S payload is a policy request from the mod. */
    public static boolean isRequest(String payload) {
        if (payload == null || payload.isBlank()) {
            return false;
        }
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            return json.has("type") && "request".equals(json.get("type").getAsString());
        } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
            return false;
        }
    }
}
