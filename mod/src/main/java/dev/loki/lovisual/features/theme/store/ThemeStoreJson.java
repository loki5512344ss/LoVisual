package dev.loki.lovisual.features.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.loki.lovisual.features.theme.Themes.GradientSpec;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;

import java.util.Locale;

/**
 * Static JSON (de)serialisation helpers for {@link ThemeStore}. Owns the shape of the
 * on-disk format: an {@code id} / {@code name} header plus a nested {@code colors} object
 * (12 ARGB slots) and a {@code gradients} object (five named gradient specs).
 * <p>
 * Lives in a sibling folder so the main theme directory stays within the
 * four-file-per-directory limit; the package declaration is unchanged.
 */
final class ThemeStoreJson {

    private ThemeStoreJson() { }

    static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).trim();
    }

    static String colorToString(int argb) {
        return String.format("#%08X", argb);
    }

    /** Accepts {@code #RRGGBB[AAMM]} and bare hex; short forms are treated as RGB with opaque alpha. */
    static int parseColorString(String s, int fallback) {
        if (s == null || s.isBlank()) return fallback;
        String str = s.trim();
        if (str.startsWith("0x") || str.startsWith("0X")) str = str.substring(2);
        if (str.startsWith("#")) str = str.substring(1);
        try {
            long v = Long.parseUnsignedLong(str, 16);
            return str.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static JsonObject gradientToJson(GradientSpec spec) {
        GradientSpec out = spec != null ? spec : new GradientSpec(false, 0xFFFFFFFF, 0xFFFFFFFF, 90f);
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", out.enabled());
        obj.addProperty("start", colorToString(out.start()));
        obj.addProperty("end", colorToString(out.end()));
        obj.addProperty("angle", out.angleDeg());
        return obj;
    }

    static int parseColor(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        return parseColor(obj.get(key), fallback);
    }

    static int parseColor(JsonElement raw, int fallback) {
        if (raw == null || !raw.isJsonPrimitive()) return fallback;
        var prim = raw.getAsJsonPrimitive();
        if (prim.isNumber()) return prim.getAsInt();
        if (prim.isString()) return parseColorString(prim.getAsString(), fallback);
        return fallback;
    }

    static JsonObject getObject(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) return null;
        JsonElement el = obj.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    static JsonArray getArray(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) return null;
        JsonElement el = obj.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
    }

    static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || key == null || !obj.has(key)) return fallback;
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return fallback;
        var prim = el.getAsJsonPrimitive();
        if (prim.isBoolean()) return prim.getAsBoolean();
        if (prim.isString()) return Boolean.parseBoolean(prim.getAsString());
        return fallback;
    }

    static String getString(JsonObject obj, String key, String fallback) {
        if (obj == null || key == null || !obj.has(key)) return fallback;
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return fallback;
        var prim = el.getAsJsonPrimitive();
        return prim.isString() ? prim.getAsString() : fallback;
    }

    static float getFloat(JsonObject obj, String key, float fallback) {
        if (obj == null || key == null || !obj.has(key)) return fallback;
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return fallback;
        var prim = el.getAsJsonPrimitive();
        if (prim.isNumber()) return prim.getAsFloat();
        if (prim.isString()) {
            try {
                return Float.parseFloat(prim.getAsString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static float normalizeAngle(float angle) {
        if (!Float.isFinite(angle)) return 90f;
        angle %= 360f;
        return angle < 0f ? angle + 360f : angle;
    }

    /** Full on-disk shape: id / name / builtin header plus nested colors and gradients objects. */
    static JsonObject entryToJson(ThemeEntry entry) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", entry.getId());
        obj.addProperty("name", entry.name());
        obj.addProperty("builtin", false);
        Themes.Theme theme = entry.theme();
        JsonObject colors = new JsonObject();
        colors.addProperty("windowBg", colorToString(theme.windowBg()));
        colors.addProperty("windowHeader", colorToString(theme.windowHeader()));
        colors.addProperty("windowStroke", colorToString(theme.windowStroke()));
        colors.addProperty("surface", colorToString(theme.surface()));
        colors.addProperty("surfaceHover", colorToString(theme.surfaceHover()));
        colors.addProperty("cardEnabled", colorToString(theme.cardEnabled()));
        colors.addProperty("cardDisabled", colorToString(theme.cardDisabled()));
        colors.addProperty("textPrimary", colorToString(theme.textPrimary()));
        colors.addProperty("textMuted", colorToString(theme.textMuted()));
        colors.addProperty("accent", colorToString(theme.accent()));
        colors.addProperty("accentSoft", colorToString(theme.accentSoft()));
        colors.addProperty("strokeSoft", colorToString(theme.strokeSoft()));
        obj.add("colors", colors);
        JsonObject gradients = new JsonObject();
        gradients.add("window", gradientToJson(entry.windowGradient()));
        gradients.add("header", gradientToJson(entry.headerGradient()));
        gradients.add("surface", gradientToJson(entry.surfaceGradient()));
        gradients.add("card", gradientToJson(entry.cardGradient()));
        gradients.add("stroke", gradientToJson(entry.strokeGradient()));
        obj.add("gradients", gradients);
        obj.add("backdrop", backdropToJson(entry.backdrop()));
        return obj;
    }

    /** Parse one theme entry from its JSON object; missing slots fall back to the same-named preset (or classic). */
    static ThemeEntry parseEntry(JsonObject obj, boolean allowBuiltin) {
        String id = getString(obj, "id", null);
        if (id == null || id.isBlank()) return null;
        String key = normalizeId(id);
        ThemeEntry preset = Themes.presetEntry(key);
        if (preset != null && !allowBuiltin) return null;
        String name = getString(obj, "name", preset != null ? preset.name() : key);
        boolean builtin = allowBuiltin && getBoolean(obj, "builtin", preset != null && preset.builtin());
        JsonObject colors = getObject(obj, "colors");
        Themes.Theme base = preset != null ? preset.theme() : Theme.classic();
        int windowBg = parseColor(colors, "windowBg", base.windowBg());
        int windowHeader = parseColor(colors, "windowHeader", base.windowHeader());
        int windowStroke = parseColor(colors, "windowStroke", base.windowStroke());
        int surface = parseColor(colors, "surface", base.surface());
        int surfaceHover = parseColor(colors, "surfaceHover", base.surfaceHover());
        int cardEnabled = parseColor(colors, "cardEnabled", base.cardEnabled());
        int cardDisabled = parseColor(colors, "cardDisabled", base.cardDisabled());
        int textPrimary = parseColor(colors, "textPrimary", base.textPrimary());
        int textMuted = parseColor(colors, "textMuted", base.textMuted());
        int accent = parseColor(colors, "accent", base.accent());
        int accentSoft = parseColor(colors, "accentSoft", base.accentSoft());
        int strokeSoft = parseColor(colors, "strokeSoft", base.strokeSoft());
        Themes.Theme theme = new Themes.Theme(windowBg, windowHeader, windowStroke, surface, surfaceHover, cardEnabled,
                cardDisabled, textPrimary, textMuted, accent, accentSoft, strokeSoft);
        JsonObject gradients = getObject(obj, "gradients");
        return new ThemeEntry(key, name != null && !name.isBlank() ? name.trim() : key, builtin, theme,
                parseGradient(getObject(gradients, "window"), preset != null ? preset.windowGradient() : new GradientSpec(false, windowBg, windowBg, 90f), windowBg),
                parseGradient(getObject(gradients, "header"), preset != null ? preset.headerGradient() : new GradientSpec(false, windowHeader, windowHeader, 90f), windowHeader),
                parseGradient(getObject(gradients, "surface"), preset != null ? preset.surfaceGradient() : new GradientSpec(false, surface, surface, 90f), surface),
                parseGradient(getObject(gradients, "card"), preset != null ? preset.cardGradient() : new GradientSpec(false, cardEnabled, cardEnabled, 90f), cardEnabled),
                parseGradient(getObject(gradients, "stroke"), preset != null ? preset.strokeGradient() : new GradientSpec(false, windowStroke, windowStroke, 90f), windowStroke),
                parseBackdrop(getObject(obj, "backdrop"), preset != null ? preset.backdrop() : Backdrop.NONE));
    }

    static JsonObject backdropToJson(Backdrop backdrop) {
        JsonObject json = new JsonObject();
        json.addProperty("mode", backdrop.mode().id());
        json.addProperty("intensity", backdrop.intensity());
        json.addProperty("speed", backdrop.speed());
        json.addProperty("scale", backdrop.scale());
        return json;
    }

    static Backdrop parseBackdrop(JsonObject map, Backdrop fallback) {
        if (map == null || map.size() == 0) return fallback;
        return new Backdrop(Backdrop.Mode.byId(getString(map, "mode", fallback.mode().id())),
                getFloat(map, "intensity", fallback.intensity()), getFloat(map, "speed", fallback.speed()),
                getFloat(map, "scale", fallback.scale()));
    }

    static GradientSpec parseGradient(JsonObject map, GradientSpec fallback, int baseColor) {
        if (map == null || map.size() == 0) return fallback;
        boolean enabled = getBoolean(map, "enabled", fallback.enabled());
        int start = parseColor(map, "start", fallback.start() != 0 ? fallback.start() : baseColor);
        int end = parseColor(map, "end", fallback.end() != 0 ? fallback.end() : baseColor);
        float angle = normalizeAngle(getFloat(map, "angle", fallback.angleDeg()));
        return new GradientSpec(enabled, start, end, angle);
    }
}
