/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.clickgui.settings.Setting;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

enum ModulesMenuResolver {
    ;

    static List<ModuleComponent.CardEntry> buildCards(ModulesMenuCategory category) {
        List<ModuleComponent.CardEntry> out = new ArrayList<>();
        if (category == null) return out;

        for (Module module : ModuleManager.getModules()) {
            if (module == null) continue;
            if (!category.matches(module.getCategory())) continue;
            String bind = module.getKeyBindSetting() != null && module.getKeyBindSetting().getValue() != null
                    ? module.getKeyBindSetting().getValue().get()
                    : "";
            out.add(new ModuleComponent.CardEntry(
                    module.name(),
                    module.getDisplayName(),
                    "",
                    bind == null ? "" : bind,
                    !module.getSettings().isEmpty(),
                    module.isEnabled(),
                    true,
                    module.isShownInModuleList(),
                    module.getAliases()
            ));
        }

        out.sort(Comparator.comparing(ModuleComponent.CardEntry::title, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    static void toggleEntry(String id) {
        Module module = moduleById(id);
        if (module == null) return;
        module.toggle();
    }

    static void beginBind(String id) {
        Module module = moduleById(id);
        if (module == null || module.getKeyBindSetting() == null) return;
        ClickGuiRenderer.beginKeyBind(module.getKeyBindSetting());
    }

    static void toggleModuleListVisibility(String id) {
        Module module = moduleById(id);
        if (module == null) return;
        module.toggleShownInModuleList();
    }

    static ResolvedSettings resolveSettings(String id) {
        Module module = moduleById(id);
        if (module == null) return null;
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty()) return null;
        for (Setting setting : settings) {
            if (setting == null) continue;
            setting.preflightI18n();
        }
        return new ResolvedSettings(module.name(), module.getDisplayName(), settings);
    }

    static Module moduleById(String id) {
        if (id == null || id.isBlank()) return null;
        return ModuleManager.get(id);
    }

    record ResolvedSettings(String id, String title, List<Setting> settings) {
        public String getId() {
            return id;
        }
    }
}
