package dev.loki.lovisual.config.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.i18n.ConfigValueAliasProvider;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.util.logging.DebugLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigGsonSupport {
    static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ConfigGsonSupport() {}

    static ConfigSaveScheduler.PendingWrite buildModuleWrite(ConfigObject obj) {
        try {
            JsonObject root = new JsonObject();
            for (ConfigValue<?> v : obj.getConfigValues()) {
                root.add(v.getName(), toJsonElement(v.toJson()));
            }
            String text = GSON.toJson(root) + "\n";
            Path out = ConfigFilePaths.moduleFile(obj);
            return new ConfigSaveScheduler.PendingWrite(out, text);
        } catch (Exception e) {
            DebugLog.error("Failed to serialize config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
            return null;
        }
    }

    static ConfigSaveScheduler.PendingWrite buildJsonWrite(ConfigObject obj) {
        try {
            JsonObject root = new JsonObject();
            for (ConfigValue<?> v : obj.getConfigValues()) {
                root.add(v.getName(), toJsonElement(v.toJson()));
            }
            String text = GSON.toJson(root) + "\n";
            Path out = ConfigFilePaths.jsonFile(obj);
            return new ConfigSaveScheduler.PendingWrite(out, text);
        } catch (Exception e) {
            DebugLog.error("Failed to serialize config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
            return null;
        }
    }

    static void loadModule(ConfigObject obj) {
        try {
            Path path = ConfigFilePaths.moduleFile(obj);
            if (!Files.exists(path)) {
                DebugLog.config("Config defaults created: %s", path.toAbsolutePath());
                ConfigSerializer.save(obj);
                return;
            }
            DebugLog.config("Config loading: %s", path.toAbsolutePath());
            String txt = Files.readString(path);
            JsonElement parsed = GSON.fromJson(txt, JsonElement.class);
            if (!(parsed instanceof JsonObject root)) return;
            Module module = (Module) obj;
            JsonElement legacySettings = root.get("settings");
            if (legacySettings != null) {
                module.applyLegacySettings(unwrapJsonElement(legacySettings));
            }
            boolean usedLegacyAlias = false;
            for (ConfigValue<?> v : obj.getConfigValues()) {
                JsonLookup lookup = findJsonElement(obj, root, v.getName());
                if (lookup.element() != null) {
                    v.fromJson(unwrapJsonElement(lookup.element()));
                    usedLegacyAlias |= lookup.alias();
                }
            }
            module.syncSettingsAfterLoad();
            if (usedLegacyAlias) ConfigSerializer.requestSave(obj);
        } catch (Exception e) {
            DebugLog.error("Failed to load config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
        }
    }

    static void loadJson(ConfigObject obj) {
        try {
            Path path = ConfigFilePaths.jsonFile(obj);
            if (!Files.exists(path)) {
                DebugLog.config("Config defaults created: %s", path.toAbsolutePath());
                ConfigSerializer.save(obj);
                return;
            }
            DebugLog.config("Config loading: %s", path.toAbsolutePath());
            String txt = Files.readString(path);
            JsonElement parsed = GSON.fromJson(txt, JsonElement.class);
            if (!(parsed instanceof JsonObject root)) return;
            boolean missing = false;
            boolean usedLegacyAlias = false;
            for (ConfigValue<?> v : obj.getConfigValues()) {
                JsonLookup lookup = findJsonElement(obj, root, v.getName());
                if (lookup.element() != null) {
                    v.fromJson(unwrapJsonElement(lookup.element()));
                    usedLegacyAlias |= lookup.alias();
                } else {
                    missing = true;
                }
            }
            if (missing || usedLegacyAlias) ConfigSerializer.requestSave(obj);
        } catch (Exception e) {
            DebugLog.error("Failed to load config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
        }
    }

    static JsonElement toJsonElement(Object v) {
        if (v == null) return JsonNull.INSTANCE;
        if (v instanceof JsonElement el) return el;
        if (v instanceof Boolean b) return new JsonPrimitive(b);
        if (v instanceof Number n) return new JsonPrimitive(n);
        if (v instanceof CharSequence s) return new JsonPrimitive(s.toString());
        if (v instanceof Map<?, ?> map) {
            JsonObject obj = new JsonObject();
            for (var e : map.entrySet()) obj.add(String.valueOf(e.getKey()), toJsonElement(e.getValue()));
            return obj;
        }
        if (v instanceof Iterable<?> it) {
            JsonArray arr = new JsonArray();
            for (Object el : it) arr.add(toJsonElement(el));
            return arr;
        }
        return new JsonPrimitive(String.valueOf(v));
    }

    static Object unwrapJsonElement(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsNumber();
            return p.getAsString();
        }
        if (el.isJsonArray()) {
            List<Object> out = new ArrayList<>();
            for (JsonElement child : el.getAsJsonArray()) out.add(unwrapJsonElement(child));
            return out;
        }
        if (el.isJsonObject()) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (var e : el.getAsJsonObject().entrySet()) out.put(e.getKey(), unwrapJsonElement(e.getValue()));
            return out;
        }
        return null;
    }

    static JsonLookup findJsonElement(ConfigObject obj, JsonObject root, String valueName) {
        if (root == null || valueName == null) return JsonLookup.missing();
        JsonElement direct = root.get(valueName);
        if (direct != null && !direct.isJsonNull()) return new JsonLookup(direct, false);
        if (obj instanceof ConfigValueAliasProvider provider) {
            List<String> aliases = provider.getLegacyValueNames(valueName);
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias == null || alias.isBlank()) continue;
                    JsonElement legacy = root.get(alias);
                    if (legacy != null && !legacy.isJsonNull()) return new JsonLookup(legacy, true);
                }
            }
        }
        return JsonLookup.missing();
    }

    record JsonLookup(JsonElement element, boolean alias) {
        static JsonLookup missing() { return new JsonLookup(null, false); }
    }
}
