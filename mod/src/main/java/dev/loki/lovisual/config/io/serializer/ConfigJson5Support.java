package dev.loki.lovisual.config.io;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;
import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.i18n.ConfigValueAliasProvider;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.relations.EntityFilters;
import dev.loki.lovisual.util.logging.DebugLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigJson5Support {
    static final Json5 JSON5 = Json5.builder(builder -> builder
            .trailingComma()
            .indentFactor(2)
            .quoteSingle()
            .build());

    private ConfigJson5Support() {}

    static ConfigSaveScheduler.PendingWrite buildJson5Write(ConfigObject obj) {
        try {
            Json5Object root = new Json5Object();
            for (ConfigValue<?> v : obj.getConfigValues()) {
                root.add(v.getName(), toJson5Element(v.toJson()));
            }
            String text = JSON5.serialize(root);
            String sb = text + "\n";
            Path out = ConfigFilePaths.utilFile(obj);
            if (obj instanceof EntityFilters) {
                DebugLog.config("[EntityFilters][SAVE] to=%s data=%s", out.toAbsolutePath(), text);
            }
            return new ConfigSaveScheduler.PendingWrite(out, sb);
        } catch (Exception e) {
            DebugLog.error("Failed to serialize config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
            return null;
        }
    }

    static void loadJson5(ConfigObject obj) {
        try {
            Path path = ConfigFilePaths.utilFile(obj);
            if (!Files.exists(path)) {
                DebugLog.config("Config defaults created: %s", path.toAbsolutePath());
                ConfigSerializer.save(obj);
                return;
            }
            DebugLog.config("Config loading: %s", path.toAbsolutePath());
            String txt = Files.readString(path);
            Json5Element parsed = JSON5.parse(txt);
            if (!(parsed instanceof Json5Object root)) return;
            Map<String, Json5Element> map = root.asMap();
            boolean missing = false;
            boolean usedLegacyAlias = false;
            for (ConfigValue<?> v : obj.getConfigValues()) {
                Json5Lookup lookup = findJson5Element(obj, map, v.getName());
                if (lookup.element() != null) {
                    v.fromJson(unwrapJson5(lookup.element()));
                    usedLegacyAlias |= lookup.alias();
                } else {
                    missing = true;
                }
            }
            if (obj instanceof Module module) module.syncSettingsAfterLoad();
            if (missing || usedLegacyAlias) ConfigSerializer.requestSave(obj);
        } catch (Exception e) {
            DebugLog.error("Failed to load config %s", e, ConfigFilePaths.fileOf(obj).toAbsolutePath());
        }
    }

    static Json5Element toJson5Element(Object v) {
        if (v == null) return Json5Primitive.fromNull();
        if (v instanceof Json5Element el) return el;
        if (v instanceof Boolean b) return Json5Primitive.fromBoolean(b);
        if (v instanceof Number n) return Json5Primitive.fromNumber(n);
        if (v instanceof CharSequence s) return Json5Primitive.fromString(s.toString());
        if (v instanceof Map<?, ?> map) {
            Json5Object obj = new Json5Object();
            for (var e : map.entrySet()) obj.add(String.valueOf(e.getKey()), toJson5Element(e.getValue()));
            return obj;
        }
        if (v instanceof Iterable<?> it) {
            Json5Array arr = new Json5Array();
            for (Object el : it) arr.add(toJson5Element(el));
            return arr;
        }
        return Json5Primitive.fromString(String.valueOf(v));
    }

    static Object unwrapJson5(Json5Element el) {
        if (el == null || el.isJson5Null()) return null;
        if (el.isJson5Primitive()) {
            Json5Primitive p = el.getAsJson5Primitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsNumber();
            return p.getAsString();
        }
        if (el.isJson5Array()) {
            List<Object> out = new ArrayList<>();
            for (Json5Element child : el.getAsJson5Array()) out.add(unwrapJson5(child));
            return out;
        }
        if (el.isJson5Object()) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (var e : el.getAsJson5Object().asMap().entrySet()) out.put(e.getKey(), unwrapJson5(e.getValue()));
            return out;
        }
        return null;
    }

    static Json5Lookup findJson5Element(ConfigObject obj, Map<String, Json5Element> map, String valueName) {
        if (map == null || valueName == null) return Json5Lookup.missing();
        Json5Element direct = map.get(valueName);
        if (direct != null) return new Json5Lookup(direct, false);
        if (obj instanceof ConfigValueAliasProvider provider) {
            List<String> aliases = provider.getLegacyValueNames(valueName);
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias == null || alias.isBlank()) continue;
                    Json5Element legacy = map.get(alias);
                    if (legacy != null) return new Json5Lookup(legacy, true);
                }
            }
        }
        return Json5Lookup.missing();
    }

    record Json5Lookup(Json5Element element, boolean alias) {
        static Json5Lookup missing() { return new Json5Lookup(null, false); }
    }
}
