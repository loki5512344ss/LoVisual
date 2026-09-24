package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.features.theme.Themes.GradientSpec;
import dev.loki.lovisual.features.theme.Themes.Theme;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serialises a {@link ThemeEntry} into the flat string-keyed map used by exportable
 * config profiles, and rebuilds an entry from that map on import. Every field is
 * written as a plain scalar so profiles stay human-editable.
 * <p>
 * Lives in a sibling folder to keep the main theme directory within the
 * four-file-per-directory limit; the package is shared with {@link Themes} so
 * STORE access keeps working through the package-private reference.
 */
final class ThemesProfileCodec {

    private ThemesProfileCodec() { }

    /** Pack a theme entry (12 semantic colors + 5 gradient blocks) into a profile map. */
    static Map<String, Object> toProfileValues(ThemeEntry entry) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (entry == null) return out;
        Theme t = entry.theme();
        out.put("name", entry.name());
        out.put("windowBg", Themes.colorString(t.windowBg()));
        out.put("windowHeader", Themes.colorString(t.windowHeader()));
        out.put("windowStroke", Themes.colorString(t.windowStroke()));
        out.put("surface", Themes.colorString(t.surface()));
        out.put("surfaceHover", Themes.colorString(t.surfaceHover()));
        out.put("cardEnabled", Themes.colorString(t.cardEnabled()));
        out.put("cardDisabled", Themes.colorString(t.cardDisabled()));
        out.put("textPrimary", Themes.colorString(t.textPrimary()));
        out.put("textMuted", Themes.colorString(t.textMuted()));
        out.put("accent", Themes.colorString(t.accent()));
        out.put("accentSoft", Themes.colorString(t.accentSoft()));
        out.put("strokeSoft", Themes.colorString(t.strokeSoft()));
        writeGradient(out, "window", entry.windowGradient());
        writeGradient(out, "header", entry.headerGradient());
        writeGradient(out, "surface", entry.surfaceGradient());
        writeGradient(out, "card", entry.cardGradient());
        writeGradient(out, "stroke", entry.strokeGradient());
        return out;
    }

    /** Rebuild a theme entry from a profile map, using the currently-stored entry (or classic) as fallback. */
    static ThemeEntry fromProfileValues(String id, String fallbackName, Map<String, Object> values) {
        String key = id == null || id.isBlank() ? Themes.uniqueCustomId(fallbackName) : id.toLowerCase();
        ThemeEntry existing = Themes.STORE.getById(key);
        ThemeEntry baseEntry = existing != null ? existing : ThemePresets1.CLASSIC_ENTRY;
        Theme base = baseEntry.theme();
        Map<String, Object> map = values == null ? Map.of() : values;
        String name = stringValue(map.get("name"), fallbackName == null || fallbackName.isBlank() ? key : fallbackName);
        Theme theme = new Theme(
                Themes.colorValue(map.get("windowBg"), base.windowBg()),
                Themes.colorValue(map.get("windowHeader"), base.windowHeader()),
                Themes.colorValue(map.get("windowStroke"), base.windowStroke()),
                Themes.colorValue(map.get("surface"), base.surface()),
                Themes.colorValue(map.get("surfaceHover"), base.surfaceHover()),
                Themes.colorValue(map.get("cardEnabled"), base.cardEnabled()),
                Themes.colorValue(map.get("cardDisabled"), base.cardDisabled()),
                Themes.colorValue(map.get("textPrimary"), base.textPrimary()),
                Themes.colorValue(map.get("textMuted"), base.textMuted()),
                Themes.colorValue(map.get("accent"), base.accent()),
                Themes.colorValue(map.get("accentSoft"), base.accentSoft()),
                Themes.colorValue(map.get("strokeSoft"), base.strokeSoft())
        );
        return new ThemeEntry(
                key, name, false, theme,
                readGradient(map, "window", baseEntry.windowGradient(), theme.windowBg()),
                readGradient(map, "header", baseEntry.headerGradient(), theme.windowHeader()),
                readGradient(map, "surface", baseEntry.surfaceGradient(), theme.surface()),
                readGradient(map, "card", baseEntry.cardGradient(), theme.cardEnabled()),
                readGradient(map, "stroke", baseEntry.strokeGradient(), theme.windowStroke())
        );
    }

    private static void writeGradient(Map<String, Object> out, String prefix, GradientSpec spec) {
        GradientSpec g = spec != null ? spec : ThemesBlending.flatGradient(ThemePresets1.CLASSIC.accent());
        out.put(prefix + "GradientEnabled", g.enabled());
        out.put(prefix + "GradientStart", Themes.colorString(g.start()));
        out.put(prefix + "GradientEnd", Themes.colorString(g.end()));
        out.put(prefix + "GradientAngle", g.angleDeg());
    }

    private static GradientSpec readGradient(Map<String, Object> map, String prefix, GradientSpec fallback, int baseColor) {
        GradientSpec fb = fallback != null ? fallback : ThemesBlending.flatGradient(baseColor);
        boolean enabled = booleanValue(map.get(prefix + "GradientEnabled"), fb.enabled());
        int start = Themes.colorValue(map.get(prefix + "GradientStart"), fb.start());
        int end = Themes.colorValue(map.get(prefix + "GradientEnd"), fb.end());
        float angle = floatValue(map.get(prefix + "GradientAngle"), fb.angleDeg());
        return new GradientSpec(enabled, start, end, angle);
    }

    private static String stringValue(Object raw, String fallback) {
        if (raw instanceof CharSequence s && !s.toString().isBlank()) return s.toString();
        return fallback;
    }

    private static boolean booleanValue(Object raw, boolean fallback) {
        if (raw instanceof Boolean b) return b;
        if (raw instanceof CharSequence s) return Boolean.parseBoolean(s.toString());
        return fallback;
    }

    private static float floatValue(Object raw, float fallback) {
        if (raw instanceof Number n) return n.floatValue();
        if (raw instanceof CharSequence s) {
            try {
                return Float.parseFloat(s.toString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
