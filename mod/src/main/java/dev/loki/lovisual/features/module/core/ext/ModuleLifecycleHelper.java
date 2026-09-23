package dev.loki.lovisual.features.module.core;

import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.addon.manager.ModuleExtensionManager;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.module.lifecycle.ModuleActivationSource;

final class ModuleLifecycleHelper {
    private final Module owner;
    private boolean enabled;
    private ModuleActivationSource source = ModuleActivationSource.NONE;

    ModuleLifecycleHelper(Module owner) { this.owner = owner; }

    boolean isEnabled() { return enabled; }
    ModuleActivationSource activationSource() { return source; }

    static ModuleActivationSource normalize(boolean state, ModuleActivationSource s) {
        if (!state) return ModuleActivationSource.NONE;
        return s==null||s==ModuleActivationSource.NONE ? ModuleActivationSource.INTERNAL : s;
    }

    boolean isAvailable() { return getReason().isEmpty(); }
    String getReason() { try{String r=owner.getUnavailableReason(); return r==null?"":r.trim();}catch(Exception e){return ""; } }

    void setEnabled(boolean state, ModuleActivationSource src, BooleanValue ev, EnumValue<ModuleActivationSource> av) {
        if (state && !Lifecycle.isActive()) return;
        ModuleActivationSource norm = normalize(state, src);
        if (enabled==state) {
            if (source==norm && ev.get()==enabled && av.get()==source) return;
            source=norm; ev.set(enabled); av.set(source);
            dev.loki.lovisual.features.module.module.ModuleConfigValues.requestSaveQuietly(owner); return;
        }
        source=norm; enabled=state;
        if (enabled) {
            if (!ModuleExtensionManager.beforeEnable(owner)) { enabled=false; source=ModuleActivationSource.NONE; return; }
            owner.onEnable(); ModuleExtensionManager.afterEnable(owner);
        } else {
            if (!ModuleExtensionManager.beforeDisable(owner)) { enabled=true; source=norm; return; }
            owner.onDisable(); ModuleExtensionManager.afterDisable(owner);
        }
        ModuleManager.notifyListeners(owner.name(), enabled);
        ev.set(enabled); av.set(source);
        dev.loki.lovisual.features.module.module.ModuleConfigValues.requestSaveQuietly(owner);
    }

    void setTransient(boolean state, BooleanValue ev, EnumValue<ModuleActivationSource> av) {
        if (enabled==state) return; enabled=state;
        if (enabled) {
            if (ModuleExtensionManager.beforeEnable(owner)) { owner.onEnable(); ModuleExtensionManager.afterEnable(owner);} else enabled=false;
        } else if (ModuleExtensionManager.beforeDisable(owner)) { owner.onDisable(); ModuleExtensionManager.afterDisable(owner);} else enabled=true;
        ModuleManager.notifyListeners(owner.name(), enabled);
    }

    void loadAndApply(BooleanValue ev, EnumValue<ModuleActivationSource> av) {
        enabled=ev.get(); source=normalize(enabled, av.get()); av.set(source);
        if (enabled) {
            if (ModuleExtensionManager.beforeEnable(owner)) { owner.onEnable(); ModuleExtensionManager.afterEnable(owner);}
            else { enabled=false; source=ModuleActivationSource.NONE; ev.set(false); av.set(source); }
        }
    }

    void setSource(ModuleActivationSource src, EnumValue<ModuleActivationSource> av, BooleanValue ev) {
        source=normalize(enabled, src); av.set(source);
        dev.loki.lovisual.features.module.module.ModuleConfigValues.requestSaveQuietly(owner);
    }
    boolean isFromKeybind(){return enabled && source==ModuleActivationSource.KEYBIND;}
}
