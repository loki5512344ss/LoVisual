/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

final class ColorSettingLayout {
    private ColorSettingLayout() {
    }

    static float colorHeaderH() {
        return SettingSkinUtil.m(48f, 18f);
    }

    static float colorExpandedHeight(ColorSetting.UiState ui, boolean hasAlpha) {
        int bars = hasAlpha ? 4 : 3;
        float rgbBlock = (SettingSkinUtil.m(8f, 7f) + bars * SettingSkinUtil.m(9f, 7f) + (bars - 1) * SettingSkinUtil.m(5f, 5f)) * colorRgbReveal(ui);
        float presets = SettingSkinUtil.m(20f, 12f);
        float presetsExpanded = (SettingSkinUtil.m(5f, 3f) + SettingSkinUtil.m(44f, 31f)) * colorPresetReveal(ui);
        float hex = SettingSkinUtil.m(18f, 12f);
        float actions = SettingSkinUtil.m(17f, 10.5f);
        return Math.max(0f, colorHeaderH() + SettingSkinUtil.m(6f, 5f) + SettingSkinUtil.m(82f, 60f) + rgbBlock + SettingSkinUtil.m(6f, 4f) + presets + presetsExpanded + SettingSkinUtil.m(5f, 3f) + hex + SettingSkinUtil.m(5f, 3f) + actions + SettingSkinUtil.m(14f, 8f) - colorHeaderH());
    }

    static float colorRgbReveal(ColorSetting.UiState ui) {
        return AnimationUtility.easeInOutCubic(UnifiedSettingsSkin.clamp01(ui.rgbExpandAnim));
    }

    static float colorPresetReveal(ColorSetting.UiState ui) {
        return AnimationUtility.easeInOutCubic(UnifiedSettingsSkin.clamp01(ui.presetsExpandAnim));
    }

    static float colorLayoutAnim(ColorSetting.UiState ui) {
        float t = UnifiedSettingsSkin.clamp01(ui.expandAnim);
        return ui.editing ? AnimationUtility.clamp(AnimationUtility.easeOutBack(t, 0.42f), 0f, 1.02f) : 1f - AnimationUtility.easeInCubic(1f - t);
    }

    static float colorReveal(ColorSetting.UiState ui) {
        float t = UnifiedSettingsSkin.clamp01(ui.expandAnim);
        if (ui.editing) return AnimationUtility.easeInOutCubic(UnifiedSettingsSkin.clamp01((t - 0.08f) / 0.92f));
        return 1f - AnimationUtility.easeInCubic(1f - t);
    }

