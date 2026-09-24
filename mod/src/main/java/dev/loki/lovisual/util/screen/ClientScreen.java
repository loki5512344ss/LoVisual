package dev.loki.lovisual.util.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public enum ClientScreen {
    ;

    public static Screen current() {
        Minecraft mc = Minecraft.getInstance();
        return current(mc);
    }

    public static Screen current(Minecraft mc) {
        return mc != null && mc.gui != null ? mc.gui.screen() : null;
    }

    public static void show(Screen screen) {
        show(Minecraft.getInstance(), screen);
    }

    public static void show(Minecraft mc, Screen screen) {
        if (mc != null && mc.gui != null) {
            mc.gui.setScreen(screen);
        }
    }
}
