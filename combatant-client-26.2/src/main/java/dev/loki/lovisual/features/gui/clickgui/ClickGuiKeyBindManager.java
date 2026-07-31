package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.settings.FunctionBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.KeyBindSetting;
import dev.loki.lovisual.util.input.KeyUtil;
import dev.loki.lovisual.util.wav.ClickGuiSounds;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

final class ClickGuiKeyBindManager {

    private ClickGuiKeyBindManager() {
    }

    static void beginKeyBind(Object owner) {
        ClickGuiEditorManager.clearInlineText();
        ClickGuiRenderer.waitingForKey = true;
        ClickGuiRenderer.keyBindOwner = owner;
        ClickGuiRenderer.pendingBindKeys.clear();
        ClickGuiSearch.deactivate();
        ClickGuiSounds.bindingStart();
    }

    static void finishKeyBind() {
        ClickGuiRenderer.waitingForKey = false;
        ClickGuiRenderer.keyBindOwner = null;
        ClickGuiRenderer.pendingBindKeys.clear();
    }

    static void addPendingKey(int keyCode, int scancode) {
        int code = KeyUtil.fromKeyInput(keyCode, scancode);
        if (code == KeyUtil.NONE) return;
        ClickGuiRenderer.pendingBindKeys.add(code);
    }

    static void addPendingMouseButton(int button) {
        if (button < GLFW.GLFW_MOUSE_BUTTON_1 || button > GLFW.GLFW_MOUSE_BUTTON_LAST) return;
        ClickGuiRenderer.pendingBindKeys.add(KeyUtil.mouseButtonToCode(button));
    }

    static void submitKeySelection(Set<Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            submitKeySelection((String) null);
            return;
        }
        submitKeySelection(KeyUtil.comboToString(keys));
    }

    static void submitKeySelection(String combo) {
        if (ClickGuiRenderer.keyBindOwner instanceof KeyBindSetting ks) {
            ks.onKeySelected(combo);
        } else if (ClickGuiRenderer.keyBindOwner instanceof FunctionBindSetting fs) {
            fs.onKeySelected(combo);
        }
    }

    static String getPendingBindDisplay() {
        if (!ClickGuiRenderer.waitingForKey) return null;
        return ClickGuiRenderer.pendingBindKeys.isEmpty() ? "..." : KeyUtil.comboToString(ClickGuiRenderer.pendingBindKeys);
    }
}
