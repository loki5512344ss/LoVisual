/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.client;

import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;

import java.util.List;

public final class LoVisualClientApi {
    private static final LoVisualClientApi INSTANCE = new LoVisualClientApi();

    private LoVisualClientApi() {
    }

    public static LoVisualClientApi get() {
        return INSTANCE;
    }

    public List<Module> modules() {
        return ModuleManager.getModules();
    }

    public Module module(String id) {
        return ModuleManager.get(id);
    }

    public boolean isModuleEnabled(String id) {
        return ModuleManager.isEnabled(id);
    }

    public void setModuleEnabled(String id, boolean enabled) {
        ModuleManager.setEnabled(id, enabled);
    }
}
