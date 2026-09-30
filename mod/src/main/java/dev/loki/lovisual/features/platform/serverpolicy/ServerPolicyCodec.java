package dev.loki.lovisual.features.platform.serverpolicy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON half of the {@code lovisual:policy} channel. Parsing is deliberately
 * forgiving: anything that is not a well-formed {@code "type":"policy"}
 * object returns {@code null}, and an unknown mode degrades to the
 * conservative {@link ServerPolicy.Mode#LOCK} — a broken payload must never
 * silently unlock the client.
 */
public final class ServerPolicyCodec {

    /** The C→S answer the proxy matches on to push the policy again. */
    public static final String REQUEST_JSON = "{\"type\":\"request\"}";

    private ServerPolicyCodec() {
    }

    public static ServerPolicy parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject object = element.getAsJsonObject();
            JsonElement type = object.get("type");
            if (type == null || !type.isJsonPrimitive() || !"policy".equals(type.getAsString())) {
                return null;
            }
            return new ServerPolicy(
                    parseMode(object.get("mode")),
                    string(object.get("reason")),
                    parseDeny(object.get("deny")),
                    string(object.get("server")));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ServerPolicy.Mode parseMode(JsonElement raw) {
        if (raw == null || !raw.isJsonPrimitive()) {
            return ServerPolicy.Mode.LOCK;
        }
        String value = raw.getAsString();
        if ("suggest".equalsIgnoreCase(value)) {
            return ServerPolicy.Mode.SUGGEST;
        }
        return ServerPolicy.Mode.LOCK;
    }

    private static List<String> parseDeny(JsonElement raw) {
        if (raw == null || !raw.isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement item : raw.getAsJsonArray()) {
            if (item != null && item.isJsonPrimitive()) {
                out.add(item.getAsString());
            }
        }
        return out;
    }

    private static String string(JsonElement raw) {
        return raw != null && raw.isJsonPrimitive() ? raw.getAsString() : "";
    }
}
