package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.config.profile.service.ConfigProfileStorage;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent registry of custom themes. Owns the merged (presets + custom) lookup
 * index used by {@link Themes} and exposes the CRUD API consumed by the clickgui
 * theme editor. All disk access is delegated to {@link ThemeStoreIO}; the JSON
 * shape lives in {@link ThemeStoreJson}.
 */
public final class ThemeStore {

    private static final ThemeStore INSTANCE = new ThemeStore();

    // Package-private so ThemeStoreIO can populate / rebuild them during bootstrap.
    final List<ThemeEntry> themes = new ArrayList<>();
    final List<ThemeEntry> customThemes = new ArrayList<>();
    final Map<String, ThemeEntry> themesById = new LinkedHashMap<>();
    String currentId = Themes.THEME_DEFAULT;

    private ThemeStore() {
        ThemeStoreIO.load(this);
    }

    public static ThemeStore get() {
        return INSTANCE;
    }

    public List<ThemeEntry> getThemes() {
        return Collections.unmodifiableList(themes);
    }

    public List<ThemeEntry> getCustomThemes() {
        return Collections.unmodifiableList(customThemes);
    }

    public String getCurrentId() {
        return currentId;
    }

    /** Update the active theme id and persist to {@code state.json}. Blank / unchanged ids are ignored. */
    public void setCurrentId(String id) {
        if (id == null || id.isBlank()) return;
        String key = ThemeStoreJson.normalizeId(id);
        if (key.equals(currentId)) return;
        currentId = key;
        ThemeStoreIO.saveState(this);
    }

    public ThemeEntry getById(String id) {
        return id == null ? null : themesById.get(ThemeStoreJson.normalizeId(id));
    }

    public boolean isCustomId(String id) {
        String key = ThemeStoreJson.normalizeId(id);
        if (key.isEmpty()) return false;
        for (ThemeEntry entry : customThemes) {
            if (entry.getId().equals(key)) return true;
        }
        return false;
    }

    /** Sanitise {@code requestedName} and append {@code _2}, {@code _3}, ... until the id is unused. */
    public String uniqueCustomId(String requestedName) {
        String base = ConfigProfileStorage.sanitizeFileName(requestedName == null || requestedName.isBlank() ? "custom_theme" : requestedName);
        if (base.isBlank()) base = "custom_theme";
        String candidate = ThemeStoreJson.normalizeId(base);
        int i = 2;
        while (Themes.presetEntry(candidate) != null || themesById.containsKey(candidate)) {
            candidate = ThemeStoreJson.normalizeId(base + "_" + i++);
        }
        return candidate;
    }

    /** Insert or replace a custom entry, keeping the alphabetical order stable, and write it to disk. */
    public ThemeEntry saveCustom(ThemeEntry entry) {
        if (entry == null) return null;
        String id = ThemeStoreJson.normalizeId(entry.getId());
        if (id.isBlank()) return null;
        if (Themes.presetEntry(id) != null) id = uniqueCustomId(entry.name());
        ThemeEntry normalized = new ThemeEntry(id,
                entry.name() == null || entry.name().isBlank() ? id : entry.name().trim(),
                false, entry.theme(), entry.windowGradient(), entry.headerGradient(),
                entry.surfaceGradient(), entry.cardGradient(), entry.strokeGradient());
        boolean replaced = false;
        for (int i = 0; i < customThemes.size(); i++) {
            if (customThemes.get(i).getId().equals(id)) {
                customThemes.set(i, normalized);
                replaced = true;
                break;
            }
        }
        if (!replaced) customThemes.add(normalized);
        customThemes.sort(Comparator.comparing(ThemeEntry::name, String.CASE_INSENSITIVE_ORDER));
        ThemeStoreIO.rebuildMergedIndex(this);
        ThemeStoreIO.saveCustomFile(normalized);
        return normalized;
    }

    /** Drop a custom entry, delete its file and fall back to classic if it was the active one. */
    public boolean deleteCustom(String id) {
        String key = ThemeStoreJson.normalizeId(id);
        if (key.isBlank()) return false;
        boolean removed = customThemes.removeIf(entry -> entry.getId().equals(key));
        if (!removed) return false;
        ThemeStoreIO.deleteCustomFile(key);
        if (key.equals(currentId)) currentId = Themes.THEME_CLASSIC;
        ThemeStoreIO.rebuildMergedIndex(this);
        ThemeStoreIO.saveState(this);
        return true;
    }

    public static int parseColorString(String s, int fallback) {
        return ThemeStoreJson.parseColorString(s, fallback);
    }

    static String colorToString(int argb) {
        return ThemeStoreJson.colorToString(argb);
    }
}
