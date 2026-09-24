package dev.loki.lovisual.features.module.module;

import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Name and identity helpers for modules: alias normalization, setting id derivation
 * from config names and the i18n-first display/description translation lookup.
 */
public final class ModuleNames {

    private ModuleNames() {
    }

    public static List<String> normalizeAliases(String[] aliases) {
        if (aliases == null || aliases.length == 0) return List.of();

        List<String> out = new ArrayList<>(aliases.length);
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) continue;
            out.add(alias.trim());
        }
        return List.copyOf(out);
    }

    public static String stripOwnerPrefix(String value, String owner) {
        if (value == null || value.isBlank() || owner == null || owner.isBlank()) return value;

        String normalizedOwner = normalizePrefix(owner);
        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (!lowerValue.startsWith(normalizedOwner)) return value;

        String tail = value.substring(normalizedOwner.length());
        if (tail.isEmpty()) return value;

        char first = tail.charAt(0);
        if (first == '_' || first == '-' || first == '.') {
            tail = tail.substring(1);
        } else if (!Character.isUpperCase(first)) {
            return value;
        }

        return camelToSnake(tail);
    }

    private static String normalizePrefix(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String camelToSnake(String value) {
        if (value == null || value.isEmpty()) return value;

        StringBuilder out = new StringBuilder(value.length() + 4);
        char previous = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '-' || c == '.' || c == ' ') {
                c = '_';
            }
            if (c == '_') {
                if (!out.isEmpty() && out.charAt(out.length() - 1) != '_') {
                    out.append('_');
                }
                previous = '_';
                continue;
            }
            if (Character.isUpperCase(c) && i > 0 && previous != '_' && !Character.isUpperCase(previous)) {
                out.append('_');
            }
            out.append(Character.toLowerCase(c));
            previous = c;
        }
        return out.toString();
    }

    public static String settingId(String configName, String ownerId, String ownerSimpleName) {
        if (configName == null || configName.isBlank()) return configName;

        String tail = stripOwnerPrefix(configName, ownerId);
        tail = stripOwnerPrefix(tail, ownerSimpleName);
        return tail;
    }

    public static String translateFirst(String fallback, String... keys) {
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            String translated = I18n.get(key);
            if (!translated.equals(key)) {
                return translated;
            }
        }
        return fallback == null ? "" : fallback;
    }
}