    static void layoutColor(ColorSetting.UiState ui, float x, float y, float w, boolean hasAlpha) {
        int verticalBars = hasAlpha ? 2 : 1;
        float sidePad = SettingSkinUtil.m(5f, 5f);
        float rightInset = SettingSkinUtil.m(5f, 4f);
        float barW = SettingSkinUtil.m(6f, 4f);
        float barGap = SettingSkinUtil.m(7f, 5f);
        ui.squareX = x + sidePad;
        ui.squareY = y + colorHeaderH() + SettingSkinUtil.m(6f, 5f);
        ui.squareH = SettingSkinUtil.m(82f, 60f);
        ui.squareW = Math.max(SettingSkinUtil.m(74f, 44f), w - sidePad * 2f - rightInset - verticalBars * barW - verticalBars * barGap);
        ui.hueX = ui.squareX + ui.squareW + barGap;
        ui.hueY = ui.squareY;
        ui.hueW = barW;
        ui.hueH = ui.squareH;
        if (hasAlpha) {
            ui.alphaX = ui.hueX + barW + barGap;
            ui.alphaY = ui.squareY;
            ui.alphaW = barW;
            ui.alphaH = ui.squareH;
        } else {
            ui.alphaX = ui.hueX;
            ui.alphaY = ui.hueY;
            ui.alphaW = 0f;
            ui.alphaH = 0f;
        }
        float rgbReveal = colorRgbReveal(ui);
        int rgbBars = hasAlpha ? 4 : 3;
        float rgbGapY = SettingSkinUtil.m(5f, 5f);
        float rgbH = SettingSkinUtil.m(9f, 7f);
        float valueColumn = SettingSkinUtil.m(25f, 18f);
        float rgbBlockFull = SettingSkinUtil.m(8f, 7f) + rgbBars * rgbH + Math.max(0, rgbBars - 1) * rgbGapY;
        float rgbBlockH = rgbBlockFull * rgbReveal;
        ui.rX = ui.squareX;
        ui.rY = ui.squareY + ui.squareH + SettingSkinUtil.m(8f, 7f);
        ui.rW = Math.max(1f, ui.squareW - valueColumn);
        ui.rH = rgbH;
        ui.gX = ui.rX;
        ui.gY = ui.rY + rgbH + rgbGapY;
        ui.gW = ui.rW;
        ui.gH = ui.rH;
        ui.bX = ui.rX;
        ui.bY = ui.gY + rgbH + rgbGapY;
        ui.bW = ui.rW;
        ui.bH = ui.rH;
        if (hasAlpha) {
            ui.aX = ui.rX;
            ui.aY = ui.bY + rgbH + rgbGapY;
            ui.aW = ui.rW;
            ui.aH = ui.rH;
        } else {
            ui.aX = ui.rX;
            ui.aY = ui.bY;
            ui.aW = 0f;
            ui.aH = 0f;
        }
        ui.rgbBottom = ui.squareY + ui.squareH + rgbBlockH;

        float actionX = ui.squareX;
        float actionW = Math.max(1f, x + w - SettingSkinUtil.m(10f, 10f) - actionX);
        float gap = SettingSkinUtil.m(4f, 4f);
        ui.presetX = actionX;
        ui.presetY = ui.rgbBottom + SettingSkinUtil.m(6f, 4f);
        ui.presetW = actionW;
        ui.presetH = SettingSkinUtil.m(20f, 12f);

        float presetButtonW = SettingSkinUtil.m(20f, 14f);
        ui.presetToggleW = presetButtonW;
        ui.savePresetW = presetButtonW;
        ui.deletePresetW = presetButtonW;
        ui.presetToggleX = actionX + actionW - presetButtonW * 3f;
        ui.savePresetX = ui.presetToggleX + presetButtonW;
        ui.deletePresetX = ui.savePresetX + presetButtonW;
        ui.savePresetY = ui.presetY;
        ui.deletePresetY = ui.presetY;
        ui.presetToggleY = ui.presetY;
        ui.savePresetH = ui.presetH;
        ui.deletePresetH = ui.presetH;
        ui.presetToggleH = ui.presetH;

        float listReveal = colorPresetReveal(ui);
        float listGap = SettingSkinUtil.m(5f, 3f);
        float listFullH = SettingSkinUtil.m(44f, 31f);
        ui.presetListX = actionX;
        ui.presetListY = ui.presetY + ui.presetH + listGap;
        ui.presetListW = actionW;
        ui.presetListH = listFullH * listReveal;

        ui.hexX = actionX;
        ui.hexY = ui.presetY + ui.presetH + (listGap + listFullH) * listReveal + SettingSkinUtil.m(5f, 3f);
        ui.hexW = actionW;
        ui.hexH = SettingSkinUtil.m(18f, 12f);
        float actionY = ui.hexY + ui.hexH + SettingSkinUtil.m(5f, 3f);
        float bw = Math.max(1f, (actionW - gap) / 2f);
        float bh = SettingSkinUtil.m(17f, 10.5f);
        ui.sliderX = actionX;
        ui.sliderY = actionY;
        ui.sliderW = bw;
        ui.sliderH = bh;
        ui.rnbX = ui.sliderX + bw + gap;
        ui.rnbY = actionY;
        ui.rnbW = bw;
        ui.rnbH = bh;
    }
}