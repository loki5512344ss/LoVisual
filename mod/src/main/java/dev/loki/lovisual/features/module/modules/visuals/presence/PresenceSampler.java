package dev.loki.lovisual.features.module.modules.visuals.presence;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.ModulesMenuScreen;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.MenuPresence;
import dev.loki.lovisual.features.platform.presence.PresenceCodec;
import dev.loki.lovisual.features.platform.presence.PresenceTimeline;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Samples the local ClickGUI cursor every 100 ms into a short queue that is flushed with the next sync. */
final class PresenceSampler {

    private final List<PresenceCodec.Sample> queue = new ArrayList<>();
    private MenuPresence last;
    private long lastSample;

    /** @return true while a menu is open (and sampled) */
    boolean sample(long now) {
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.getWindow() != null
                && GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        MenuPresence snap = ModulesMenuScreen.presenceSnapshot(down);
        if (snap == null) {
            last = null;
            queue.clear();
            return false;
        }
        last = snap;
        if (now - lastSample >= PresenceTimeline.SAMPLE_MS) {
            lastSample = now;
            queue.add(new PresenceCodec.Sample(Math.round(snap.x() * 255.0f), Math.round(snap.y() * 255.0f),
                    snap.pressed(), snap.scroll()));
            while (queue.size() > PresenceCodec.MAX_SAMPLES) queue.remove(0);
        }
        return true;
    }

    /** Payload for the current menu state with the queued samples, or null when no menu is open. */
    PresenceCodec.Gui drain() {
        if (last == null) return null;
        PresenceCodec.Gui gui = new PresenceCodec.Gui(last.category(), last.settingsOpen(), last.searching(), false,
                last.moduleIndex(), new ArrayList<>(queue));
        queue.clear();
        return gui;
    }
}
