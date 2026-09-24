package dev.loki.lovisual.features.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.loki.lovisual.config.io.ConfigPaths;
import dev.loki.lovisual.config.profile.service.ConfigProfileStorage;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;
import dev.loki.lovisual.util.logging.DebugLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Filesystem access for {@link ThemeStore}: layout of the {@code themes/} folder,
 * bootstrap / legacy-migration sequence and the {@code state.json} + per-custom-file
 * writers. Kept out of the store class so the main theme directory stays within the
 * four-file-per-directory limit; the package declaration is unchanged.
 */
final class ThemeStoreIO {

    static final Path LEGACY_FILE = ConfigPaths.root().resolve("clickgui_themes.json");
    static final Path THEME_ROOT = ConfigPaths.root().resolve("themes");
    static final Path CUSTOM_DIR = THEME_ROOT.resolve("custom");
    static final Path STATE_FILE = THEME_ROOT.resolve("state.json");
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ThemeStoreIO() { }

    /** Read persisted state + custom files, migrate the legacy single-file format and rebuild the merged index. */
    static void load(ThemeStore store) {
        store.currentId = loadCurrentId();
        loadCustomThemes(store);
        migrateLegacyCustomThemes(store);
        retireLegacyFile(store);
        rebuildMergedIndex(store);
        if (store.getById(store.currentId) == null) {
            store.currentId = Themes.THEME_CLASSIC;
            saveState(store);
        }
    }

    private static String loadCurrentId() {
        String fromState = readCurrentId(STATE_FILE);
        if (fromState != null) return fromState;
        String fromLegacy = readCurrentId(LEGACY_FILE);
        return fromLegacy != null ? fromLegacy : Themes.THEME_CLASSIC;
    }

    private static String readCurrentId(Path file) {
        if (!Files.exists(file)) return null;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file));
            if (parsed != null && parsed.isJsonObject()) {
                String current = ThemeStoreJson.getString(parsed.getAsJsonObject(), "current", null);
                if (current != null && !current.isBlank()) return ThemeStoreJson.normalizeId(current);
            }
        } catch (Exception e) {
            DebugLog.error("ClickGuiThemeStore: state load failed", e);
        }
        return null;
    }

    private static void loadCustomThemes(ThemeStore store) {
        store.customThemes.clear();
        if (!Files.isDirectory(CUSTOM_DIR)) return;
        try (var stream = Files.list(CUSTOM_DIR)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            JsonElement parsed = JsonParser.parseString(Files.readString(path));
                            if (parsed == null || !parsed.isJsonObject()) return;
                            ThemeEntry entry = ThemeStoreJson.parseEntry(parsed.getAsJsonObject(), false);
                            if (entry == null || Themes.presetEntry(entry.getId()) != null) return;
                            store.customThemes.removeIf(existing -> existing.getId().equals(entry.getId()));
                            store.customThemes.add(entry);
                        } catch (Exception e) {
                            DebugLog.error("ClickGuiThemeStore: custom theme load failed %s", e, path.toAbsolutePath());
                        }
                    });
        } catch (IOException e) {
            DebugLog.error("ClickGuiThemeStore: custom theme list failed", e);
        }
        store.customThemes.sort(Comparator.comparing(ThemeEntry::name, String.CASE_INSENSITIVE_ORDER));
    }

    private static void migrateLegacyCustomThemes(ThemeStore store) {
        if (!Files.exists(LEGACY_FILE)) return;
        boolean changed = false;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(LEGACY_FILE));
            if (parsed == null || !parsed.isJsonObject()) return;
            var arr = ThemeStoreJson.getArray(parsed.getAsJsonObject(), "themes");
            if (arr == null) return;
            rebuildMergedIndex(store);
            for (JsonElement item : arr) {
                if (item == null || !item.isJsonObject()) continue;
                ThemeEntry entry = ThemeStoreJson.parseEntry(item.getAsJsonObject(), false);
                if (entry == null || Themes.presetEntry(entry.getId()) != null || store.isCustomId(entry.getId())) continue;
                store.saveCustom(entry);
                changed = true;
            }
        } catch (Exception e) {
            DebugLog.error("ClickGuiThemeStore: legacy custom migration failed", e);
        }
        if (changed) saveState(store);
    }

    private static void retireLegacyFile(ThemeStore store) {
        if (!Files.exists(LEGACY_FILE)) return;
        try {
            Files.deleteIfExists(LEGACY_FILE);
            saveState(store);
        } catch (IOException e) {
            DebugLog.error("ClickGuiThemeStore: legacy file cleanup failed", e);
        }
    }

    /** Merge presets + custom themes into the flat lookup index used by {@code getById}. */
    static void rebuildMergedIndex(ThemeStore store) {
        store.themes.clear();
        store.themesById.clear();
        for (ThemeEntry preset : Themes.presetEntries()) {
            store.themes.add(preset);
            store.themesById.put(preset.getId(), preset);
        }
        for (ThemeEntry custom : store.customThemes) {
            if (custom == null) continue;
            store.themes.add(custom);
            store.themesById.put(custom.getId(), custom);
        }
    }

    static void saveState(ThemeStore store) {
        try {
            Files.createDirectories(THEME_ROOT);
            var root = new com.google.gson.JsonObject();
            root.addProperty("current", store.currentId);
            Files.writeString(STATE_FILE, GSON.toJson(root) + "\n");
        } catch (IOException e) {
            DebugLog.error("ClickGuiThemeStore: state save failed", e);
        }
    }

    static void saveCustomFile(ThemeEntry entry) {
        try {
            Files.createDirectories(CUSTOM_DIR);
            Files.writeString(customFile(entry.getId()), GSON.toJson(ThemeStoreJson.entryToJson(entry)) + "\n");
        } catch (IOException e) {
            DebugLog.error("ClickGuiThemeStore: custom theme save failed", e);
        }
    }

    static void deleteCustomFile(String id) {
        try {
            Files.deleteIfExists(customFile(id));
        } catch (IOException e) {
            DebugLog.error("ClickGuiThemeStore: delete custom theme failed", e);
        }
    }

    static Path customFile(String id) {
        return CUSTOM_DIR.resolve(ConfigProfileStorage.sanitizeFileName(id) + ".json");
    }
}
