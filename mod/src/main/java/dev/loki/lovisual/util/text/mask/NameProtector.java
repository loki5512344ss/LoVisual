package dev.loki.lovisual.util.text.mask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.loki.lovisual.features.module.modules.misc.protect.NameProtect;

/**
 * Pure name-replacement logic for NameProtect: replaces occurrences of real
 * nicknames with display aliases inside plain strings (case-insensitive,
 * bounded matches only). Component-tree rewriting happens in the mixins.
 */
public final class NameProtector {

    private final Map<String, String> aliases = new LinkedHashMap<>();

    /**
     * @param realName nickname to hide; null/blank is ignored
     * @param alias    replacement text; blank keeps the alias empty
     */
    public void put(String realName, String alias) {
        if (realName == null || realName.isBlank()) return;
        aliases.put(realName, alias == null ? "" : alias);
    }

    public void clear() {
        aliases.clear();
    }

    public boolean isEmpty() {
        return aliases.isEmpty();
    }

    public String replace(String text) {
        if (text == null || text.isEmpty() || aliases.isEmpty()) return text;

        List<String> names = new ArrayList<>(aliases.keySet());
        names.sort((a, b) -> Integer.compare(b.length(), a.length()));

        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        outer:
        while (i < text.length()) {
            for (String name : names) {
                if (!matchesAt(text, i, name)) continue;
                String alias = aliases.get(name);
                out.append(alias);
                i += name.length();
                continue outer;
            }
            out.append(text.charAt(i));
            i++;
        }
        return out.toString();
    }

    /**
     * Replace only when the whole string equals the name — used for scoreboards
     * and tab entries where the display name IS the nickname.
     */
    public String replaceExact(String text) {
        if (text == null || text.isBlank() || aliases.isEmpty()) return text;
        String alias = aliases.get(text.trim());
        return alias != null ? alias : text;
    }

    private static boolean matchesAt(String text, int start, String name) {
        if (!text.regionMatches(true, start, name, 0, name.length())) return false;
        if (start > 0 && isWordChar(text.charAt(start - 1))) return false;
        int end = start + name.length();
        if (end < text.length() && isWordChar(text.charAt(end))) return false;
        return true;
    }

    private static boolean isWordChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
