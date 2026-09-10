/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.module;

import de.marhali.json5.Json5Object;
import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.module.lifecycle.ModuleActivationSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.loki.lovisual.features.module.core.Module;

/**
 * Config plumbing for modules: the ordered config value collection, legacy settings
 * application, post-load value sync and the quiet save request used across Module.
 */
public final class ModuleConfigValues {

    public static final String SETTING_SHOW_IN_MODULE_LIST = "show_in_module_list";
    public static final String LEGACY_SETTING_SHOW_IN_ACTIVE_MODULES = "show_in_active_modules";

    private ModuleConfigValues() {
    }

    public static List<ConfigValue<?>> orderedValues(BooleanValue enabledValue,
                                                     EnumValue<ModuleActivationSource> activationSourceValue,
                                                     BooleanValue moduleListVisibleValue,
                                                     List<Setting> settings) {
        Map<String, ConfigValue<?>> map = new LinkedHashMap<>();

        map.put(enabledValue.getName(), enabledValue);
        map.put(activationSourceValue.getName(), activationSourceValue);
        map.put(moduleListVisibleValue.getName(), moduleListVisibleValue);

        for (Setting s : settings) {
            ConfigValue<?> v = s.getConfigValue();
            if (v != null) {
                map.put(v.getName(), v);
            }
        }

        return new ArrayList<>(map.values());
    }

    /**
     * Legacy support: apply values from the old "settings" map saved by previous versions.
     */
    public static void applyLegacy(Object json, BooleanValue moduleListVisibleValue, List<Setting> settings) {
        if (json == null) return;

        if (json instanceof Json5Object obj) {
            for (var e : obj.asMap().entrySet()) {
                applySetting(e.getKey(), e.getValue(), moduleListVisibleValue, settings);
            }
        } else if (json instanceof Map<?, ?> map) {
            for (var e : map.entrySet()) {
                if (e.getKey() instanceof String k) {
                    applySetting(k, e.getValue(), moduleListVisibleValue, settings);
                }
            }
        }
    }

    private static void applySetting(String id, Object data, BooleanValue moduleListVisibleValue, List<Setting> settings) {
        if (SETTING_SHOW_IN_MODULE_LIST.equals(id) || LEGACY_SETTING_SHOW_IN_ACTIVE_MODULES.equals(id)) {
            moduleListVisibleValue.fromJson(data);
            return;
        }
        for (Setting s : settings) {
            if (s.getId().equals(id)) {
                s.load(data);
                break;
            }
        }
    }

    /**
     * Re-applies values to settings to trigger any side effects in Setting.load().
     */
    public static void syncAfterLoad(List<Setting> settings) {
        for (Setting s : settings) {
            ConfigValue<?> v = s.getConfigValue();
            if (v != null) {
                s.load(v.toJson());
            }
        }
    }

    public static void requestSaveQuietly(ConfigObject module) {
        try {
            ConfigSerializer.requestSave(module);
        } catch (Throwable ignored) {
        }
    }
}
