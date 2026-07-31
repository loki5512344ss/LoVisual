package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiTextEditorState;
import dev.loki.lovisual.features.gui.clickgui.picker.ClickGuiPickerState;
import dev.loki.lovisual.features.gui.clickgui.protocol.CombatProtocolHeuristicsEditorState;
import dev.loki.lovisual.features.gui.clickgui.settings.TextEditorOwner;
import dev.loki.lovisual.features.gui.clickgui.settings.TextSetting;
import dev.loki.lovisual.features.module.ModuleManager;
import dev.loki.lovisual.util.text.ClipboardUtil;

final class ClickGuiEditorManager {

    private ClickGuiEditorManager() {
    }

    static void openTextEditor(TextEditorOwner owner, String title, String seedText, float anchorX, float anchorY, float anchorW) {
        if (owner == null) return;
        clearInlineText();
        if (seedText == null) seedText = "";
        ClickGuiSearch.deactivate();
        ClickGuiRenderer.textEditor = new ClickGuiTextEditorState(owner, title, seedText, anchorX, anchorY, anchorW);
    }

    static void closeTextEditor(boolean save) {
        if (ClickGuiRenderer.textEditor == null) return;
        if (save) ClickGuiRenderer.textEditor.save();
        ClickGuiRenderer.textEditor = null;
    }

    static void renderTextEditor(int fbw, int fbh) {
        if (ClickGuiRenderer.textEditor == null) return;
        ClickGuiRenderer.textEditor.render(fbw, fbh);
    }

    static void renderPicker(int fbw, int fbh) {
        if (ClickGuiRenderer.picker == null) return;
        ClickGuiRenderer.picker.render(fbw, fbh);
    }

    static void toggleInlineText(TextSetting owner) {
        if (owner == null) return;
        if (ClickGuiRenderer.inlineTextOwner == owner) {
            clearInlineText();
            return;
        }
        ClickGuiRenderer.inlineTextOwner = owner;
        ClickGuiRenderer.inlineTextBuffer = new StringBuilder(owner.getEditorText());
        ClickGuiSearch.deactivate();
    }

    static boolean isInlineTextActive() {
        return ClickGuiRenderer.inlineTextOwner != null;
    }

    static boolean isInlineTextOwner(TextSetting owner) {
        return ClickGuiRenderer.inlineTextOwner == owner;
    }

    static String getInlineText(TextSetting owner) {
        if (owner == null) return "";
        if (ClickGuiRenderer.inlineTextOwner == owner && ClickGuiRenderer.inlineTextBuffer != null) {
            return ClickGuiRenderer.inlineTextBuffer.toString();
        }
        return owner.getEditorText();
    }

    static void updateInlineTextBounds(TextSetting owner, float x, float y, float w, float h) {
        if (ClickGuiRenderer.inlineTextOwner != owner) return;
        ClickGuiRenderer.inlineTextX = x;
        ClickGuiRenderer.inlineTextY = y;
        ClickGuiRenderer.inlineTextW = w;
        ClickGuiRenderer.inlineTextH = h;
    }

    static void finishInlineText(boolean save) {
        if (ClickGuiRenderer.inlineTextOwner == null || ClickGuiRenderer.inlineTextBuffer == null) {
            clearInlineText();
            return;
        }
        if (save) {
            ClickGuiRenderer.inlineTextOwner.applyEditorText(ClickGuiRenderer.inlineTextBuffer.toString());
        }
        clearInlineText();
    }

    static void clearInlineText() {
        ClickGuiRenderer.inlineTextOwner = null;
        ClickGuiRenderer.inlineTextBuffer = null;
        ClickGuiRenderer.inlineTextX = ClickGuiRenderer.inlineTextY = ClickGuiRenderer.inlineTextW = ClickGuiRenderer.inlineTextH = 0f;
    }

    static void inlineBackspace() {
        if (ClickGuiRenderer.inlineTextBuffer == null) return;
        int len = ClickGuiRenderer.inlineTextBuffer.length();
        if (len <= 0) return;
        ClickGuiRenderer.inlineTextBuffer.deleteCharAt(len - 1);
    }

    static void inlinePaste() {
        if (ClickGuiRenderer.inlineTextBuffer == null) return;
        String clip = ClipboardUtil.get();
        if (clip == null || clip.isEmpty()) return;
        ClickGuiRenderer.inlineTextBuffer.append(normalizeInlineText(clip));
    }

    static String normalizeInlineText(String text) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.replace('\n', ' ');
    }

    static boolean isTextEditorActive() {
        return ClickGuiRenderer.textEditor != null;
    }

    static boolean isPickerActive() {
        return ClickGuiRenderer.picker != null || ClickGuiRenderer.protocolHeuristicsEditor != null;
    }

    static boolean isBlockingModuleKeybinds() {
        if (!ModuleManager.isEnabled("clickgui")) return false;
        if (ClickGuiRenderer.waitingForKey) return true;
        if (ClickGuiRenderer.textEditor != null) return true;
        if (ClickGuiRenderer.inlineTextOwner != null) return true;
        if (ClickGuiRenderer.protocolHeuristicsEditor != null) return true;
        if (ClickGuiRenderer.picker != null && ClickGuiRenderer.picker.isListening()) return true;
        return ClickGuiSearch.isActive();
    }
}
