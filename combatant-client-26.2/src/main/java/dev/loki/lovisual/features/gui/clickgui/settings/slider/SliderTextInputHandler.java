/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.slider;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.text.edit.ClipboardUtil;
import dev.loki.lovisual.util.wav.util.ClickGuiSounds;
import org.lwjgl.glfw.GLFW;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.util.SettingTextInputUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderTextInputState;

public final class SliderTextInputHandler {
    private SliderTextInputHandler() {
    }

    static void beginSliderEdit(SliderSetting<?> setting) {
        SliderSetting.UiState ui = setting.ui();
        ui.editing = true;
        ui.dragging = false;
        ui.editBuffer = String.valueOf(setting.value().get()).replace(',', '.');
        ui.editCursor = ui.editBuffer.length();
        ui.editSelection.clear();
        ui.errorAnim = 0f;
        ui.cursorBlink = 0f;
    }

    static void cancelSliderEdit(SliderSetting<?> setting) {
        SliderSetting.UiState ui = setting.ui();
        ui.editing = false;
        ui.editBuffer = "";
        ui.editCursor = 0;
        ui.editSelection.clear();
        ui.errorAnim = 0f;
    }

    public static <N extends Number> void commitSliderEdit(SliderSetting<N> setting, boolean save) {
        SliderSetting.UiState ui = setting.ui();
        String raw = ui.editBuffer == null ? "" : ui.editBuffer.trim().replace(',', '.');
        if (!SliderTextInputState.sliderTextCanCommit(setting, raw)) {
            SliderTextInputState.pulseSliderError(ui);
            return;
        }
        try {
            double parsed = Double.parseDouble(raw);
            double min = setting.value().getMin().doubleValue();
            double max = setting.value().getMax().doubleValue();
            parsed = Math.max(min, Math.min(max, parsed));
            if (SliderTextInputState.sliderInteger(setting)) parsed = Math.round(parsed);
            else parsed = Math.round(parsed * 1000d) / 1000d;
            N next = setting.value().castToType(parsed);
            setting.value().set(next);
            ui.editing = false;
            ui.editBuffer = "";
            ui.editCursor = 0;
            ui.editSelection.clear();
            if (save && setting.getParent() != null) setting.getParent().saveConfig();
        } catch (Throwable ignored) {
            SliderTextInputState.pulseSliderError(ui);
        }
    }

    static void setSliderEditCursorFromMouse(SliderSetting<?> setting, float mx, boolean shift) {
        SliderSetting.UiState ui = setting.ui();
        TextRenderer font = SettingSkinUtil.modules() ? UnifiedSettingsSkin.fontRegular() : ClickGuiRenderer.getIosevkaRegular();
        int cursor = SettingTextInputUtil.caretFromText(font, ui.editBuffer == null ? "" : ui.editBuffer, ui.valueTextSize <= 0f ? SettingSkinUtil.m(17f, 6.8f) : ui.valueTextSize, ui.valueTextX, mx);
        SliderTextInputState.setSliderCursor(ui, cursor, shift);
    }

