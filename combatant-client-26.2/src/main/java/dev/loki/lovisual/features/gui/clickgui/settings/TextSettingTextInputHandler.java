/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.text.ClipboardUtil;
import org.lwjgl.glfw.GLFW;

final class TextSettingTextInputHandler {
    private TextSettingTextInputHandler() {
    }

    static void beginTextEdit(TextSetting setting) {
        TextSetting.UiState ui = setting.ui();
        if (!ui.editing) {
            ui.editBuffer = setting.getEditorText();
            ui.editCursor = ui.editBuffer.length();
            ui.editSelection.clear();
            ui.errorAnim = 0f;
        }
        ui.editing = true;
        ui.cursorBlink = 0f;
    }

    static void cancelTextEdit(TextSetting setting) {
        TextSetting.UiState ui = setting.ui();
        ui.editing = false;
        ui.editBuffer = setting.getEditorText();
        ui.editCursor = ui.editBuffer.length();
        ui.textScroll = 0f;
        ui.editSelection.clear();
        ui.errorAnim = 0f;
    }

    static void commitTextEdit(TextSetting setting, boolean save) {
        TextSetting.UiState ui = setting.ui();
        setting.applyEditorText(ui.editBuffer == null ? "" : ui.editBuffer);
        ui.editing = false;
        ui.textScroll = 0f;
        ui.editSelection.clear();
        if (!save && setting.getParent() != null) setting.getParent().saveConfig();
    }

    static void setTextCursorFromMouse(TextSetting setting, float mx, boolean shift) {
        TextSetting.UiState ui = setting.ui();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        int cursor = SettingTextInputUtil.caretFromText(font, ui.editBuffer == null ? "" : ui.editBuffer, ui.textSize <= 0f ? SettingSkinUtil.m(14f, 6.1f) : ui.textSize, ui.textX, mx);
        setTextCursor(ui, cursor, shift);
    }

    static void setTextCursor(TextSetting.UiState ui, int pos, boolean shift) {
        int next = Math.max(0, Math.min(ui.editBuffer == null ? 0 : ui.editBuffer.length(), pos));
        if (shift) {
            if (!ui.editSelection.hasRange()) ui.editSelection.begin(0, ui.editCursor);
            ui.editSelection.updateCaret(0, next);
        } else {
            ui.editSelection.clear();
        }
        ui.editCursor = next;
        ui.cursorBlink = 0f;
    }

    static void insertTextText(TextSetting setting, String text) {
        TextSetting.UiState ui = setting.ui();
        if (text == null || text.isEmpty()) return;
        deleteTextSelection(ui);
        String clean = text.replace("\r", "").replace("\n", " ");
        int cursor = Math.max(0, Math.min(ui.editCursor, ui.editBuffer.length()));
        ui.editBuffer = ui.editBuffer.substring(0, cursor) + clean + ui.editBuffer.substring(cursor);
        ui.editCursor = cursor + clean.length();
        ui.editSelection.clear();
    }

    static void ensureTextCursorVisible(TextSetting.UiState ui, TextRenderer font, String text, float size, float visibleW) {
        String safe = text == null ? "" : text;
        int cursor = Math.max(0, Math.min(ui.editCursor, safe.length()));
        float textW = UnifiedSettingsSkin.textWidth(font, safe, size);
        float maxScroll = Math.max(0f, textW - visibleW);
        float caretX = UnifiedSettingsSkin.textWidth(font, safe.substring(0, cursor), size);
        float margin = Math.min(SettingSkinUtil.m(8f, 4f), visibleW * 0.35f);

        if (caretX - ui.textScroll > visibleW - margin) {
            ui.textScroll = caretX - visibleW + margin;
        } else if (caretX - ui.textScroll < margin) {
            ui.textScroll = caretX - margin;
        }
        ui.textScroll = Math.max(0f, Math.min(maxScroll, ui.textScroll));
    }

    static void selectAllText(TextSetting.UiState ui) {
        ui.editSelection.begin(0, 0);
        ui.editSelection.updateCaret(0, ui.editBuffer == null ? 0 : ui.editBuffer.length());
        ui.editCursor = ui.editBuffer == null ? 0 : ui.editBuffer.length();
    }

    static String selectedOrAllText(TextSetting.UiState ui) {
        if (hasTextSelection(ui)) return ui.editBuffer.substring(textSelStart(ui), textSelEnd(ui));
        return ui.editBuffer == null ? "" : ui.editBuffer;
    }

    static boolean hasTextSelection(TextSetting.UiState ui) {
        return ui.editSelection.hasRange();
    }

    static int textSelStart(TextSetting.UiState ui) {
        return hasTextSelection(ui) ? Math.max(0, Math.min(ui.editSelection.start(), ui.editBuffer.length())) : -1;
    }

    static int textSelEnd(TextSetting.UiState ui) {
        return hasTextSelection(ui) ? Math.max(0, Math.min(ui.editSelection.end(), ui.editBuffer.length())) : -1;
    }

    static boolean deleteTextSelection(TextSetting.UiState ui) {
        if (!hasTextSelection(ui)) return false;
        int start = textSelStart(ui);
        int end = textSelEnd(ui);
        ui.editBuffer = ui.editBuffer.substring(0, start) + ui.editBuffer.substring(end);
        ui.editCursor = start;
        ui.editSelection.clear();
        return true;
    }

    static boolean keyPressed(TextSetting setting, int keyCode, int scanCode, int modifiers) {
        TextSetting.UiState ui = setting.ui();
        if (!ui.editing) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) { selectAllText(ui); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) { ClipboardUtil.copy(selectedOrAllText(ui)); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) { insertTextText(setting, ClipboardUtil.get()); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            ClipboardUtil.copy(selectedOrAllText(ui));
            deleteTextSelection(ui);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { commitTextEdit(setting, true); return true; }
            case GLFW.GLFW_KEY_ESCAPE -> { cancelTextEdit(setting); return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!deleteTextSelection(ui) && ui.editCursor > 0) {
                    ui.editBuffer = ui.editBuffer.substring(0, ui.editCursor - 1) + ui.editBuffer.substring(ui.editCursor);
                    ui.editCursor--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (!deleteTextSelection(ui) && ui.editCursor < ui.editBuffer.length())
                    ui.editBuffer = ui.editBuffer.substring(0, ui.editCursor) + ui.editBuffer.substring(ui.editCursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> { setTextCursor(ui, ui.editCursor - 1, shift); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { setTextCursor(ui, ui.editCursor + 1, shift); return true; }
            case GLFW.GLFW_KEY_HOME -> { setTextCursor(ui, 0, shift); return true; }
            case GLFW.GLFW_KEY_END -> { setTextCursor(ui, ui.editBuffer.length(), shift); return true; }
        }
        return true;
    }

    static boolean charTyped(TextSetting setting, char chr, int modifiers) {
        TextSetting.UiState ui = setting.ui();
        if (!ui.editing) return false;
        if (chr == '\n' || chr == '\r') return true;
        insertTextText(setting, String.valueOf(chr));
        return true;
    }
}