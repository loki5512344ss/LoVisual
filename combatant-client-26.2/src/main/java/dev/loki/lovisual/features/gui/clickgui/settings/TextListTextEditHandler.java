/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.text.ClipboardUtil;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;

final class TextListTextEditHandler {
    private TextListTextEditHandler() {
    }

    static void beginTextListEdit(TextListSetting setting) {
        TextListSetting.UiState ui = setting.ui();
        if (!ui.editing) {
            ui.editBuffer = TextListSettingRenderer.firstEntry(setting);
            ui.editCursor = ui.editBuffer.length();
            ui.editSelection.clear();
            ui.fieldErrorAnim = 0f;
        }
        ui.editing = true;
        ui.cursorBlink = 0f;
    }

    static void cancelTextListEdit(TextListSetting setting) {
        TextListSetting.UiState ui = setting.ui();
        ui.editing = false;
        ui.editBuffer = TextListSettingRenderer.firstEntry(setting);
        ui.editCursor = ui.editBuffer.length();
        ui.editSelection.clear();
        ui.fieldErrorAnim = 0f;
    }

    static void commitTextListEdit(TextListSetting setting, boolean save) {
        TextListSetting.UiState ui = setting.ui();
        String raw = ui.editBuffer == null ? "" : ui.editBuffer.trim();
        LinkedHashSet<String> next = new LinkedHashSet<>();
        if (!raw.isEmpty()) next.add(setting.normalizeEntry(raw));
        setting.value().set(next);
        ui.editing = false;
        ui.editSelection.clear();
        if (save && setting.getParent() != null) setting.getParent().saveConfig();
    }

    static void setTextListCursorFromMouse(TextListSetting setting, float mx, boolean shift) {
        TextListSetting.UiState ui = setting.ui();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(14f, 6.1f);
        int cursor = SettingTextInputUtil.caretFromText(font, ui.editBuffer == null ? "" : ui.editBuffer, size, ui.fieldX + SettingSkinUtil.m(6f, 4f), mx);
        setTextListCursor(ui, cursor, shift);
    }

    static void setTextListCursor(TextListSetting.UiState ui, int pos, boolean shift) {
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

    static void insertTextListText(TextListSetting setting, String text) {
        TextListSetting.UiState ui = setting.ui();
        if (text == null || text.isEmpty()) return;
        deleteTextListSelection(ui);
        String clean = text.replace("\r", "").replace("\n", " ");
        int cursor = Math.max(0, Math.min(ui.editCursor, ui.editBuffer.length()));
        ui.editBuffer = ui.editBuffer.substring(0, cursor) + clean + ui.editBuffer.substring(cursor);
        ui.editCursor = cursor + clean.length();
        ui.editSelection.clear();
    }

    static void selectAllTextList(TextListSetting.UiState ui) {
        ui.editSelection.begin(0, 0);
        ui.editSelection.updateCaret(0, ui.editBuffer == null ? 0 : ui.editBuffer.length());
        ui.editCursor = ui.editBuffer == null ? 0 : ui.editBuffer.length();
    }

    static String selectedOrAllTextList(TextListSetting.UiState ui) {
        if (hasTextListSelection(ui)) return ui.editBuffer.substring(textListSelStart(ui), textListSelEnd(ui));
        return ui.editBuffer == null ? "" : ui.editBuffer;
    }

    static boolean hasTextListSelection(TextListSetting.UiState ui) {
        return ui.editSelection.hasRange();
    }

    static int textListSelStart(TextListSetting.UiState ui) {
        return hasTextListSelection(ui) ? Math.max(0, Math.min(ui.editSelection.start(), ui.editBuffer.length())) : -1;
    }

    static int textListSelEnd(TextListSetting.UiState ui) {
        return hasTextListSelection(ui) ? Math.max(0, Math.min(ui.editSelection.end(), ui.editBuffer.length())) : -1;
    }

    static boolean deleteTextListSelection(TextListSetting.UiState ui) {
        if (!hasTextListSelection(ui)) return false;
        int start = textListSelStart(ui);
        int end = textListSelEnd(ui);
        ui.editBuffer = ui.editBuffer.substring(0, start) + ui.editBuffer.substring(end);
        ui.editCursor = start;
        ui.editSelection.clear();
        return true;
    }

    static boolean keyPressed(TextListSetting setting, int keyCode, int scanCode, int modifiers) {
        if (!TextListSettingRenderer.isInlineTextList(setting)) return false;
        TextListSetting.UiState ui = setting.ui();
        if (!ui.editing) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) { selectAllTextList(ui); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) { ClipboardUtil.copy(selectedOrAllTextList(ui)); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) { insertTextListText(setting, ClipboardUtil.get()); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            ClipboardUtil.copy(selectedOrAllTextList(ui));
            deleteTextListSelection(ui);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { commitTextListEdit(setting, true); return true; }
            case GLFW.GLFW_KEY_ESCAPE -> { cancelTextListEdit(setting); return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!deleteTextListSelection(ui) && ui.editCursor > 0) {
                    ui.editBuffer = ui.editBuffer.substring(0, ui.editCursor - 1) + ui.editBuffer.substring(ui.editCursor);
                    ui.editCursor--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (!deleteTextListSelection(ui) && ui.editCursor < ui.editBuffer.length())
                    ui.editBuffer = ui.editBuffer.substring(0, ui.editCursor) + ui.editBuffer.substring(ui.editCursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> { setTextListCursor(ui, ui.editCursor - 1, shift); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { setTextListCursor(ui, ui.editCursor + 1, shift); return true; }
            case GLFW.GLFW_KEY_HOME -> { setTextListCursor(ui, 0, shift); return true; }
            case GLFW.GLFW_KEY_END -> { setTextListCursor(ui, ui.editBuffer.length(), shift); return true; }
        }
        return true;
    }

    static boolean charTyped(TextListSetting setting, char chr, int modifiers) {
        if (!TextListSettingRenderer.isInlineTextList(setting)) return false;
        TextListSetting.UiState ui = setting.ui();
        if (!ui.editing) return false;
        if (chr == '\n' || chr == '\r') return true;
        insertTextListText(setting, String.valueOf(chr));
        return true;
    }
}