    public static <N extends Number> void insertSliderText(SliderSetting<N> setting, String text) {
        String sanitized = SliderTextInputState.sanitizeSliderText(text, SliderTextInputState.sliderInteger(setting));
        if (sanitized.isEmpty()) {
            SliderTextInputState.pulseSliderError(setting.ui());
            return;
        }
        SliderSetting.UiState ui = setting.ui();
        if (SliderTextInputState.hasSliderSelection(ui)) SliderTextInputState.deleteSliderSelection(ui);
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            if (c == '.' && (SliderTextInputState.sliderInteger(setting) || SliderTextInputState.sliderTextHasDecimal(ui.editBuffer))) {
                SliderTextInputState.pulseSliderError(ui);
                continue;
            }
            if (c == '-' && (setting.value().getMin().doubleValue() >= 0.0 || ui.editCursor != 0 || ui.editBuffer.indexOf('-') >= 0)) {
                SliderTextInputState.pulseSliderError(ui);
                continue;
            }
            SliderTextInputState.insertSliderTextRaw(ui, String.valueOf(c));
        }
        SliderTextInputState.validateSliderLive(setting, ui);
    }

    public static <N extends Number> void setSliderValueFromMouse(SliderSetting<N> setting, float mx, float trackX, float trackW, double min, double max) {
        float percent = UnifiedSettingsSkin.clamp01((mx - trackX) / Math.max(1f, trackW));
        double nextRaw = min + (max - min) * percent;
        NumberValue<N> value = setting.value();
        if (value.get() instanceof Integer || value.get() instanceof Long)
            nextRaw = Math.round(nextRaw);
        else nextRaw = Math.round(nextRaw * 100d) / 100d;
        N current = value.get();
        N next = value.castToType(nextRaw);
        if (Double.compare(current.doubleValue(), next.doubleValue()) != 0) {
            value.set(next);
            ClickGuiSounds.sliderMove();
        }
    }

    public static <N extends Number> boolean keyPressed(SliderSetting<N> setting, int keyCode, int scanCode, int modifiers) {
        SliderSetting.UiState ui = setting.ui();
        if (!ui.editing) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) { SliderTextInputState.selectAllSliderText(ui); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) { ClipboardUtil.copy(SliderTextInputState.selectedOrAllSliderText(ui)); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) { insertSliderText(setting, ClipboardUtil.get()); return true; }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            ClipboardUtil.copy(SliderTextInputState.selectedOrAllSliderText(ui));
            SliderTextInputState.deleteSliderSelection(ui);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { commitSliderEdit(setting, true); return true; }
            case GLFW.GLFW_KEY_ESCAPE -> { cancelSliderEdit(setting); return true; }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!SliderTextInputState.deleteSliderSelection(ui) && ui.editCursor > 0) {
                    ui.editBuffer = ui.editBuffer.substring(0, ui.editCursor - 1) + ui.editBuffer.substring(ui.editCursor);
                    ui.editCursor--;
                }
                SliderTextInputState.validateSliderLive(setting, ui);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (!SliderTextInputState.deleteSliderSelection(ui) && ui.editCursor < ui.editBuffer.length()) {
                    ui.editBuffer = ui.editBuffer.substring(0, ui.editCursor) + ui.editBuffer.substring(ui.editCursor + 1);
                }
                SliderTextInputState.validateSliderLive(setting, ui);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> { SliderTextInputState.moveSliderCursor(ui, -1, shift); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { SliderTextInputState.moveSliderCursor(ui, 1, shift); return true; }
            case GLFW.GLFW_KEY_HOME -> { SliderTextInputState.setSliderCursor(ui, 0, shift); return true; }
            case GLFW.GLFW_KEY_END -> { SliderTextInputState.setSliderCursor(ui, ui.editBuffer.length(), shift); return true; }
        }
        return true;
    }

    public static <N extends Number> boolean charTyped(SliderSetting<N> setting, char chr, int modifiers) {
        SliderSetting.UiState ui = setting.ui();
        if (!ui.editing) return false;
        if (chr == ',' || chr == '.') {
            if (SliderTextInputState.sliderInteger(setting) || SliderTextInputState.sliderTextHasDecimal(ui.editBuffer)) {
                SliderTextInputState.pulseSliderError(ui);
                return true;
            }
            SliderTextInputState.insertSliderTextRaw(ui, ".");
            SliderTextInputState.validateSliderLive(setting, ui);
            return true;
        }
        if (chr == '-') {
            if (setting.value().getMin().doubleValue() >= 0.0) {
                SliderTextInputState.pulseSliderError(ui);
                return true;
            }
            if (SliderTextInputState.hasSliderSelection(ui)) SliderTextInputState.deleteSliderSelection(ui);
            if (ui.editCursor != 0 || ui.editBuffer.indexOf('-') >= 0) {
                SliderTextInputState.pulseSliderError(ui);
                return true;
            }
            SliderTextInputState.insertSliderTextRaw(ui, "-");
            SliderTextInputState.validateSliderLive(setting, ui);
            return true;
        }
        if (chr >= '0' && chr <= '9') {
            SliderTextInputState.insertSliderTextRaw(ui, String.valueOf(chr));
            SliderTextInputState.validateSliderLive(setting, ui);
            return true;
        }
        SliderTextInputState.pulseSliderError(ui);
        return true;
    }
}
