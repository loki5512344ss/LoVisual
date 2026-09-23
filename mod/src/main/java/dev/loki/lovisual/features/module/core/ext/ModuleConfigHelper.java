package dev.loki.lovisual.features.module.core;

import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.lifecycle.ModuleActivationSource;
import dev.loki.lovisual.features.module.module.ModuleConfigValues;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;

final class ModuleConfigHelper {
    private final Module owner;
    private final BooleanValue enabledValue;
    private final EnumValue<ModuleActivationSource> activationSourceValue;
    private final BooleanValue visibleValue;

    ModuleConfigHelper(Module owner, boolean defEnabled) {
        this.owner = owner;
        this.enabledValue = new BooleanValue("enabled", defEnabled);
        this.activationSourceValue = new EnumValue<>("activation_source", ModuleActivationSource.NONE, ModuleActivationSource.class);
        this.visibleValue = new BooleanValue(ModuleConfigValues.SETTING_SHOW_IN_MODULE_LIST, true);
    }

    BooleanValue enabledValue(){return enabledValue;}
    EnumValue<ModuleActivationSource> activationValue(){return activationSourceValue;}
    BooleanValue visibleValue(){return visibleValue;}

    List<ConfigValue<?>> ordered(List<Setting> settings){return ModuleConfigValues.orderedValues(enabledValue, activationSourceValue, visibleValue, settings);}
    List<String> legacyNames(String n){ if(ModuleConfigValues.SETTING_SHOW_IN_MODULE_LIST.equals(n)) return List.of(ModuleConfigValues.LEGACY_SETTING_SHOW_IN_ACTIVE_MODULES); return List.of();}
    void applyLegacy(Object json, List<Setting> s){ModuleConfigValues.applyLegacy(json, visibleValue, s);}
    void sync(List<Setting> s){ModuleConfigValues.syncAfterLoad(s);}
    void requestSave(){ModuleConfigValues.requestSaveQuietly(owner);}
    boolean isShown(){return visibleValue.get();}
    void setShown(boolean v){ if(visibleValue.get()==v) return; visibleValue.set(v); requestSave();}
    void toggleShown(){setShown(!visibleValue.get());}

    void loadAndApply(ModuleLifecycleHelper lh){
        try{ DebugLog.config("Module config load -> %s", owner.name()); ConfigSerializer.load((ConfigObject)owner);}catch(Throwable e){DebugLog.error("Module config load failed for %s", e, owner.name());}
        lh.loadAndApply(enabledValue, activationSourceValue);
    }
}
