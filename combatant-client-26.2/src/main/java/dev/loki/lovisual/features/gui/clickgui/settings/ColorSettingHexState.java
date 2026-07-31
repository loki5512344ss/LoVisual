/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.config.values.ColorValue;
import dev.loki.lovisual.render.engine.text.TextRenderer;

final class ColorSettingHexState {
    private ColorSettingHexState() {
    }

    static String displayHex(ColorValue value, boolean hasAlpha) {
        int argb = value.isRainbow() ? parseFallbackHex(value.rainbowFallbackHex(), value.getArgb()) : value.getArgb();
        if (hasAlpha)
            return String.format("%02X%02X%02X%02X", (argb >>> 24) & 0xFF, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
        return String.format("%02X%02X%02X", (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
    }

    private static int parseFallbackHex(String hex, int fallback) {
        Integer parsed = parseHexInput(hex, true);
        return parsed == null ? fallback : parsed;
    }

    static Integer parseHexInput(String raw, boolean hasAlpha) {
        if (raw == null) return null;
        StringBuilder clean = new StringBuilder();
        String t = raw.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (isHexChar(c)) clean.append(Character.toUpperCase(c));
        }
        String s = clean.toString();
        try {
            if (s.length() == 3) {
                int r = Integer.parseInt(s.substring(0, 1) + s.substring(0, 1), 16);
                int g = Integer.parseInt(s.substring(1, 2) + s.substring(1, 2), 16);
                int b = Integer.parseInt(s.substring(2, 3) + s.substring(2, 3), 16);
                return 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            if (s.length() == 6) {
                return (int) (0xFF000000L | Long.parseUnsignedLong(s, 16));
            }
            if (hasAlpha && s.length() == 8) {
                return (int) Long.parseUnsignedLong(s, 16);
            }
            if (!hasAlpha && s.length() == 8) {
                return (int) (0xFF000000L | (Long.parseUnsignedLong(s.substring(2), 16) & 0xFFFFFFL));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    static void insertHexText(ColorSetting.UiState ui, String text) {
        if (text == null || text.isEmpty()) return;
        deleteHexSelection(ui);
        StringBuilder insert = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isHexChar(c)) insert.append(Character.toUpperCase(c));
        }
        if (insert.length() == 0) {
            ui.hexErrorAnim = 1f;
            return;
        }
        int maxLen = 8;
        int allowed = Math.max(0, maxLen - ui.hexBuffer.length());
        if (insert.length() > allowed) {
            insert.setLength(allowed);
            ui.hexErrorAnim = 1f;
        }
        if (insert.length() == 0) return;
        ui.hexBuffer = ui.hexBuffer.substring(0, ui.hexCursor) + insert + ui.hexBuffer.substring(ui.hexCursor);
        ui.hexCursor += insert.length();
    }

    static void selectAllHex(ColorSetting.UiState ui) {
        ui.hexSelection.begin(0, 0);
        ui.hexSelection.updateCaret(0, ui.hexBuffer == null ? 0 : ui.hexBuffer.length());
        ui.hexCursor = ui.hexBuffer == null ? 0 : ui.hexBuffer.length();
    }

    static String selectedOrAllHex(ColorSetting.UiState ui) {
        if (hasHexSelection(ui)) return ui.hexBuffer.substring(hexSelStart(ui), hexSelEnd(ui));
        return ui.hexBuffer == null ? "" : ui.hexBuffer;
    }

    static boolean hasHexSelection(ColorSetting.UiState ui) {
        return ui.hexSelection.hasRange();
    }

    static int hexSelStart(ColorSetting.UiState ui) {
        return hasHexSelection(ui) ? Math.max(0, Math.min(ui.hexSelection.start(), ui.hexBuffer.length())) : -1;
    }

    static int hexSelEnd(ColorSetting.UiState ui) {
        return hasHexSelection(ui) ? Math.max(0, Math.min(ui.hexSelection.end(), ui.hexBuffer.length())) : -1;
    }

    static boolean deleteHexSelection(ColorSetting.UiState ui) {
        if (!hasHexSelection(ui)) return false;
        int start = hexSelStart(ui);
        int end = hexSelEnd(ui);
        ui.hexBuffer = ui.hexBuffer.substring(0, start) + ui.hexBuffer.substring(end);
        ui.hexCursor = start;
        ui.hexSelection.clear();
        return true;
    }

    static void moveHexCursor(ColorSetting.UiState ui, int dir, boolean shift) {
        setHexCursor(ui, Math.max(0, Math.min(ui.hexBuffer.length(), ui.hexCursor + dir)), shift);
    }

    static void setHexCursor(ColorSetting.UiState ui, int pos, boolean shift) {
        int next = Math.max(0, Math.min(ui.hexBuffer == null ? 0 : ui.hexBuffer.length(), pos));
        if (shift) {
            if (!ui.hexSelection.hasRange()) ui.hexSelection.begin(0, ui.hexCursor);
            ui.hexSelection.updateCaret(0, next);
        } else {
            ui.hexSelection.clear();
        }
        ui.hexCursor = next;
        ui.hexCursorBlink = 0f;
    }

    static void setHexCursorFromMouse(ColorSetting.UiState ui, float mx, boolean shift) {
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(11.5f, 5.4f);
        String label = "HEX";
        float tx = ui.hexX + SettingSkinUtil.m(18f, 12f);
        float labelW = UnifiedSettingsSkin.textWidth(font, label, size) + SettingSkinUtil.m(5f, 4f);
        float textX = tx + labelW + UnifiedSettingsSkin.textWidth(font, "#", size);
        int cursor = SettingTextInputUtil.caretFromText(font, ui.hexBuffer == null ? "" : ui.hexBuffer, size, textX, mx);
        setHexCursor(ui, cursor, shift);
    }
}
