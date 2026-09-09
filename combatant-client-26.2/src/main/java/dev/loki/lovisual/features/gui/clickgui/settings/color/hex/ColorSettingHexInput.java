/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.color.hex;

import dev.loki.lovisual.config.values.ColorValue;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.ScissorFunction;
import dev.loki.lovisual.util.text.ClipboardUtil;
import org.lwjgl.glfw.GLFW;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.color.hex.ColorSettingHexState;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingDrag;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.util.SettingTextInputUtil;

public final class ColorSettingHexInput {
    private ColorSettingHexInput() {
    }

    public static void beginHexEdit(ColorSetting.UiState ui, ColorValue value) {
        ui.hexFocused = true;
        ui.hexBuffer = ColorSettingHexState.displayHex(value, value.supportsAlpha());
        ui.hexCursor = ui.hexBuffer.length();
        ui.hexSelection.clear();
        ui.hexErrorAnim = 0f;
    }

    static void cancelHexEdit(ColorSetting.UiState ui, ColorValue value) {
        ui.hexFocused = false;
        ui.hexBuffer = ColorSettingHexState.displayHex(value, value.supportsAlpha());
        ui.hexCursor = ui.hexBuffer.length();
        ui.hexSelection.clear();
        ui.hexErrorAnim = 0f;
    }

    public static boolean commitHexEdit(ColorSetting setting, ColorValue value, ColorSetting.UiState ui, boolean save) {
        Integer parsed = ColorSettingHexState.parseHexInput(ui.hexBuffer, value.supportsAlpha());
        if (parsed == null) {
            ui.hexErrorAnim = 1f;
            return false;
        }
        ColorSettingDrag.applyColor(value, parsed);
        ColorSettingDrag.syncColorFromValue(ui, value);
        ui.hexFocused = false;
        ui.hexSelection.clear();
        if (save && setting.getParent() != null) setting.getParent().saveConfig();
        return true;
    }

