/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.Comparator;

final class ModuleListData {

    private ModuleListData() {}

    static int collectEnabled(ModuleList host) {
        host.enabledModules.clear();
        int hash = 1;
        for (Module module : ModuleManager.getModules()) {
            if (module != null && module.isEnabled() && module.isShownInModuleList()) {
                host.enabledModules.add(module);
                String label = module.getDisplayName();
                hash = 31 * hash + System.identityHashCode(module);
                hash = 31 * hash + (label != null ? label.hashCode() : 0);
            }
        }
        return hash;
    }

    static boolean rebuildRowsIfNeeded(ModuleList host, TextRenderer listRenderer, float textScale, int modulesHash) {
        boolean layoutChanged = modulesHash != host.cachedModulesHash
                || host.enabledModules.size() != host.cachedModulesCount
                || Math.abs(textScale - host.cachedTextScale) > 0.0001f;
        if (!layoutChanged) return false;
        host.rows.clear();
        listRenderer.begin(textScale, false, false);
        for (Module module : host.enabledModules) {
            ModuleList.Row row = new ModuleList.Row(module, module.getDisplayName());
            row.textWidth = (float) listRenderer.getWidth(row.label, false);
            host.rows.add(row);
        }
        listRenderer.end();
        host.rows.sort(Comparator.comparingDouble((ModuleList.Row row) -> row.textWidth).reversed());
        host.cachedModulesHash = modulesHash;
        host.cachedModulesCount = host.enabledModules.size();
        host.cachedTextScale = textScale;
        return true;
    }

    static float computeMaxWidth(ModuleList host, float stringPad, float rectExtra) {
        float maxWidth = 0f;
        for (ModuleList.Row row : host.rows) {
            maxWidth = Math.max(maxWidth, row.textWidth + stringPad + rectExtra);
        }
        return maxWidth;
    }
}
