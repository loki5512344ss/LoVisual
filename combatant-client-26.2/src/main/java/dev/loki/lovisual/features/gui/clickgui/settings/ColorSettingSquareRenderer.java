/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.ScissorFunction;

final class ColorSettingSquareRenderer {
    private ColorSettingSquareRenderer() {
    }

    static void renderColorSquare(ColorSetting.UiState ui, float y) {
        int left = ColorSettingMath.hsbToArgb(ui.hue, 0f, 1f, 255);
        int right = ColorSettingMath.hsbToArgb(ui.hue, 1f, 1f, 255);
        ClickGuiRenderer.drawRoundedRect(ui.squareX, y, ui.squareW, ui.squareH, SettingSkinUtil.m(6f, 6f), right);
        ClickGuiRenderer.drawGradientRect(ui.squareX, y, ui.squareW, ui.squareH, left, right, right, left);
        ClickGuiRenderer.drawGradientRect(ui.squareX, y, ui.squareW, ui.squareH, 0x00FFFFFF, 0x00FFFFFF, 0xFF000000, 0xFF000000);
    }

    static void renderHueBar(ColorSetting.UiState ui, float slide) {
        int steps = Math.max(1, Math.round(ui.hueH));
        for (int i = 0; i < steps; i++) {
            float h = i / Math.max(1f, ui.hueH - 1f);
            ClickGuiRenderer.drawCircle(ui.hueX + ui.hueW * 0.5f, ui.hueY + slide + i, ui.hueW * 0.5f, ColorSettingMath.hsbToArgb(h, 1f, 1f, 255));
        }
    }

    static void renderAlphaBar(ColorSetting.UiState ui, float slide) {
        int rgb = ColorSettingMath.hsbToArgb(ui.hue, ui.saturation, ui.brightness, 255);
        ClickGuiRenderer.drawGradientRect(ui.alphaX, ui.alphaY + slide, ui.alphaW, ui.alphaH, ColorSettingMath.withAlpha(rgb, 255), ColorSettingMath.withAlpha(rgb, 255), ColorSettingMath.withAlpha(rgb, 0), ColorSettingMath.withAlpha(rgb, 0));
    }

    static void renderRgbBars(ColorSetting.UiState ui, boolean hasAlpha, float slide) {
        float reveal = ColorSettingLayout.colorRgbReveal(ui);
        if (reveal <= 0.001f) return;
        int alpha = hasAlpha ? ui.alpha : 255;
        float top = ui.rY + slide - SettingSkinUtil.m(2f, 2f);
        float bottom = (hasAlpha ? ui.aY + ui.aH : ui.bY + ui.bH) + slide + SettingSkinUtil.m(2f, 2f);
        boolean clip = ScissorFunction.pushRaw(ui.rX - SettingSkinUtil.m(10f, 8f), top, ui.rW + SettingSkinUtil.m(40f, 28f), Math.max(1f, (bottom - top) * reveal));
        renderRgbBar("R", ui.rX, ui.rY + slide, ui.rW, ui.rH, (alpha << 24) | (ui.green << 8) | ui.blue, (alpha << 24) | (255 << 16) | (ui.green << 8) | ui.blue, ui.red / 255f, ui.red);
        renderRgbBar("G", ui.gX, ui.gY + slide, ui.gW, ui.gH, (alpha << 24) | (ui.red << 16) | ui.blue, (alpha << 24) | (ui.red << 16) | (255 << 8) | ui.blue, ui.green / 255f, ui.green);
        renderRgbBar("B", ui.bX, ui.bY + slide, ui.bW, ui.bH, (alpha << 24) | (ui.red << 16) | (ui.green << 8), (alpha << 24) | (ui.red << 16) | (ui.green << 8) | 255, ui.blue / 255f, ui.blue);
        if (hasAlpha) {
            SettingSkinUtil.renderChecker(ui.aX + SettingSkinUtil.m(8f, 8f), ui.aY + slide, Math.max(1f, ui.aW - SettingSkinUtil.m(16f, 8f)), ui.aH, SettingSkinUtil.m(3f, 3f), SettingSkinUtil.m(2.5f, 2.5f));
            renderRgbBar("A", ui.aX, ui.aY + slide, ui.aW, ui.aH, (ui.red << 16) | (ui.green << 8) | ui.blue, (255 << 24) | (ui.red << 16) | (ui.green << 8) | ui.blue, ui.alpha / 255f, ui.alpha);
        }
        if (clip) ScissorFunction.pop();
    }

