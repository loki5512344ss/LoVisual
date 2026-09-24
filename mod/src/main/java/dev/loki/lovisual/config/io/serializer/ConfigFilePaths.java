package dev.loki.lovisual.config.io;

import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.module.core.Module;

import java.nio.file.Path;
import java.util.Locale;

final class ConfigFilePaths {
    private static final Path CONFIG_DIR = ConfigPaths.root();

    private ConfigFilePaths() {}

    static Path fileOf(ConfigObject obj) {
        return isModule(obj) ? moduleFile(obj) : utilFile(obj);
    }

    static Path moduleFile(ConfigObject obj) {
        String category = "uncategorized";
        if (obj instanceof Module module && module.getCategory() != null) {
            category = sanitizeConfigName(module.getCategory().name());
        }
        return CONFIG_DIR.resolve("modules")
                .resolve(category)
                .resolve(configNameOf(obj) + ".json");
    }

    static Path jsonFile(ConfigObject obj) {
        if (obj instanceof DraggableHudElement) {
            return CONFIG_DIR.resolve("hud")
                    .resolve("draggable")
                    .resolve(configNameOf(obj) + ".json");
        }
        if (obj instanceof AbstractHudElement) {
            return CONFIG_DIR.resolve("hud")
                    .resolve("nondraggable")
                    .resolve(configNameOf(obj) + ".json");
        }
        return namedJsonFile(configNameOf(obj));
    }

    static Path utilFile(ConfigObject obj) {
        return isJsonConfig(obj) ? jsonFile(obj) : namedJson5File(configNameOf(obj));
    }

    static String configNameOf(ConfigObject obj) {
        String name = null;
        if (obj instanceof ConfigNameProvider provider) {
            name = provider.getConfigName();
        }
        if (name == null || name.isBlank()) {
            name = obj.getClass().getSimpleName();
        }
        return sanitizeConfigName(name);
    }

    static String sanitizeConfigName(String name) {
        if (name == null) return "config";
        String base = name.trim().toLowerCase(Locale.ROOT);
        if (base.isEmpty()) return "config";
        StringBuilder out = new StringBuilder(base.length());
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.toString();
    }

    static Path namedJsonFile(String configName) {
        return CONFIG_DIR.resolve(sanitizeConfigName(configName) + ".json");
    }

    static Path namedJson5File(String configName) {
        return CONFIG_DIR.resolve(sanitizeConfigName(configName) + ".json5");
    }

    static Path rootDir() {
        return CONFIG_DIR;
    }

    private static boolean isModule(ConfigObject obj) {
        return obj instanceof Module;
    }

    private static boolean isJsonConfig(ConfigObject obj) {
        return obj instanceof JsonConfigObject;
    }
}
