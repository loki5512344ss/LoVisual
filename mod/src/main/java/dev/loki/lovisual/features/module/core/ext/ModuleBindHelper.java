package dev.loki.lovisual.features.module.core;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.FunctionBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.KeyBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.module.ModuleBinds;

import java.util.*;

final class ModuleBindHelper {
    private final Module owner;
    private final KeyBindSetting keyBind;
    private final Map<String, FunctionBindSetting> actions = new LinkedHashMap<>();

    ModuleBindHelper(Module owner) {
        this.owner = owner;
        this.keyBind = ModuleBinds.createKeyBindSetting();
    }

    KeyBindSetting keyBind() { return keyBind; }
    Map<String, FunctionBindSetting> actions() { return actions; }
    Collection<FunctionBindSetting> actionSettings() { return Collections.unmodifiableCollection(actions.values()); }

    void setDefaultBind(String key) { keyBind.setKeyByName(key); }
    FunctionBindSetting action(String name, String def, dev.loki.lovisual.config.values.mode.BindMode mode) { return ModuleBinds.registerAction(actions, owner.getSettings(), name, def, mode); }
    FunctionBindSetting action(String name, String def) { return action(name, def, dev.loki.lovisual.config.values.mode.BindMode.PRESS); }
    void addAction(String name, String def, dev.loki.lovisual.config.values.mode.BindMode mode) { action(name, def, mode); }
    void addAction(String name, String def) { action(name, def); }
    boolean isPressedOnce(String name, boolean enabled) { return ModuleBinds.isPressedOnce(enabled, actions.get(name)); }
    boolean isHeld(String name, boolean enabled) { return ModuleBinds.isHeld(enabled, actions.get(name)); }
    String actionKey(String name) { return ModuleBinds.actionKey(actions.get(name)); }
    ConfigValue<?> findConfig(String name, List<Setting> settings) {
        if (name==null||name.isBlank()) return null;
        for (Setting s: settings) { ConfigValue<?> v=s.getConfigValue(); if(v!=null&&name.equals(v.getName())) return v; }
        return null;
    }
}
