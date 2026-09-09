/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.module;

import dev.loki.lovisual.config.values.BindMode;
import dev.loki.lovisual.config.values.KeyBindValue;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.FunctionBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.KeyBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.util.input.KeyManager;

import java.util.List;
import java.util.Map;

/**
 * Keybind and action helpers for modules: the shared key bind setting creation, action
 * registration into the module's settings and action query helpers.
 */
public final class ModuleBinds {

    private ModuleBinds() {
    }

    public static KeyBindSetting createKeyBindSetting() {
        return new KeyBindSetting(new KeyBindValue("bind", "NONE"));
    }

    public static FunctionBindSetting registerAction(Map<String, FunctionBindSetting> actions,
                                                     List<Setting> settings,
                                                     String name,
                                                     String defaultKey,
                                                     BindMode mode) {
        FunctionBindSetting setting = new FunctionBindSetting(name, defaultKey, mode);
        actions.put(name, setting);
        settings.add(setting);
        return setting;
    }

    public static boolean isPressedOnce(boolean enabled, FunctionBindSetting setting) {
        if (!enabled || setting == null) return false;
        return KeyManager.wasPressed(setting.getBindingName());
    }

    public static boolean isHeld(boolean enabled, FunctionBindSetting setting) {
        if (!enabled || setting == null) return false;
        return KeyManager.isHeld(setting.getBindingName());
    }

    public static String actionKey(FunctionBindSetting setting) {
        return setting == null ? "NONE" : setting.get();
    }
}
