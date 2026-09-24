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
