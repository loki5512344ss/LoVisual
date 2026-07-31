/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud;

/**
 * HUD widget bootstrapper.
 */
public enum HudElements {
    ;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        HudGlobalConfig.get();

        HudElementAutoLoader.loadDraggable("dev.loki.lovisual.features.gui.hud.draggable.impl");
    }
}
