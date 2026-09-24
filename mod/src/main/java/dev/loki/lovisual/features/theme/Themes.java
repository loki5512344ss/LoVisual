package dev.loki.lovisual.features.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static entry point for the theme system: current theme accessors, the fixed
 * preset catalogue, custom-theme CRUD and profile import / export.
 * <p>
 * Mutable state and helpers are split across three sibling files that share the
 * same Java package: {@link ThemesTransition} (current pointer + cross-fade
 * animation), {@link ThemesBlending} (stateless color / gradient math) and
 * {@link ThemesProfileCodec} (flat-map profile serialisation).
 */
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

    static final long THEME_TRANSITION_MS = 340L;
    static final ThemeStore STORE = ThemeStore.get();

    /** Display order for the preset picker; also drives {@link #PRESET_ENTRIES}. */
    static final List<ThemeEntry> PRESET_LIST = List.of(
            ThemePresets1.CLASSIC_ENTRY, ThemePresets1.LEGACY_ENTRY, ThemePresets1.AMBER_ENTRY,
            ThemePresets1.NOIR_ENTRY, ThemePresets1.PURPLE_ENTRY, ThemePresets1.BLUE_ENTRY,
            ThemePresets2.AZURE_ENTRY, ThemePresets2.NITRO_ENTRY, ThemePresets2.SUNSET_ENTRY,
            ThemePresets2.AURORA_ENTRY, ThemePresets2.MINT_ENTRY, ThemePresets2.PEACH_ENTRY,
            ThemePresets3.LILAC_ENTRY, ThemePresets3.SAND_ENTRY, ThemePresets3.DUSK_ENTRY,
            ThemePresets3.PRISM_ENTRY, ThemePresets3.CINDER_ENTRY, ThemePresets3.FOREST_ENTRY
    );
    static final Map<String, ThemeEntry> PRESET_ENTRIES = buildPresetMap(PRESET_LIST);

    private static Map<String, ThemeEntry> buildPresetMap(List<ThemeEntry> list) {
        Map<String, ThemeEntry> m = new LinkedHashMap<>();
        for (ThemeEntry e : list) m.put(e.getId(), e);
        return Collections.unmodifiableMap(m);
    }

    // Mutable transition state; read / written by ThemesTransition (same package).
    static ThemeEntry currentEntry = ThemePresets1.CLASSIC_ENTRY;
    static ThemeEntry transitionFromEntry = ThemePresets1.CLASSIC_ENTRY;
    static ThemeEntry animatedEntryCache = ThemePresets1.CLASSIC_ENTRY;
    static Theme current = ThemePresets1.CLASSIC;
    static String currentId = THEME_CLASSIC;
    static Theme transitionFromTheme = ThemePresets1.CLASSIC;
    static long transitionStartMs = 0L;
    static boolean transitionActive = false;
    static Theme animatedThemeCache = ThemePresets1.CLASSIC;
    static int animatedCacheKey = -1;

    static {
        ThemesTransition.applyFromStore();
    }

    static Theme theme() { return ThemesTransition.theme(); }
    static ThemeEntry currentEntry() { return ThemesTransition.currentEntry(); }
    static String currentId() { return currentId; }
    static List<ThemeEntry> themes() { return STORE.getThemes(); }
    static List<ThemeEntry> customThemes() { return STORE.getCustomThemes(); }
    static boolean isCustomTheme(String id) { return STORE.isCustomId(id); }
    static String uniqueCustomId(String name) { return STORE.uniqueCustomId(name); }
    static Theme classic() { return ThemePresets1.CLASSIC; }
    static void setTheme(String id) { ThemesTransition.setTheme(id); }
    static void setTheme(Theme t) { ThemesTransition.setTheme(t); }
    static GradientSpec flatGradient(int color) { return ThemesBlending.flatGradient(color); }
    static GradientSpec subtleHeaderGradient(int color) { return ThemesBlending.subtleHeaderGradient(color); }

    /** Insertion-ordered view of the 18 built-in presets; used by the picker. */
    public static List<ThemeEntry> presetEntries() { return PRESET_LIST; }

    public static ThemeEntry presetEntry(String id) {
        return id == null ? null : PRESET_ENTRIES.get(id.toLowerCase());
    }

    /** Persist (or update) a custom entry; optionally switch to it immediately. */
    static ThemeEntry saveCustomTheme(ThemeEntry entry, boolean select) {
        ThemeEntry saved = STORE.saveCustom(entry);
        if (saved != null && select) setTheme(saved.getId());
        return saved;
    }

    static ThemeEntry saveCustomTheme(ThemeEntry entry) {
        return saveCustomTheme(entry, currentId != null && entry != null && currentId.equals(entry.getId()));
    }

    /** Remove a custom theme; if it was the active one, snap back to classic. */
    static boolean deleteCustomTheme(String id) {
        boolean removed = STORE.deleteCustom(id);
        if (removed && (id == null || id.equals(currentId))) setTheme(THEME_CLASSIC);
        return removed;
    }

    /** Clone the base (or current) entry under a new "custom" id and, optionally, activate it. */
    static ThemeEntry createCustomTheme(String name, ThemeEntry base, boolean select) {
        ThemeEntry src = base != null ? base : currentEntry();
        if (src == null) src = ThemePresets1.CLASSIC_ENTRY;
        String displayName = name == null || name.isBlank() ? "Custom Theme" : name.trim();
        String id = STORE.uniqueCustomId(displayName);
        ThemeEntry entry = new ThemeEntry(id, displayName, false, src.theme(),
                src.windowGradient(), src.headerGradient(), src.surfaceGradient(), src.cardGradient(), src.strokeGradient());
        return saveCustomTheme(entry, select);
    }

    public static Map<String, Object> toProfileValues(ThemeEntry entry) {
        return ThemesProfileCodec.toProfileValues(entry);
    }

    public static ThemeEntry fromProfileValues(String id, String fallbackName, Map<String, Object> values) {
        return ThemesProfileCodec.fromProfileValues(id, fallbackName, values);
    }

    public static ThemeEntry importProfileValues(String id, String fallbackName, Map<String, Object> values) {
        return saveCustomTheme(fromProfileValues(id, fallbackName, values), false);
    }

    public static String colorString(int argb) {
        return String.format("#%08X", argb);
    }

    public static int colorValue(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        if (raw instanceof CharSequence s) return ThemeStore.parseColorString(s.toString(), fallback);
        return fallback;
    }

    /** The 12 semantic color slots every theme must define. */
    public record Theme(
            int windowBg, int windowHeader, int windowStroke,
            int surface, int surfaceHover,
            int cardEnabled, int cardDisabled,
            int textPrimary, int textMuted,
            int accent, int accentSoft, int strokeSoft
    ) { }

    /** A single gradient stop: enabled flag, two endpoint colors and a direction in degrees. */
    public record GradientSpec(boolean enabled, int start, int end, float angleDeg) { }

    /** Full theme definition: color palette plus the five named gradient slots used by the clickgui. */
    public record ThemeEntry(
            String id, String name, boolean builtin, Theme theme,
            GradientSpec windowGradient, GradientSpec headerGradient,
            GradientSpec surfaceGradient, GradientSpec cardGradient, GradientSpec strokeGradient
    ) {
        public String getId() {
            return id;
        }
    }
}
