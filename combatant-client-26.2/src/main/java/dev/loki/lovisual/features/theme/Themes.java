/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.theme;

import net.minecraft.util.Util;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public enum Themes {
    ;

    public static final String THEME_CLASSIC = "classic";
    public static final String THEME_LEGACY = "legacy";
    public static final String THEME_AMBER = "amber";
    public static final String THEME_NOIR = "noir";
    public static final String THEME_PURPLE = "purple";
    public static final String THEME_BLUE = "blue";
    public static final String THEME_AZURE = "azure";
    public static final String THEME_NITRO = "nitro";
    public static final String THEME_SUNSET = "sunset";
    public static final String THEME_AURORA = "aurora";
    public static final String THEME_MINT = "mint";
    public static final String THEME_PEACH = "peach";
    public static final String THEME_LILAC = "lilac";
    public static final String THEME_SAND = "sand";
    public static final String THEME_DUSK = "dusk";
    public static final String THEME_PRISM = "prism";
    public static final String THEME_CINDER = "cinder";
    public static final String THEME_FOREST = "forest";
    private static ThemeEntry currentEntry = ThemePresets1.CLASSIC_ENTRY;
    private static ThemeEntry transitionFromEntry = ThemePresets1.CLASSIC_ENTRY;
    private static ThemeEntry animatedEntryCache = ThemePresets1.CLASSIC_ENTRY;

















    private static final Map<String, ThemeEntry> PRESET_ENTRIES = Map.ofEntries(
            Map.entry(THEME_CLASSIC, ThemePresets1.CLASSIC_ENTRY),
            Map.entry(THEME_LEGACY, ThemePresets1.LEGACY_ENTRY),
            Map.entry(THEME_AMBER, ThemePresets1.AMBER_ENTRY),
            Map.entry(THEME_NOIR, ThemePresets1.NOIR_ENTRY),
            Map.entry(THEME_PURPLE, ThemePresets1.PURPLE_ENTRY),
            Map.entry(THEME_BLUE, ThemePresets1.BLUE_ENTRY),
            Map.entry(THEME_AZURE, ThemePresets2.AZURE_ENTRY),
            Map.entry(THEME_NITRO, ThemePresets2.NITRO_ENTRY),
            Map.entry(THEME_SUNSET, ThemePresets2.SUNSET_ENTRY),
            Map.entry(THEME_AURORA, ThemePresets2.AURORA_ENTRY),
            Map.entry(THEME_MINT, ThemePresets2.MINT_ENTRY),
            Map.entry(THEME_PEACH, ThemePresets2.PEACH_ENTRY),
            Map.entry(THEME_LILAC, ThemePresets3.LILAC_ENTRY),
            Map.entry(THEME_SAND, ThemePresets3.SAND_ENTRY),
            Map.entry(THEME_DUSK, ThemePresets3.DUSK_ENTRY),
            Map.entry(THEME_PRISM, ThemePresets3.PRISM_ENTRY),
            Map.entry(THEME_CINDER, ThemePresets3.CINDER_ENTRY),
            Map.entry(THEME_FOREST, ThemePresets3.FOREST_ENTRY)
    );

    private static final ThemeStore STORE = ThemeStore.get();

    private static final long THEME_TRANSITION_MS = 340L;

    private static Theme current = ThemePresets1.CLASSIC;
    private static String currentId = THEME_CLASSIC;
    private static Theme transitionFromTheme = ThemePresets1.CLASSIC;
    private static long transitionStartMs = 0L;
    private static boolean transitionActive = false;
    private static Theme animatedThemeCache = ThemePresets1.CLASSIC;
    private static int animatedCacheKey = -1;

    static {
        applyThemeFromStore();
    }

    static Theme theme() {
        return animatedTheme();
    }

    static ThemeEntry currentEntry() {
        return animatedEntry();
    }

    static String currentId() {
        return currentId;
    }

    static List<ThemeEntry> themes() {
        return STORE.getThemes();
    }

    public static List<ThemeEntry> presetEntries() {
        List<ThemeEntry> out = new ArrayList<>();
        out.add(ThemePresets1.CLASSIC_ENTRY);
        out.add(ThemePresets1.LEGACY_ENTRY);
        out.add(ThemePresets1.AMBER_ENTRY);
        out.add(ThemePresets1.NOIR_ENTRY);
        out.add(ThemePresets1.PURPLE_ENTRY);
        out.add(ThemePresets1.BLUE_ENTRY);
        out.add(ThemePresets2.AZURE_ENTRY);
        out.add(ThemePresets2.NITRO_ENTRY);
        out.add(ThemePresets2.SUNSET_ENTRY);
        out.add(ThemePresets2.AURORA_ENTRY);
        out.add(ThemePresets2.MINT_ENTRY);
        out.add(ThemePresets2.PEACH_ENTRY);
        out.add(ThemePresets3.LILAC_ENTRY);
        out.add(ThemePresets3.SAND_ENTRY);
        out.add(ThemePresets3.DUSK_ENTRY);
        out.add(ThemePresets3.PRISM_ENTRY);
        out.add(ThemePresets3.CINDER_ENTRY);
        out.add(ThemePresets3.FOREST_ENTRY);
        return Collections.unmodifiableList(out);
    }

    public static ThemeEntry presetEntry(String id) {
        if (id == null) return null;
        return PRESET_ENTRIES.get(id.toLowerCase());
    }

    static List<ThemeEntry> customThemes() {
        return STORE.getCustomThemes();
    }

    static boolean isCustomTheme(String id) {
        return STORE.isCustomId(id);
    }

    static String uniqueCustomId(String name) {
        return STORE.uniqueCustomId(name);
    }

    static ThemeEntry saveCustomTheme(ThemeEntry entry, boolean select) {
        ThemeEntry saved = STORE.saveCustom(entry);
        if (saved != null && select) {
            setTheme(saved.getId());
        }
        return saved;
    }

    static ThemeEntry saveCustomTheme(ThemeEntry entry) {
        return saveCustomTheme(entry, currentId != null && entry != null && currentId.equals(entry.getId()));
    }

    static boolean deleteCustomTheme(String id) {
        boolean removed = STORE.deleteCustom(id);
        if (removed && (id == null || id.equals(currentId))) {
            setTheme(THEME_CLASSIC);
        }
        return removed;
    }

    static ThemeEntry createCustomTheme(String name, ThemeEntry base, boolean select) {
        ThemeEntry src = base != null ? base : currentEntry();
        if (src == null) src = ThemePresets1.CLASSIC_ENTRY;
        String displayName = name == null || name.isBlank() ? "Custom Theme" : name.trim();
        String id = STORE.uniqueCustomId(displayName);
        ThemeEntry entry = new ThemeEntry(
                id,
                displayName,
                false,
                src.theme(),
                src.windowGradient(),
                src.headerGradient(),
                src.surfaceGradient(),
                src.cardGradient(),
                src.strokeGradient()
        );
        return saveCustomTheme(entry, select);
    }

    public static Map<String, Object> toProfileValues(ThemeEntry entry) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (entry == null) return out;
        Theme t = entry.theme();
        out.put("name", entry.name());
        out.put("windowBg", colorString(t.windowBg()));
        out.put("windowHeader", colorString(t.windowHeader()));
        out.put("windowStroke", colorString(t.windowStroke()));
        out.put("surface", colorString(t.surface()));
        out.put("surfaceHover", colorString(t.surfaceHover()));
        out.put("cardEnabled", colorString(t.cardEnabled()));
        out.put("cardDisabled", colorString(t.cardDisabled()));
        out.put("textPrimary", colorString(t.textPrimary()));
        out.put("textMuted", colorString(t.textMuted()));
        out.put("accent", colorString(t.accent()));
        out.put("accentSoft", colorString(t.accentSoft()));
        out.put("strokeSoft", colorString(t.strokeSoft()));
        writeGradient(out, "window", entry.windowGradient());
        writeGradient(out, "header", entry.headerGradient());
        writeGradient(out, "surface", entry.surfaceGradient());
        writeGradient(out, "card", entry.cardGradient());
        writeGradient(out, "stroke", entry.strokeGradient());
        return out;
    }

    public static ThemeEntry fromProfileValues(String id, String fallbackName, Map<String, Object> values) {
        String key = id == null || id.isBlank() ? uniqueCustomId(fallbackName) : id.toLowerCase();
        ThemeEntry existing = STORE.getById(key);
        ThemeEntry baseEntry = existing != null ? existing : ThemePresets1.CLASSIC_ENTRY;
        Theme base = baseEntry.theme();
        Map<String, Object> map = values == null ? Map.of() : values;
        String name = stringValue(map.get("name"), fallbackName == null || fallbackName.isBlank() ? key : fallbackName);
        Theme theme = new Theme(
                colorValue(map.get("windowBg"), base.windowBg()),
                colorValue(map.get("windowHeader"), base.windowHeader()),
                colorValue(map.get("windowStroke"), base.windowStroke()),
                colorValue(map.get("surface"), base.surface()),
                colorValue(map.get("surfaceHover"), base.surfaceHover()),
                colorValue(map.get("cardEnabled"), base.cardEnabled()),
                colorValue(map.get("cardDisabled"), base.cardDisabled()),
                colorValue(map.get("textPrimary"), base.textPrimary()),
                colorValue(map.get("textMuted"), base.textMuted()),
                colorValue(map.get("accent"), base.accent()),
                colorValue(map.get("accentSoft"), base.accentSoft()),
                colorValue(map.get("strokeSoft"), base.strokeSoft())
        );
        return new ThemeEntry(
                key,
                name,
                false,
                theme,
                readGradient(map, "window", baseEntry.windowGradient(), theme.windowBg()),
                readGradient(map, "header", baseEntry.headerGradient(), theme.windowHeader()),
                readGradient(map, "surface", baseEntry.surfaceGradient(), theme.surface()),
                readGradient(map, "card", baseEntry.cardGradient(), theme.cardEnabled()),
                readGradient(map, "stroke", baseEntry.strokeGradient(), theme.windowStroke())
        );
    }

    public static ThemeEntry importProfileValues(String id, String fallbackName, Map<String, Object> values) {
        ThemeEntry entry = fromProfileValues(id, fallbackName, values);
        return saveCustomTheme(entry, false);
    }

    private static void writeGradient(Map<String, Object> out, String prefix, GradientSpec spec) {
        GradientSpec g = spec != null ? spec : flatGradient(ThemePresets1.CLASSIC.accent());
        out.put(prefix + "GradientEnabled", g.enabled());
        out.put(prefix + "GradientStart", colorString(g.start()));
        out.put(prefix + "GradientEnd", colorString(g.end()));
        out.put(prefix + "GradientAngle", g.angleDeg());
    }

    private static GradientSpec readGradient(Map<String, Object> map, String prefix, GradientSpec fallback, int baseColor) {
        GradientSpec fb = fallback != null ? fallback : flatGradient(baseColor);
        boolean enabled = booleanValue(map.get(prefix + "GradientEnabled"), fb.enabled());
        int start = colorValue(map.get(prefix + "GradientStart"), fb.start());
        int end = colorValue(map.get(prefix + "GradientEnd"), fb.end());
        float angle = floatValue(map.get(prefix + "GradientAngle"), fb.angleDeg());
        return new GradientSpec(enabled, start, end, angle);
    }

    public static String colorString(int argb) {
        return String.format("#%08X", argb);
    }

    public static int colorValue(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        if (raw instanceof CharSequence s) return ThemeStore.parseColorString(s.toString(), fallback);
        return fallback;
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
            }
        }
        return fallback;
    }

    static void setTheme(String id) {
        if (id == null || id.isBlank()) {
            boolean animate = !THEME_CLASSIC.equals(currentId) || transitionActive;
            applyThemeFromEntry(ThemePresets1.CLASSIC_ENTRY, animate);
            STORE.setCurrentId(THEME_CLASSIC);
            return;
        }
        String key = id.toLowerCase();
        ThemeEntry entry = STORE.getById(key);
        if (entry == null) entry = PRESET_ENTRIES.get(key);
        if (entry == null) entry = ThemePresets1.CLASSIC_ENTRY;
        boolean animate = !entry.getId().equals(currentId) || transitionActive;
        applyThemeFromEntry(entry, animate);
        STORE.setCurrentId(entry.getId());
    }

    static void setTheme(Theme t) {
        if (t == null) {
            setTheme(THEME_CLASSIC);
            return;
        }
        for (ThemeEntry entry : PRESET_ENTRIES.values()) {
            if (entry.theme() == t) {
                setTheme(entry.getId());
                return;
            }
        }
        Theme from = displayedThemeRaw();
        ThemeEntry fromEntry = displayedEntryRaw();
        current = t;
        currentId = THEME_CLASSIC;
        currentEntry = new ThemeEntry(
                THEME_CLASSIC,
                "Custom",
                false,
                t,
                flatGradient(t.windowBg()),
                subtleHeaderGradient(t.windowHeader()),
                flatGradient(t.surface()),
                flatGradient(t.cardEnabled()),
                flatGradient(t.windowStroke())
        );
        beginThemeTransition(from, fromEntry);
    }

    static Theme classic() {
        return ThemePresets1.CLASSIC;
    }

    private static void applyThemeFromStore() {
        String id = STORE.getCurrentId();
        ThemeEntry entry = STORE.getById(id);
        if (entry == null) entry = PRESET_ENTRIES.get(THEME_CLASSIC);
        if (entry == null) entry = ThemePresets1.CLASSIC_ENTRY;
        applyThemeFromEntry(entry, false);
    }

    private static void applyThemeFromEntry(ThemeEntry entry, boolean animate) {
        if (entry == null) entry = ThemePresets1.CLASSIC_ENTRY;
        if (animate) {
            beginThemeTransition(displayedThemeRaw(), displayedEntryRaw());
        } else {
            transitionActive = false;
            animatedCacheKey = -1;
        }
        current = entry.theme();
        currentId = entry.getId();
        currentEntry = entry;
        if (!animate) {
            animatedThemeCache = current;
            animatedEntryCache = currentEntry;
        }
    }

    private static void beginThemeTransition(Theme fromTheme, ThemeEntry fromEntry) {
        transitionFromTheme = fromTheme != null ? fromTheme : current;
        transitionFromEntry = fromEntry != null ? fromEntry : currentEntry;
        transitionStartMs = Util.getMillis();
        transitionActive = true;
        animatedCacheKey = -1;
    }

    private static Theme displayedThemeRaw() {
        if (!transitionActive) return current;
        return animatedTheme();
    }

    private static ThemeEntry displayedEntryRaw() {
        if (!transitionActive) return currentEntry;
        return animatedEntry();
    }

    private static Theme animatedTheme() {
        if (!transitionActive) return current;
        float progress = transitionProgress();
        if (!transitionActive) return current;
        int key = Math.round(progress * 1024.0f);
        if (key == animatedCacheKey && animatedThemeCache != null) return animatedThemeCache;
        animatedThemeCache = blendTheme(transitionFromTheme, current, progress);
        animatedEntryCache = null;
        animatedCacheKey = key;
        return animatedThemeCache;
    }

    private static ThemeEntry animatedEntry() {
        if (!transitionActive) return currentEntry;
        float progress = transitionProgress();
        if (!transitionActive) return currentEntry;
        int key = Math.round(progress * 1024.0f);
        if (key != animatedCacheKey || animatedThemeCache == null) {
            animatedThemeCache = blendTheme(transitionFromTheme, current, progress);
            animatedEntryCache = null;
            animatedCacheKey = key;
        }
        if (animatedEntryCache != null) return animatedEntryCache;
        animatedEntryCache = new ThemeEntry(
                currentEntry.getId(),
                currentEntry.name(),
                currentEntry.builtin(),
                animatedThemeCache,
                blendGradient(transitionFromEntry.windowGradient(), currentEntry.windowGradient(), progress),
                blendGradient(transitionFromEntry.headerGradient(), currentEntry.headerGradient(), progress),
                blendGradient(transitionFromEntry.surfaceGradient(), currentEntry.surfaceGradient(), progress),
                blendGradient(transitionFromEntry.cardGradient(), currentEntry.cardGradient(), progress),
                blendGradient(transitionFromEntry.strokeGradient(), currentEntry.strokeGradient(), progress)
        );
        return animatedEntryCache;
    }

    private static float transitionProgress() {
        long elapsed = Math.max(0L, Util.getMillis() - transitionStartMs);
        if (elapsed >= THEME_TRANSITION_MS) {
            transitionActive = false;
            animatedThemeCache = current;
            animatedEntryCache = currentEntry;
            animatedCacheKey = -1;
            return 1.0f;
        }
        float linear = THEME_TRANSITION_MS <= 0L ? 1.0f : (float) elapsed / (float) THEME_TRANSITION_MS;
        return AnimationUtility.easeOutCubic(AnimationUtility.clamp01(linear));
    }

    private static Theme blendTheme(Theme from, Theme to, float t) {
        if (from == null) from = ThemePresets1.CLASSIC;
        if (to == null) to = ThemePresets1.CLASSIC;
        return new Theme(
                mixArgb(from.windowBg(), to.windowBg(), t),
                mixArgb(from.windowHeader(), to.windowHeader(), t),
                mixArgb(from.windowStroke(), to.windowStroke(), t),
                mixArgb(from.surface(), to.surface(), t),
                mixArgb(from.surfaceHover(), to.surfaceHover(), t),
                mixArgb(from.cardEnabled(), to.cardEnabled(), t),
                mixArgb(from.cardDisabled(), to.cardDisabled(), t),
                mixArgb(from.textPrimary(), to.textPrimary(), t),
                mixArgb(from.textMuted(), to.textMuted(), t),
                mixArgb(from.accent(), to.accent(), t),
                mixArgb(from.accentSoft(), to.accentSoft(), t),
                mixArgb(from.strokeSoft(), to.strokeSoft(), t)
        );
    }

    private static GradientSpec blendGradient(GradientSpec from, GradientSpec to, float t) {
        if (from == null) from = flatGradient(ThemePresets1.CLASSIC.accent());
        if (to == null) to = flatGradient(ThemePresets1.CLASSIC.accent());
        boolean enabled = from.enabled() || to.enabled();
        int start = mixArgb(from.start(), to.start(), t);
        int end = mixArgb(from.end(), to.end(), t);
        float angle = mixAngle(from.angleDeg(), to.angleDeg(), t);
        return new GradientSpec(enabled, start, end, angle);
    }

    private static int mixArgb(int from, int to, float t) {
        t = AnimationUtility.clamp01(t);
        int fa = (from >>> 24) & 0xFF;
        int fr = (from >>> 16) & 0xFF;
        int fg = (from >>> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF;
        int tr = (to >>> 16) & 0xFF;
        int tg = (to >>> 8) & 0xFF;
        int tb = to & 0xFF;
        int a = Math.round(fa + (ta - fa) * t);
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (clampChannel(a) << 24) | (clampChannel(r) << 16) | (clampChannel(g) << 8) | clampChannel(b);
    }

    private static float mixAngle(float from, float to, float t) {
        float delta = ((to - from + 540.0f) % 360.0f) - 180.0f;
        float out = from + delta * AnimationUtility.clamp01(t);
        out %= 360.0f;
        return out < 0.0f ? out + 360.0f : out;
    }

    static GradientSpec flatGradient(int color) {
        return new GradientSpec(false, color, color, 90f);
    }

    static GradientSpec subtleHeaderGradient(int color) {
        int start = adjustBrightness(color, 0.08f);
        int end = adjustBrightness(color, -0.06f);
        return new GradientSpec(true, start, end, 90f);
    }

    private static int adjustBrightness(int argb, float delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        r = clampChannel(r + Math.round((delta >= 0 ? (255 - r) : r) * delta));
        g = clampChannel(g + Math.round((delta >= 0 ? (255 - g) : g) * delta));
        b = clampChannel(b + Math.round((delta >= 0 ? (255 - b) : b) * delta));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clampChannel(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    public record Theme(
            int windowBg,
            int windowHeader,
            int windowStroke,
            int surface,
            int surfaceHover,
            int cardEnabled,
            int cardDisabled,
            int textPrimary,
            int textMuted,
            int accent,
            int accentSoft,
            int strokeSoft
    ) {
    }

    public record GradientSpec(boolean enabled, int start, int end, float angleDeg) {
    }

    public record ThemeEntry(
            String id,
            String name,
            boolean builtin,
            Theme theme,
            GradientSpec windowGradient,
            GradientSpec headerGradient,
            GradientSpec surfaceGradient,
            GradientSpec cardGradient,
            GradientSpec strokeGradient
    ) {
        public String getId() {
            return id;
        }
    }
}
