package dev.loki.lovisual.features.module.lifecycle;

import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;

public enum Modules {
    ;

    public static <T extends Module> T get(Class<T> type) {
        if (!Lifecycle.isActive()) return null;
        return ModuleManager.get(type);
    }

    public static <T extends Module> T require(Class<T> type) {
        if (!Lifecycle.isActive()) {
            throw new IllegalStateException("Modules are not available yet");
        }
        return ModuleManager.require(type);
    }

    public static <T extends Module> boolean enabled(Class<T> type) {
        if (!Lifecycle.isActive()) return false;
        T module = get(type);
        return module != null && module.isEnabled();
    }

    public static boolean enabled(String id) {
        if (!Lifecycle.isActive()) return false;
        return ModuleManager.isEnabled(id);
    }
}
