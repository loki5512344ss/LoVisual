package dev.loki.lovisual.features.gui.clickgui.screen;

import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.features.gui.clickgui.picker.ClickGuiPickerState;
import dev.loki.lovisual.features.gui.clickgui.protocol.CombatProtocolHeuristicsEditorState;
import dev.loki.lovisual.features.gui.clickgui.sections.settings.SettingsTabRuntime;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.ProtocolHeuristicsSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.util.screen.ClientScreen;
import dev.loki.lovisual.util.wav.util.ClickGuiSounds;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiEditorManager;
import dev.loki.lovisual.features.gui.clickgui.picker.screen.ClickGuiPickerScreen;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.screen.ClickGuiScreen;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;

public final class ClickGuiScreenManager {

    private ClickGuiScreenManager() {
    }

    public static void openClickGuiScreen() {

        if (Minecraft.getInstance() == null) return;
        if (ClientScreen.current() != null) return;
        if (ClickGuiRenderer.mainScreen == null) {
            ClickGuiRenderer.mainScreen = new ClickGuiScreen();
        }
        ClickGuiSounds.guiOpen();
        ClientScreen.show(Minecraft.getInstance(), ClickGuiRenderer.mainScreen);
    }

    public static void captureMainScreen() {

        if (Minecraft.getInstance() != null && ClientScreen.current() instanceof ClickGuiScreen cs) {
            ClickGuiRenderer.mainScreen = cs;
        }
    }

    public static ClickGuiScreen ensureMainScreen() {

        if (ClickGuiRenderer.mainScreen == null) {
            ClickGuiRenderer.mainScreen = new ClickGuiScreen();
        }
        return ClickGuiRenderer.mainScreen;
    }

    private static void openPickerScreen() {
        if (Minecraft.getInstance() == null) return;
        if (ClientScreen.current() instanceof ClickGuiScreen cs) {
            ClickGuiRenderer.mainScreen = cs;
        }
        if (ClickGuiRenderer.pickerScreen == null) {
            ClickGuiRenderer.pickerScreen = new ClickGuiPickerScreen();
        }
        ClientScreen.show(Minecraft.getInstance(), ClickGuiRenderer.pickerScreen);
    }

    public static void clearScreens() {

        ClickGuiRenderer.mainScreen = null;
        ClickGuiRenderer.pickerScreen = null;
        SettingsTabRuntime.clearScreens();
    }

    public static void openPicker(TextListSetting owner, String title, TextListSetting.PickerMode mode) {

        if (owner == null) return;
        ClickGuiEditorManager.clearInlineText();
        ClickGuiSearch.deactivate();
        ClickGuiRenderer.protocolHeuristicsEditor = null;
        ClickGuiRenderer.picker = new ClickGuiPickerState(owner, title, mode);
        openPickerScreen();
    }

    public static void openProtocolHeuristicsEditor(ProtocolHeuristicsSetting owner, String title) {

        if (owner == null) return;
        ClickGuiEditorManager.clearInlineText();
        ClickGuiSearch.deactivate();
        ClickGuiRenderer.picker = null;
        ClickGuiRenderer.protocolHeuristicsEditor = new CombatProtocolHeuristicsEditorState(owner, title);
        openPickerScreen();
    }

    public static void closeProtocolHeuristicsEditor() {

        closePicker();
    }

    public static void closePickerScreen() {

        closePicker();
    }

    public static void closePicker() {

        ClickGuiRenderer.picker = null;
        ClickGuiRenderer.protocolHeuristicsEditor = null;
        ClickGuiRenderer.textEditor = null;
        if (Minecraft.getInstance() != null && ClientScreen.current() instanceof ClickGuiPickerScreen) {
            if (ClickGuiRenderer.mainScreen == null) {
                ClickGuiRenderer.mainScreen = new ClickGuiScreen();
            }
            ClientScreen.show(Minecraft.getInstance(), ClickGuiRenderer.mainScreen);
        }
    }

    static void clearPicker() {
        ClickGuiRenderer.picker = null;
        ClickGuiRenderer.protocolHeuristicsEditor = null;
    }

    static synchronized void requestConfigSave() {
        Runnable task = () -> {
            try {
                ConfigSerializer.saveAll();
            } catch (Throwable ignored) {
            }
        };

        if (ClickGuiRenderer.pendingSaveTask == null || ClickGuiRenderer.pendingSaveTask.isDone()) {
            ClickGuiRenderer.pendingSaveTask = CompletableFuture.runAsync(task);
        } else {
            ClickGuiRenderer.pendingSaveTask = ClickGuiRenderer.pendingSaveTask.thenRunAsync(task);
        }
    }
}
