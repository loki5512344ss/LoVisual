package dev.loki.lovisual.features.gui.hud.base;

import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Naming and localization helpers for {@link BaseHudElement}: derive stable setting ids from
 * config names by stripping the element/class owner prefix, and resolve a translated HUD title
 * from the resource-pack i18n keys. Extracted so the base class stays under the file-size cap.
 */
final class HudElementNamer {

    private HudElementNamer() { }

    /** Strip the element-id and simple-class prefixes from a config name to yield the setting id. */
    static String settingId(String configName, String ownerId, String ownerClass) {
        if (configName == null || configName.isBlank()) return configName;
        String tail = stripOwnerPrefix(configName, ownerId);
        tail = stripOwnerPrefix(tail, ownerClass);
        return tail;
    }

    /** Resolve a translated HUD title from candidate i18n keys, falling back to the raw title. */
    static String translateTitle(String fallback, String prefix, String id) {
        List<String> keys = new ArrayList<>(3);
        if (prefix != null && prefix.startsWith("setting.draggable.")) {
            keys.add("hud.draggable." + id);
            keys.add("hud.draggable." + id + ".title");
        } else if (prefix != null && prefix.startsWith("setting.hud_element.")) {
            keys.add("hud.static." + id);
            keys.add("hud.static." + id + ".title");
        }
        keys.add("hud." + id);
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            String translated = I18n.get(key);
            if (!translated.equals(key)) return translated;
        }
        return fallback == null ? "" : fallback;
    }

    private static String stripOwnerPrefix(String value, String owner) {
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
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) out.append(c);
        }
        return out.toString();
    }

    private static String camelToSnake(String value) {
        if (value == null || value.isEmpty()) return value;
        StringBuilder out = new StringBuilder(value.length() + 4);
        char previous = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '-' || c == '.' || c == ' ') c = '_';
            if (c == '_') {
                if (!out.isEmpty() && out.charAt(out.length() - 1) != '_') out.append('_');
                previous = '_';
                continue;
            }
            if (Character.isUpperCase(c) && i > 0 && previous != '_' && !Character.isUpperCase(previous)) out.append('_');
            out.append(Character.toLowerCase(c));
            previous = c;
        }
        return out.toString();
    }
}
