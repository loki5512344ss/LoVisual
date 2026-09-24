package dev.loki.lovisual.util.text.edit;

import net.minecraft.client.Minecraft;

public enum ClipboardUtil {
    ;

    public static void copy(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.keyboardHandler != null) {
            try {
                mc.keyboardHandler.setClipboard(text == null ? "" : text);
            } catch (Throwable ignored) {
            }
        }
    }

    public static String get() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.keyboardHandler != null) {
            try {
                String clip = mc.keyboardHandler.getClipboard();
                return clip == null ? "" : clip;
            } catch (Throwable ignored) {
            }
        }
        return "";
    }
}
