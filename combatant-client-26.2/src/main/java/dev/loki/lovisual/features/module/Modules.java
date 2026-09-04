/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module;

import dev.loki.lovisual.Lifecycle;

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
