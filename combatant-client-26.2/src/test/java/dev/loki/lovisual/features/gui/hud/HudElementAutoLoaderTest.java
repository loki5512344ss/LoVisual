package dev.loki.lovisual.features.gui.hud;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HudElementAutoLoaderTest {

    @Test
    void registersDraggableWidgets() {
        HudElementAutoLoader.loadDraggable("dev.loki.lovisual.features.gui.hud.draggable.impl");
        int count = DraggableHudElementRegistry.getWidgets().size();
        System.out.println("Registered draggable widgets: " + count);
        assertTrue(count > 0, "no draggable HUD widgets registered");
    }
}
