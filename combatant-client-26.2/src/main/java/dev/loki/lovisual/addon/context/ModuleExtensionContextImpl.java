/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.addon.context;

import dev.loki.lovisual.api.v0.module.ModuleExtensionContext;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.features.module.core.Module;

public final class ModuleExtensionContextImpl implements ModuleExtensionContext {
    private final String addonId;
    private final Module module;

    public ModuleExtensionContextImpl(String addonId, Module module) {

        this.addonId = addonId;
        this.module = module;
    }

    @Override
    public String addonId() {
        return addonId;
    }

    @Override
    public String moduleId() {
        return module.name();
    }

    @Override
    public Module module() {
        return module;
    }

    @Override
    public void addSetting(SettingDef setting) {
        module.addExtensionSetting(setting);
    }

    @Override
    public ConfigValue<?> getConfigValue(String configName) {
        return module.getConfigValue(configName);
    }

    @Override
    public boolean addModeOption(String configName, String option) {
        ConfigValue<?> value = getConfigValue(configName);
        return value instanceof ModeValue mode && mode.addOption(option);
    }
}
