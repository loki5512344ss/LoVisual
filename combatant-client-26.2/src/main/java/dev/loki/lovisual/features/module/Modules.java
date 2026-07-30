/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module;

import dev.loki.lovisual.runtime.ClientRuntime;
import dev.loki.lovisual.runtime.RuntimeGate;

public enum Modules {
    ;

    public static <T extends Module> T get(Class<T> type) {
        if (!RuntimeGate.canRunModules()) return null;
        return ModuleManager.get(type);
    }

    public static <T extends Module> T require(Class<T> type) {
        if (!RuntimeGate.canRunModules()) {
            throw new IllegalStateException("Modules are not available while runtime state is " + ClientRuntime.state());
        }
        return ModuleManager.require(type);
    }

    public static <T extends Module> boolean enabled(Class<T> type) {
        if (!RuntimeGate.canRunModules()) return false;
        T module = get(type);
        return module != null && module.isEnabled();
    }

    public static boolean enabled(String id) {
        if (!RuntimeGate.canRunModules()) return false;
        return ModuleManager.isEnabled(id);
    }
}