    private static void renderRgbBar(String label, float x, float y, float w, float h, int c0, int c1, float progress, int value) {
        TextRenderer font = UnifiedSettingsSkin.fontLight();
        float size = SettingSkinUtil.m(11f, 5.5f);
        ClickGuiRenderer.drawText(font, label, x, y - SettingSkinUtil.m(0.3f, 0.3f), size, UnifiedSettingsSkin.TEXT_MUTED, false);
        float bx = x + SettingSkinUtil.m(8f, 8f);
        float bw = Math.max(1f, w - SettingSkinUtil.m(16f, 8f));
        ClickGuiRenderer.drawGradientRect(bx, y, bw, h, c0, c1, c1, c0);
        float px = bx + UnifiedSettingsSkin.clamp01(progress) * bw;
        ClickGuiRenderer.drawRoundedRect(px - SettingSkinUtil.m(1f, 1f), y - SettingSkinUtil.m(1f, 1f), SettingSkinUtil.m(2f, 2f), h + SettingSkinUtil.m(2f, 2f), SettingSkinUtil.m(1f, 1f), UnifiedSettingsSkin.TEXT_PRIMARY);
        String valueText = Integer.toString(Math.max(0, Math.min(255, value)));
        float valueSize = SettingSkinUtil.m(10.5f, 5.2f);
        float valueX = x + w + SettingSkinUtil.m(3f, 3f);
        float valueY = y + (h - UnifiedSettingsSkin.textHeight(font, valueSize)) * 0.5f - SettingSkinUtil.m(0.2f, 0.15f);
        ClickGuiRenderer.drawText(font, valueText, valueX, valueY, valueSize, UnifiedSettingsSkin.TEXT_MUTED, false);
    }

    static void renderColorMarkers(ColorSetting.UiState ui, boolean hasAlpha, float slide) {
        float sbX = ui.squareX + ui.saturation * ui.squareW;
        float sbY = ui.squareY + slide + (1f - ui.brightness) * ui.squareH;
        ClickGuiRenderer.drawCircle(sbX, sbY, SettingSkinUtil.m(4f, 4f), 0xFF000000);
        ClickGuiRenderer.drawCircle(sbX, sbY, SettingSkinUtil.m(3f, 3f), 0xFFFFFFFF);
        float hueY = ui.hueY + slide + ui.hue * Math.max(1f, ui.hueH - 1f);
        ClickGuiRenderer.drawCircle(ui.hueX + ui.hueW * 0.5f, hueY, SettingSkinUtil.m(4f, 4f), 0xFF000000);
        ClickGuiRenderer.drawCircle(ui.hueX + ui.hueW * 0.5f, hueY, SettingSkinUtil.m(3f, 3f), 0xFFFFFFFF);
        if (hasAlpha) {
            float alphaY = ui.alphaY + slide + (1f - ui.alpha / 255f) * Math.max(1f, ui.alphaH - 1f);
            ClickGuiRenderer.drawCircle(ui.alphaX + ui.alphaW * 0.5f, alphaY, SettingSkinUtil.m(4f, 4f), 0xFF000000);
            ClickGuiRenderer.drawCircle(ui.alphaX + ui.alphaW * 0.5f, alphaY, SettingSkinUtil.m(3f, 3f), 0xFFFFFFFF);
        }
    }
}