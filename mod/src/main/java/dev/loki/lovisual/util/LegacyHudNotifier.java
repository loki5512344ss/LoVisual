/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public enum LegacyHudNotifier {
    ;

    public static void show(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.hud.setOverlayMessage(Component.nullToEmpty(message), false);
        }
    }

    public static void show(Component text) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.hud.setOverlayMessage(text, false);
        }
    }
}
