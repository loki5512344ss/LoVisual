/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.registry;

import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.registry.HudElementAutoLoader;

/**
 * HUD widget bootstrapper.
 */
public enum HudElements {
    ;

    private static boolean initialized = false;
    private static boolean loaded = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            HudGlobalConfig.get();
        } catch (Throwable t) {
            DebugLog.error("Failed to init HUD global config", t);
        }

        ensureLoaded();
    }

    /**
     * Ленивая загрузка виджетов при первом обращении — не зависит от порядка init-вызовов.
     */
    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            HudElementAutoLoader.loadDraggable("dev.loki.lovisual.features.gui.hud.draggable.impl");
        } catch (Throwable t) {
            DebugLog.error("Failed to load draggable HUD widgets", t);
        }
    }
}
