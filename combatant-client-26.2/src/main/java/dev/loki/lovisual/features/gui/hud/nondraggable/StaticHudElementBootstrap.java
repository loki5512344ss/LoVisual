/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable;

import dev.loki.lovisual.features.gui.hud.HudElementAutoLoader;

public enum StaticHudElementBootstrap {
    ;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        HudElementAutoLoader.loadStatic("dev.loki.lovisual.features.gui.hud.nondraggable.impl");
    }
}
