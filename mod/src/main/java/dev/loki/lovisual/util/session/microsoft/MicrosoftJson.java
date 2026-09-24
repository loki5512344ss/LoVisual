package dev.loki.lovisual.util.session.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public final class MicrosoftJson {
    public static final Gson GSON = new Gson();

    private MicrosoftJson() {
    }

    public static String string(JsonObject json, String key) {
        try {
            return json.get(key).getAsString();
        } catch (Throwable t) {
            throw new JsonParseException("Expected string '" + key + "': " + json, t);
        }
    }

    public static long number(JsonObject json, String key) {
        try {
            return json.get(key).getAsLong();
        } catch (Throwable t) {
            throw new JsonParseException("Expected number '" + key + "': " + json, t);
        }
    }

    public static JsonObject object(JsonObject json, String key) {
        try {
            return json.get(key).getAsJsonObject();
        } catch (Throwable t) {
            throw new JsonParseException("Expected object '" + key + "': " + json, t);
        }
    }

    public static JsonArray array(JsonObject json, String key) {
        try {
            return json.get(key).getAsJsonArray();
        } catch (Throwable t) {
            throw new JsonParseException("Expected array '" + key + "': " + json, t);
        }
    }
}
