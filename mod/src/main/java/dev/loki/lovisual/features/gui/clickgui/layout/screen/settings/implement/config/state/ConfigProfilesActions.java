package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config;

import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.service.ConfigProfileApplier;
import dev.loki.lovisual.config.profile.service.ConfigProfileService;
import dev.loki.lovisual.config.profile.service.ConfigProfileStorage;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.util.logging.DebugLog;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConfigProfilesActions {
    private static final DateTimeFormatter GENERATED_NAME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    private final ConfigProfilesState state;
    private final ConfigDiffPanelComponent diffPanel;

    ConfigProfilesActions(ConfigProfilesState state, ConfigDiffPanelComponent diffPanel) {
        this.state = state;
        this.diffPanel = diffPanel;
    }

    List<ConfigProfileMeta> filteredProfiles(List<ConfigProfileMeta> src) {
        if (!ClickGuiSearch.isActive() || ClickGuiSearch.getText().isBlank()) return src;
        List<ConfigProfileMeta> out = new ArrayList<>();
        String q = ClickGuiSearch.getText().toLowerCase(Locale.ROOT);
        for (ConfigProfileMeta meta : src) {
            if (meta.name().toLowerCase(Locale.ROOT).contains(q) || meta.author().toLowerCase(Locale.ROOT).contains(q)) out.add(meta);
        }
        return out;
    }

    void switchType(ConfigProfileType next) {
        if (state.type() == next) return;
        state.type(next);
        state.selected(null);
        state.selectedDiff(null);
        state.diffError(null);
        diffPanel.close();
        state.clearStatus();
        state.cancelRename();
        state.resetScroll();
        diffPanel.resetScroll();
    }

    void select(ConfigProfileMeta meta) {
        ConfigProfileMeta sel = state.selected();
        boolean same = sel != null && meta != null && sel.type() == meta.type() && sel.getId().equals(meta.getId());
        state.selected(meta);
        if (!same) {
            state.selectedDiff(null);
            state.diffError(null);
            diffPanel.close();
            diffPanel.resetScroll();
        }
    }

    void saveCurrent() {
        String name = state.type().folderName() + " " + GENERATED_NAME_TIME.format(LocalDateTime.now());
        try {
            diffPanel.close();
            ConfigProfileMeta saved = ConfigProfileService.INSTANCE.saveCurrent(state.type(), name).meta();
            state.selected(saved);
            state.selectedDiff(null);
            state.diffError(null);
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.saved", "Saved: %s", saved.name()));
        } catch (Exception e) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.save_failed", "Save failed"));
            DebugLog.error("Failed to save config profile", e);
        }
    }

    void overwriteSelected() {
        if (state.selected() == null) return;
        try {
            diffPanel.close();
            ConfigProfileMeta saved = ConfigProfileService.INSTANCE.overwriteCurrent(state.selected()).meta();
            state.selected(saved);
            state.selectedDiff(null);
            state.diffError(null);
            diffPanel.resetScroll();
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.overwritten", "Overwritten: %s", saved.name()));
        } catch (Exception e) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.overwrite_failed", "Overwrite failed"));
            DebugLog.error("Failed to overwrite config profile", e);
        }
    }

    void loadDiff(ConfigProfileMeta meta) {
        try {
            var diff = ConfigProfileService.INSTANCE.diffWithCurrent(meta.type(), meta.getId());
            state.selectedDiff(diff);
            state.diffError(null);
            diffPanel.open();
            diffPanel.resetScroll();
            state.setStatus(diff.isEmpty()
                    ? ClickGuiI18n.tr("clickgui.settings.config.status.no_differences", "No differences")
                    : ClickGuiI18n.tr("clickgui.settings.config.status.diff_count", "Diff: %s changes", diff.valueChangeCount()));
        } catch (Exception e) {
            state.selectedDiff(null);
            state.diffError(e.getMessage());
            diffPanel.open();
            diffPanel.resetScroll();
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.diff_failed", "Diff failed"));
            DebugLog.error("Failed to diff config profile", e);
        }
    }

    void apply(ConfigProfileMeta meta) {
        try {
            diffPanel.close();
            ConfigProfileApplier.ApplyResult result = ConfigProfileService.INSTANCE.apply(meta.type(), meta.getId());
            state.selectedDiff(null);
            state.diffError(null);
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.applied", "Applied: %s, missing: %s/%s", result.appliedOwners(), result.missingOwners(), result.missingValues()));
        } catch (Exception e) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.apply_failed", "Apply failed"));
            DebugLog.error("Failed to apply config profile", e);
        }
    }

    void delete(ConfigProfileMeta meta) {
        try {
            ConfigProfileService.INSTANCE.delete(meta.type(), meta.getId());
            ConfigProfileMeta sel = state.selected();
            if (sel != null && sel.getId().equals(meta.getId()) && sel.type() == meta.type()) {
                state.selected(null);
                state.selectedDiff(null);
                state.diffError(null);
                diffPanel.close();
            }
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.deleted", "Deleted: %s", meta.name()));
        } catch (Exception e) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.delete_failed", "Delete failed"));
            DebugLog.error("Failed to delete config profile", e);
        }
    }

    boolean importDroppedFiles(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return false;
        int imported = 0;
        int failed = 0;
        ConfigProfileMeta last = null;
        for (Path path : paths) {
            try {
                ConfigProfileStorage.ImportResult result = ConfigProfileService.INSTANCE.importProfileFile(path, false);
                imported++;
                last = result.meta();
            } catch (Exception e) {
                failed++;
                DebugLog.error("Failed to import dropped config profile %s", e, path == null ? "<null>" : path.toAbsolutePath());
            }
        }
        if (last != null) {
            switchType(last.type());
            state.selected(last);
        }
        if (imported > 0 && failed > 0) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.imported_partial", "Imported: %s, failed: %s", imported, failed));
        } else if (imported > 0) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.imported", "Imported: %s", imported));
        } else {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.import_failed", "Import failed"));
        }
        return true;
    }

    void commitRename() {
        if (!state.isRenaming()) return;
        String next = state.editingText() == null ? "" : state.editingText().trim();
        if (next.isBlank()) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.empty_name", "Name cannot be empty"));
            return;
        }
        try {
            ConfigProfileMeta oldSelected = state.selected();
            ConfigProfileMeta renamed = ConfigProfileService.INSTANCE.rename(state.editingType(), state.editingId(), next).meta();
            if (oldSelected != null && oldSelected.type() == state.editingType() && oldSelected.getId().equals(state.editingId())) {
                state.selected(renamed);
            }
            state.cancelRename();
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.renamed", "Renamed: %s", renamed.name()));
        } catch (Exception e) {
            state.setStatus(ClickGuiI18n.tr("clickgui.settings.config.status.rename_failed", "Rename failed"));
            DebugLog.error("Failed to rename config profile", e);
        }
    }
}
