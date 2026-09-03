package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.settings.FunctionBindSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.KeyBindSetting;
import dev.loki.lovisual.util.input.KeyUtil;
import dev.loki.lovisual.util.wav.ClickGuiSounds;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;
import java.util.Set;

final class ClickGuiKeyBindManager {

    private ClickGuiKeyBindManager() {
    }

    // Keys/buttons currently physically held down while capturing a bind.
    // A single tap (press+release with nothing else pressed meanwhile) commits
    // instantly on release; holding it while pressing another key builds a combo,
    // which commits once every held key in it has been released.
    private static final Set<Integer> heldKeys = new LinkedHashSet<>();

    static void beginKeyBind(Object owner) {
        ClickGuiEditorManager.clearInlineText();
        ClickGuiRenderer.waitingForKey = true;
        ClickGuiRenderer.keyBindOwner = owner;
        ClickGuiRenderer.pendingBindKeys.clear();
        heldKeys.clear();
        ClickGuiSearch.deactivate();
        ClickGuiSounds.bindingStart();
    }

    static void finishKeyBind() {
        ClickGuiRenderer.waitingForKey = false;
        ClickGuiRenderer.keyBindOwner = null;
        ClickGuiRenderer.pendingBindKeys.clear();
        heldKeys.clear();
    }

    /** Cancels bind capture without touching the previously saved value. */
    static void cancelKeyBind() {
        if (!ClickGuiRenderer.waitingForKey) return;
        ClickGuiRenderer.waitingForKey = false;
        ClickGuiRenderer.keyBindOwner = null;
        ClickGuiRenderer.pendingBindKeys.clear();
        heldKeys.clear();
        ClickGuiSounds.bindingStart();
    }

    static void addPendingKey(int keyCode, int scancode) {
        int code = KeyUtil.fromKeyInput(keyCode, scancode);
        if (code == KeyUtil.NONE) return;
        ClickGuiRenderer.pendingBindKeys.add(code);
        heldKeys.add(code);
    }

    static void addPendingMouseButton(int button) {
        if (button < GLFW.GLFW_MOUSE_BUTTON_1 || button > GLFW.GLFW_MOUSE_BUTTON_LAST) return;
        int code = KeyUtil.mouseButtonToCode(button);
        ClickGuiRenderer.pendingBindKeys.add(code);
        heldKeys.add(code);
    }

    static void onKeyReleased(int keyCode, int scancode) {
        handleRelease(KeyUtil.fromKeyInput(keyCode, scancode));
    }

    static void onMouseReleased(int button) {
        if (button < GLFW.GLFW_MOUSE_BUTTON_1 || button > GLFW.GLFW_MOUSE_BUTTON_LAST) return;
        handleRelease(KeyUtil.mouseButtonToCode(button));
    }

    private static void handleRelease(int code) {
        if (!ClickGuiRenderer.waitingForKey) return;
        if (code == KeyUtil.NONE) return;
        if (!heldKeys.remove(code)) return;
        if (!heldKeys.isEmpty()) return;
        if (ClickGuiRenderer.pendingBindKeys.isEmpty()) return;

        submitKeySelection(ClickGuiRenderer.pendingBindKeys);
        finishKeyBind();
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
