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
