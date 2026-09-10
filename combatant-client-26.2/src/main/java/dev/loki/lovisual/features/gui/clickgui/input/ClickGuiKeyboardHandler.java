package dev.loki.lovisual.features.gui.clickgui.input;

import dev.loki.lovisual.features.gui.clickgui.sections.settings.SettingsTabRuntime;
import dev.loki.lovisual.features.module.core.ModuleManager;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiEditorManager;
import dev.loki.lovisual.features.gui.clickgui.input.ClickGuiKeyBindManager;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.screen.ClickGuiScreenManager;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiTabState;

public final class ClickGuiKeyboardHandler {

    private ClickGuiKeyboardHandler() {
    }

    public static boolean onEditorCharTyped(char c) {

        return onCharTyped(c);
    }

    public static boolean onEditorKey(int key, int scancode, int action, int mods) {

        return onKey(key, scancode, action, mods);
    }

    public static void closeGuiEditor() {

        SettingsTabRuntime.closeGuiEditor();
    }

    public static boolean onCharTyped(char c) {

        if (!ModuleManager.isEnabled("clickgui")) return false;
        if (ClickGuiRenderer.textEditor != null) {
            ClickGuiRenderer.textEditor.insertChar(c);
            return true;
        }
        if (ClickGuiRenderer.picker != null) {
            if (ClickGuiRenderer.picker.isListening()) {
                ClickGuiRenderer.picker.insertChar(c);
                return true;
            }
            return false;
        }
        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
            return ClickGuiRenderer.protocolHeuristicsEditor.charTyped(c);
        }
        if (ClickGuiRenderer.inlineTextOwner != null && ClickGuiRenderer.inlineTextBuffer != null) {
            if (StringUtil.isAllowedChatCharacter(c)) {
                ClickGuiRenderer.inlineTextBuffer.append(c);
                return true;
            }
            return false;
        }
        return ClickGuiTabState.getActiveSection().charTyped(c, 0);
    }

    public static boolean onFilesDrop(List<Path> paths) {

        if (!ModuleManager.isEnabled("clickgui")) return false;
        if (ClickGuiRenderer.textEditor != null || ClickGuiRenderer.picker != null || ClickGuiRenderer.protocolHeuristicsEditor != null) return false;
        return ClickGuiTabState.getActiveSection().onFilesDrop(paths);
    }

    public static boolean onKey(int key, int scancode, int action, int mods) {

        if (!ModuleManager.isEnabled("clickgui")) return false;

        if (ClickGuiRenderer.textEditor != null) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
                boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
                switch (key) {
                    case GLFW.GLFW_KEY_ESCAPE -> ClickGuiEditorManager.closeTextEditor(false);
                    case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> ClickGuiRenderer.textEditor.insertNewline();
                    case GLFW.GLFW_KEY_BACKSPACE -> ClickGuiRenderer.textEditor.backspace();
                    case GLFW.GLFW_KEY_DELETE -> ClickGuiRenderer.textEditor.deleteForward();
                    case GLFW.GLFW_KEY_LEFT -> ClickGuiRenderer.textEditor.moveCaret(-1, shift);
                    case GLFW.GLFW_KEY_RIGHT -> ClickGuiRenderer.textEditor.moveCaret(1, shift);
                    case GLFW.GLFW_KEY_UP -> ClickGuiRenderer.textEditor.moveCaretVertical(-1, shift);
                    case GLFW.GLFW_KEY_DOWN -> ClickGuiRenderer.textEditor.moveCaretVertical(1, shift);
                    case GLFW.GLFW_KEY_HOME -> ClickGuiRenderer.textEditor.moveCaretToStart(shift);
                    case GLFW.GLFW_KEY_END -> ClickGuiRenderer.textEditor.moveCaretToEnd(shift);
                    default -> {
                        if (key == GLFW.GLFW_KEY_V && ctrl) ClickGuiRenderer.textEditor.pasteFromClipboard();
                        if (key == GLFW.GLFW_KEY_C && ctrl) ClickGuiRenderer.textEditor.copySelection();
                        if (key == GLFW.GLFW_KEY_A && ctrl) ClickGuiRenderer.textEditor.selectAll();
                    }
                }
            }
            return true;
        }

        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
            return ClickGuiRenderer.protocolHeuristicsEditor.keyPressed(key, scancode, action, mods);
        }

        if (ClickGuiRenderer.picker != null) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
                switch (key) {
                    case GLFW.GLFW_KEY_ESCAPE -> ClickGuiScreenManager.closePicker();
                    case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> ClickGuiRenderer.picker.stopListening();
                    case GLFW.GLFW_KEY_BACKSPACE -> ClickGuiRenderer.picker.backspace();
                    default -> {
                        if (key == GLFW.GLFW_KEY_F && ctrl) ClickGuiRenderer.picker.toggleListening();
                    }
                }
            }
            return true;
        }

        if (ClickGuiRenderer.inlineTextOwner != null && ClickGuiRenderer.inlineTextBuffer != null) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
                switch (key) {
                    case GLFW.GLFW_KEY_ESCAPE -> ClickGuiEditorManager.finishInlineText(false);
                    case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> ClickGuiEditorManager.finishInlineText(true);
                    case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE -> ClickGuiEditorManager.inlineBackspace();
                    default -> {
                        if (key == GLFW.GLFW_KEY_V && ctrl) ClickGuiEditorManager.inlinePaste();
                    }
                }
            }
            return true;
        }

        if (ClickGuiRenderer.waitingForKey) {

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                // Escape always exits the bind capture immediately without
                // touching the previously saved bind.
                ClickGuiKeyBindManager.cancelKeyBind();
                return true;
            }

            if (action == GLFW.GLFW_PRESS) {
                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    ClickGuiKeyBindManager.submitKeySelection(ClickGuiRenderer.pendingBindKeys);
                    ClickGuiKeyBindManager.finishKeyBind();
                    return true;
                }

                ClickGuiKeyBindManager.addPendingKey(key, scancode);
            } else if (action == GLFW.GLFW_RELEASE) {
                // A tap (press+release with no other key held) commits instantly;
                // holding this key while pressing another builds a combo, which
                // commits once every held key in it has been released.
                ClickGuiKeyBindManager.onKeyReleased(key, scancode);
            }

            return true;
        }

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            return ClickGuiTabState.getActiveSection().keyPressed(key, scancode, mods);
        }
        return false;
    }
}
