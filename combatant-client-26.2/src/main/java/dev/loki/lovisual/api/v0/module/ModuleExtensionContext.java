/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.module;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.features.module.core.Module;

public interface ModuleExtensionContext {
    String addonId();

    String moduleId();

    Module module();

    void addSetting(SettingDef setting);

    ConfigValue<?> getConfigValue(String configName);

    boolean addModeOption(String configName, String option);
}
