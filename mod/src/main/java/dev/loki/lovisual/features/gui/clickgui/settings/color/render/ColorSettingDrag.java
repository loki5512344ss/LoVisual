package dev.loki.lovisual.features.gui.clickgui.settings.color.render;

import dev.loki.lovisual.config.values.color.ColorValue;

import java.awt.Color;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;

public final class ColorSettingDrag {
    private ColorSettingDrag() {
    }

    static boolean colorDragActive(ColorSetting.UiState ui) {
        return ui.sbFocused || ui.hFocused || ui.aFocused || ui.rFocused || ui.gFocused || ui.bFocused || ui.aRgbFocused;
    }

    static void clearColorFocus(ColorSetting.UiState ui) {
        ui.sbFocused = false;
        ui.hFocused = false;
        ui.aFocused = false;
        ui.rFocused = false;
        ui.gFocused = false;
        ui.bFocused = false;
        ui.aRgbFocused = false;
    }

    static void updateColorDrag(ColorSetting.UiState ui, ColorValue value, float mx, float my, boolean hasAlpha) {
        if (value.isRainbow()) return;
        if (ui.sbFocused) {
            ui.saturation = UnifiedSettingsSkin.clamp01((mx - ui.squareX) / Math.max(1f, ui.squareW));
            ui.brightness = UnifiedSettingsSkin.clamp01(1f - ((my - ui.squareY) / Math.max(1f, ui.squareH)));
            setColorFromHsb(ui, value);
        }
        if (ui.hFocused) {
            ui.hue = UnifiedSettingsSkin.clamp01((my - ui.hueY) / Math.max(1f, ui.hueH));
            setColorFromHsb(ui, value);
        }
        if (hasAlpha && ui.aFocused) {
            ui.alpha = Math.round(UnifiedSettingsSkin.clamp01(1f - ((my - ui.alphaY) / Math.max(1f, ui.alphaH))) * 255f);
            setColorFromHsb(ui, value);
        }
        if (ui.rFocused) {
            ui.red = Math.round(UnifiedSettingsSkin.clamp01((mx - ui.rX) / Math.max(1f, ui.rW)) * 255f);
            setColorFromRgb(ui, value);
        }
        if (ui.gFocused) {
            ui.green = Math.round(UnifiedSettingsSkin.clamp01((mx - ui.gX) / Math.max(1f, ui.gW)) * 255f);
            setColorFromRgb(ui, value);
        }
        if (ui.bFocused) {
            ui.blue = Math.round(UnifiedSettingsSkin.clamp01((mx - ui.bX) / Math.max(1f, ui.bW)) * 255f);
            setColorFromRgb(ui, value);
        }
        if (hasAlpha && ui.aRgbFocused) {
            ui.alpha = Math.round(UnifiedSettingsSkin.clamp01((mx - ui.aX) / Math.max(1f, ui.aW)) * 255f);
            setColorFromRgb(ui, value);
        }
    }

    public static void syncColorFromValue(ColorSetting.UiState ui, ColorValue value) {
        int argb = value.getArgb();
        ui.lastArgb = argb;
        ui.red = (argb >>> 16) & 0xFF;
        ui.green = (argb >>> 8) & 0xFF;
        ui.blue = argb & 0xFF;
        ui.alpha = (argb >>> 24) & 0xFF;
        float[] hsb = Color.RGBtoHSB(ui.red, ui.green, ui.blue, null);
        if (hsb[1] > 0.0001f && hsb[2] > 0.0001f) ui.hue = hsb[0];
        ui.saturation = hsb[1];
        ui.brightness = hsb[2];
    }

    static void setColorFromHsb(ColorSetting.UiState ui, ColorValue value) {
        int rgb = Color.HSBtoRGB(ui.hue, ui.saturation, ui.brightness) & 0x00FFFFFF;
        ui.red = (rgb >>> 16) & 0xFF;
        ui.green = (rgb >>> 8) & 0xFF;
        ui.blue = rgb & 0xFF;
        applyColor(value, ((value.supportsAlpha() ? ui.alpha : 255) << 24) | rgb);
    }

    static void setColorFromRgb(ColorSetting.UiState ui, ColorValue value) {
        float[] hsb = Color.RGBtoHSB(ui.red, ui.green, ui.blue, null);
        if (hsb[1] > 0.0001f && hsb[2] > 0.0001f) ui.hue = hsb[0];
        ui.saturation = hsb[1];
        ui.brightness = hsb[2];
        applyColor(value, ((value.supportsAlpha() ? ui.alpha : 255) << 24) | (ui.red << 16) | (ui.green << 8) | ui.blue);
    }

    public static void applyColor(ColorValue value, int argb) {
        if (value.supportsAlpha())
            value.set(String.format("#%02X%02X%02X%02X", (argb >>> 24) & 0xFF, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF));
        else value.set(String.format("#%02X%02X%02X", (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF));
    }
}
