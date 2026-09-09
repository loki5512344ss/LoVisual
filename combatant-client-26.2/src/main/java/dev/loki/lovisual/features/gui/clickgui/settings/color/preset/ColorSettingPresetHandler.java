/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.color.preset;

import dev.loki.lovisual.config.values.ColorValue;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.color.preset.ColorPresetStore;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingDrag;
import dev.loki.lovisual.features.gui.clickgui.settings.color.render.ColorSettingLayout;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;

public final class ColorSettingPresetHandler {
    private ColorSettingPresetHandler() {
    }

    public static boolean handleColorPresetClick(ColorSetting setting, ColorValue value, ColorSetting.UiState ui, double mx, double my, int button) {
        if (button != 0 && button != 1) return false;
        float slide = (1f - ColorSettingLayout.colorReveal(ui)) * SettingSkinUtil.m(8f, 8f);
        float rowY = ui.presetY + slide;
        float swatch = SettingSkinUtil.m(12f, 8f);
        float gap = SettingSkinUtil.m(4f, 3f);
        float sx = ui.presetX + swatch + gap;
        float available = Math.max(1f, ui.presetToggleX - gap - sx);
        java.util.List<Integer> presets = ColorPresetStore.presets();
        int max = Math.max(1, (int) Math.floor(available / (swatch + gap)));
        for (int i = 0; i < Math.min(max, presets.size()); i++) {
            float px = sx + i * (swatch + gap);
            if (!UnifiedSettingsSkin.inside(mx, my, px, rowY + (ui.presetH - swatch) * 0.5f, swatch, swatch)) continue;
            int color = presets.get(i);
            if (button == 1) ColorPresetStore.removeExact(color);
            else ColorSettingDrag.applyColor(value, color);
            ColorSettingDrag.syncColorFromValue(ui, value);
            if (setting.getParent() != null) setting.getParent().saveConfig();
            return true;
        }
        if (button == 0 && UnifiedSettingsSkin.inside(mx, my, ui.presetToggleX, rowY, ui.presetToggleW, ui.presetToggleH)) {
            ui.presetsExpanded = !ui.presetsExpanded;
            return true;
        }
        if (button == 0 && UnifiedSettingsSkin.inside(mx, my, ui.savePresetX, rowY, ui.savePresetW, ui.savePresetH)) {
            ColorPresetStore.add(value.getArgb());
            return true;
        }
        if (button == 0 && UnifiedSettingsSkin.inside(mx, my, ui.deletePresetX, rowY, ui.deletePresetW, ui.deletePresetH)) {
            ColorPresetStore.removeClosest(value.getArgb());
            return true;
        }

        if (ui.presetsExpanded && ui.presetsExpandAnim > 0.05f) {
            float listY = ui.presetListY + slide;
            if (button == 0 && ui.presetsMaxScroll > 0.5f && UnifiedSettingsSkin.inside(mx, my, ui.presetScrollbarX - SettingSkinUtil.m(3f, 2f), ui.presetScrollbarY, ui.presetScrollbarW + SettingSkinUtil.m(6f, 4f), ui.presetScrollbarH)) {
                ui.presetsScrollbarDragging = true;
                ui.presetsScrollbarDragOffset = (float) my - ui.presetScrollbarHandleY;
                updatePresetScrollbarDrag(ui, (float) my);
                return true;
            }
            if (UnifiedSettingsSkin.inside(mx, my, ui.presetListX, listY, ui.presetListW, Math.max(1f, ui.presetListH))) {
                float pad = SettingSkinUtil.m(5f, 4f);
                float innerX = ui.presetListX + pad;
                float innerY = listY + pad;
                int cols = Math.max(1, (int) Math.floor((ui.presetListW - pad * 2f - SettingSkinUtil.m(7f, 5f)) / (swatch + gap)));
                float rowH = swatch + gap;
                float localX = (float) mx - innerX;
                float localY = (float) my - innerY + ui.presetsScroll;
                int col = (int) Math.floor(localX / (swatch + gap));
                int row = (int) Math.floor(localY / rowH);
                if (col >= 0 && col < cols && row >= 0) {
                    float cellX = col * (swatch + gap);
                    float cellY = row * rowH;
                    if (localX >= cellX && localX <= cellX + swatch && localY >= cellY && localY <= cellY + swatch) {
                        int index = row * cols + col;
                        if (index >= 0 && index < presets.size()) {
                            int color = presets.get(index);
                            if (button == 1) ColorPresetStore.removeExact(color);
                            else ColorSettingDrag.applyColor(value, color);
                            ColorSettingDrag.syncColorFromValue(ui, value);
                            if (setting.getParent() != null) setting.getParent().saveConfig();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    static void updatePresetScrollbarDrag(ColorSetting.UiState ui, float my) {
        if (ui.presetsMaxScroll <= 0.5f) return;
        float track = Math.max(1f, ui.presetScrollbarH - ui.presetScrollbarHandleH);
        float handleY = AnimationUtility.clamp(my - ui.presetsScrollbarDragOffset, ui.presetScrollbarY, ui.presetScrollbarY + track);
        float t = (handleY - ui.presetScrollbarY) / track;
        ui.presetsScroll = AnimationUtility.clamp(t * ui.presetsMaxScroll, 0f, ui.presetsMaxScroll);
    }
}
