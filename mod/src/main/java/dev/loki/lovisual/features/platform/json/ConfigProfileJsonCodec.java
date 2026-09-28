package dev.loki.lovisual.features.platform.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.loki.lovisual.config.profile.codec.ConfigProfileEntry;
import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter between {@link ConfigProfileSnapshot} (LoVisual's local
 * export/import shape, normally persisted through the binary
 * {@code ConfigProfileBinaryCodec}) and a plain JSON tree, so a snapshot can
 * travel as the {@code data} field of the backend's
 * {@code GET /configs/shared/{code}} response.
 *
 * <p>This is a genuine adapter, not a re-skin: the local format is a
 * compressed, CRC32-checked binary blob written by
 * {@code ConfigProfileBinaryCodec}, which is not what a JSONB column can
 * hold. The backend's own plan docs do not yet define an exact shape for
 * {@code data} (as of this pass, mod-side upload rides on this same shape via
 * {@code ConfigCloudUploader}, and
 * {@code configs-service/PLAN.md} only fixes its own easter-egg payload as
 * an example) so the shape below ({@code {meta:{...}, entries:[{ownerId,
 * displayName, values}]}}) is this task's judgment call: it mirrors
 * {@link ConfigProfileEntry}/{@link ConfigProfileMeta} field-for-field so a
 * future upload path can reuse it unchanged, and it round-trips the value
 * types {@code ConfigValue#fromJson} accepts (booleans, numbers, strings,
 * lists, maps) via plain Gson-native representations.
 */
public final class ConfigProfileJsonCodec {

    private ConfigProfileJsonCodec() {
    }

    public static JsonObject toJson(ConfigProfileSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is null");
        JsonObject root = new JsonObject();
        root.add("meta", metaToJson(snapshot.meta()));
        JsonArray entries = new JsonArray();
        for (ConfigProfileEntry entry : snapshot.entries()) {
            entries.add(entryToJson(entry));
        }
        root.add("entries", entries);
        return root;
    }

    public static ConfigProfileSnapshot fromJson(JsonObject root) {
        if (root == null) throw new IllegalArgumentException("json is null");
        ConfigProfileMeta meta = metaFromJson(root.getAsJsonObject("meta"));
        List<ConfigProfileEntry> entries = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("entries");
        if (array != null) {
            for (JsonElement element : array) {
                if (element.isJsonObject()) entries.add(entryFromJson(element.getAsJsonObject()));
            }
        }
        return new ConfigProfileSnapshot(meta, entries);
    }

    private static JsonObject metaToJson(ConfigProfileMeta meta) {
        JsonObject json = new JsonObject();
        json.addProperty("id", meta.id());
        json.addProperty("name", meta.name());
        json.addProperty("type", meta.type().folderName());
        json.addProperty("author", meta.author());
        json.addProperty("createdAt", meta.createdAt());
        json.addProperty("updatedAt", meta.updatedAt());
        json.addProperty("version", meta.version());
        return json;
    }

    private static ConfigProfileMeta metaFromJson(JsonObject json) {
        if (json == null) {
            return new ConfigProfileMeta(null, null, ConfigProfileType.MODULES, null, 0, 0, 0);
        }
        return new ConfigProfileMeta(
                stringOf(json, "id"),
                stringOf(json, "name"),
                ConfigProfileType.fromName(stringOf(json, "type")),
                stringOf(json, "author"),
                longOf(json, "createdAt"),
                longOf(json, "updatedAt"),
                (int) longOf(json, "version")
        );
    }

    private static JsonObject entryToJson(ConfigProfileEntry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("ownerId", entry.ownerId());
        json.addProperty("displayName", entry.displayName());
        JsonObject values = new JsonObject();
        for (Map.Entry<String, Object> value : entry.values().entrySet()) {
            values.add(value.getKey(), toElement(value.getValue()));
        }
        json.add("values", values);
        return json;
    }

    private static ConfigProfileEntry entryFromJson(JsonObject json) {
        Map<String, Object> values = new LinkedHashMap<>();
        JsonObject valuesJson = json.getAsJsonObject("values");
        if (valuesJson != null) {
            for (Map.Entry<String, JsonElement> entry : valuesJson.entrySet()) {
                values.put(entry.getKey(), toObject(entry.getValue()));
            }
        }
        return new ConfigProfileEntry(stringOf(json, "ownerId"), stringOf(json, "displayName"), values);
    }

    private static JsonElement toElement(Object value) {
        if (value == null) return com.google.gson.JsonNull.INSTANCE;
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        if (value instanceof Number n) return new JsonPrimitive(n);
        if (value instanceof CharSequence s) return new JsonPrimitive(s.toString());
        if (value instanceof Iterable<?> iterable) {
            JsonArray array = new JsonArray();
            for (Object item : iterable) array.add(toElement(item));
            return array;
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.add(String.valueOf(entry.getKey()), toElement(entry.getValue()));
            }
            return object;
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static Object toObject(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsDouble();
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) list.add(toObject(item));
            return list;
        }
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), toObject(entry.getValue()));
            }
            return map;
        }
        return null;
    }

    private static String stringOf(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    private static long longOf(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : 0L;
    }
}