    public static boolean keyPressed(ColorSetting setting, int keyCode, int scanCode, int modifiers) {
        ColorSetting.UiState ui = setting.ui();
        if (!ui.hexFocused) return false;
        ColorValue value = setting.value();
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            ColorSettingHexState.selectAllHex(ui);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            ClipboardUtil.copy("#" + ColorSettingHexState.selectedOrAllHex(ui));
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            ColorSettingHexState.insertHexText(ui, ClipboardUtil.get());
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            ClipboardUtil.copy("#" + ColorSettingHexState.selectedOrAllHex(ui));
            ColorSettingHexState.deleteHexSelection(ui);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commitHexEdit(setting, value, ui, true);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                cancelHexEdit(ui, value);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!ColorSettingHexState.deleteHexSelection(ui) && ui.hexCursor > 0) {
                    ui.hexBuffer = ui.hexBuffer.substring(0, ui.hexCursor - 1) + ui.hexBuffer.substring(ui.hexCursor);
                    ui.hexCursor--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (!ColorSettingHexState.deleteHexSelection(ui) && ui.hexCursor < ui.hexBuffer.length())
                    ui.hexBuffer = ui.hexBuffer.substring(0, ui.hexCursor) + ui.hexBuffer.substring(ui.hexCursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                ColorSettingHexState.moveHexCursor(ui, -1, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                ColorSettingHexState.moveHexCursor(ui, 1, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                ColorSettingHexState.setHexCursor(ui, 0, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                ColorSettingHexState.setHexCursor(ui, ui.hexBuffer.length(), (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
        }
        return true;
    }

    public static boolean charTyped(ColorSetting setting, char chr, int modifiers) {
        ColorSetting.UiState ui = setting.ui();
        if (!ui.hexFocused) return false;
        if (ColorSettingHexState.isHexChar(chr)) {
            ColorSettingHexState.insertHexText(ui, String.valueOf(chr));
        } else if (chr == '#') {
            // accepted as no-op so pasted/manual values with # don't flash error
        } else {
            ui.hexErrorAnim = 1f;
        }
        return true;
    }

    public static void renderHexInput(ColorSetting.UiState ui, ColorValue value, float mx, float my, float slide) {
        float y = ui.hexY + slide;
        boolean hover = UnifiedSettingsSkin.inside(mx, my, ui.hexX, y, ui.hexW, ui.hexH);
        float dt = AnimationUtility.deltaTime();
        ui.hexAnim = AnimationUtility.approach(ui.hexAnim, ui.hexFocused ? 1f : hover ? 0.55f : 0f, dt, 13f);
        ui.hexErrorAnim = AnimationUtility.approach(ui.hexErrorAnim, 0f, dt, 7.5f);
        if (ui.hexFocused) ui.hexCursorBlink += dt * 2.2f;
        else ui.hexCursorBlink = 0f;
        int bg = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE_SOFT, UnifiedSettingsSkin.SURFACE_HOVER, ui.hexAnim), 58 + Math.round(35f * ui.hexAnim));
        int stroke = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 70), 0xFFFF5F66, ui.hexErrorAnim);
        ClickGuiRenderer.drawRoundedRect(ui.hexX, y, ui.hexW, ui.hexH, SettingSkinUtil.m(5f, 3f), bg);
        ClickGuiRenderer.drawRoundedRectStroke(ui.hexX, y, ui.hexW, ui.hexH, SettingSkinUtil.m(5f, 3f), SettingSkinUtil.m(0.7f, 0.45f), stroke);
        int icon = UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 185);
        Renderer2D.COLOR.svg("brush", ui.hexX + SettingSkinUtil.m(5f, 3f), y + (ui.hexH - SettingSkinUtil.m(9f, 6f)) * 0.5f, SettingSkinUtil.m(9f, 6f), SettingSkinUtil.m(9f, 6f),
                SvgRenderOptions.overrideColor(icon));
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(11.5f, 5.4f);
        String text = ui.hexFocused ? ui.hexBuffer : ColorSettingHexState.displayHex(value, value.supportsAlpha());
        String label = "HEX";
        float tx = ui.hexX + SettingSkinUtil.m(18f, 12f);
        ClickGuiRenderer.drawText(font, label, tx, y + (ui.hexH - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f, size, UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 175), false);
        float labelW = UnifiedSettingsSkin.textWidth(font, label, size) + SettingSkinUtil.m(5f, 4f);
        int textColor = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_PRIMARY, 0xFFFF7777, ui.hexErrorAnim);
        float textX = tx + labelW;
        float textY = y + (ui.hexH - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f;
        float textClipW = Math.max(1f, ui.hexW - (textX - ui.hexX) - SettingSkinUtil.m(25f, 17f));
        boolean hexClip = ScissorFunction.pushRaw(textX, y, textClipW, ui.hexH);
        String prefix = "#";
        String draw = prefix + text;
        if (ui.hexFocused) {
            SettingTextInputUtil.renderInlineSelection(font, text, ColorSettingHexState.hexSelStart(ui), ColorSettingHexState.hexSelEnd(ui), textX + UnifiedSettingsSkin.textWidth(font, prefix, size), textY, size, ui.hexH, y);
            ClickGuiRenderer.drawText(font, draw, textX, textY, size, textColor, false);
            if (((int) (ui.hexCursorBlink * 2f) & 1) == 0) {
                int cursor = Math.max(0, Math.min(ui.hexCursor, ui.hexBuffer.length()));
                float cx = textX + UnifiedSettingsSkin.textWidth(font, prefix + ui.hexBuffer.substring(0, cursor), size) + SettingSkinUtil.m(0.5f, 0.3f);
                ClickGuiRenderer.drawRect(cx, y + SettingSkinUtil.m(3f, 2f), SettingSkinUtil.m(0.65f, 0.4f), ui.hexH - SettingSkinUtil.m(6f, 4f), UnifiedSettingsSkin.withAlpha(textColor, 190));
            }
        } else {
            String display = "#" + UnifiedSettingsSkin.fit(font, text, size, textClipW);
            ClickGuiRenderer.drawText(font, display, textX, textY, size, textColor, false);
        }
        if (hexClip) ScissorFunction.pop();
        float preview = ui.hexH - SettingSkinUtil.m(5f, 3f);
        float px = ui.hexX + ui.hexW - preview - SettingSkinUtil.m(3f, 2f);
        SettingSkinUtil.renderChecker(px, y + (ui.hexH - preview) * 0.5f, preview, preview, SettingSkinUtil.m(3f, 2f), SettingSkinUtil.m(3f, 2f));
        ClickGuiRenderer.drawRoundedRect(px, y + (ui.hexH - preview) * 0.5f, preview, preview, SettingSkinUtil.m(3f, 2f), value.getArgb());
    }
}
