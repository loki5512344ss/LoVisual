/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable;

import dev.loki.lovisual.features.gui.hud.HudElementAutoLoader;

public enum StaticHudElementBootstrap {
    ;

    private static boolean initialized = false;
    private static boolean loaded = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        ensureLoaded();
    }

    /**
     * Ленивая загрузка static-элементов при первом обращении.
     */
    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            HudElementAutoLoader.loadStatic("dev.loki.lovisual.features.gui.hud.nondraggable.impl");
        } catch (Throwable ignored) {
        }
    }
}
