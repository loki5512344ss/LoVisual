/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.wav.ClickGuiSounds;

final class ModeSettingRenderer {
    private ModeSettingRenderer() {
    }

    static void render(ModeSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        ModeSetting.UiState ui = setting.ui();
        ui.baseX = x;
        ui.baseY = y;
        ui.baseW = w;
        layoutMode(setting);

        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 6.5f);
        ClickGuiRenderer.drawText(font, setting.getDisplayName(), x + SettingSkinUtil.m(5f, 5f), y + SettingSkinUtil.m(4f, 1.3f), size, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        SettingSkinUtil.renderOptionContainer(x + SettingSkinUtil.m(5f, 5f), y + SettingSkinUtil.m(24f, 9.7f), ui.selectW, optionContainerHeightMode(ui), false);

        for (ModeSetting.OptionLayout opt : ui.optionLayouts) {
            float ox = x + opt.x();
            float oy = y + opt.y();
            boolean active = opt.getId().equals(setting.selectedId());
            boolean hover = UnifiedSettingsSkin.inside(mx, my, ox - SettingSkinUtil.m(2f, 2f), oy - SettingSkinUtil.m(1f, 1f), opt.w() + SettingSkinUtil.m(4f, 4f), SettingSkinUtil.m(16f, 8f));
            float hoverAnim = AnimationUtility.approach(ui.hoverAnims.getOrDefault(opt.getId(), 0f), hover ? 1f : 0f, 0.20f);
            ui.hoverAnims.put(opt.getId(), hoverAnim);
            int color = active ? UnifiedSettingsSkin.ACCENT : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, hoverAnim * 0.50f);
            ClickGuiRenderer.drawText(font, opt.label(), ox, oy, size, color, false);
        }
    }

    static void mouseClicked(ModeSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        ModeSetting.UiState ui = setting.ui();
        for (ModeSetting.OptionLayout opt : ui.optionLayouts) {
            float ox = ui.baseX + opt.x();
            float oy = ui.baseY + opt.y();
            if (!UnifiedSettingsSkin.inside(mx, my, ox - SettingSkinUtil.m(2f, 2f), oy - SettingSkinUtil.m(1f, 1f), opt.w() + SettingSkinUtil.m(4f, 4f), SettingSkinUtil.m(16f, 8f)))
                continue;
            String before = setting.selectedId();
            setting.selectId(opt.getId());
            if (!opt.getId().equals(before)) ClickGuiSounds.changeMode();
            if (setting.onChange() != null) setting.onChange().run();
            if (setting.getParent() != null) setting.getParent().saveConfig();
            return;
        }
    }

    static float getHeight(ModeSetting setting) {
        layoutMode(setting);
        return setting.ui().cachedHeight;
    }

    private static void layoutMode(ModeSetting setting) {
        ModeSetting.UiState ui = setting.ui();
        float width = ui.baseW > 0f ? ui.baseW : SettingSkinUtil.m(160f, 115f);
        int count = setting.optionIds().size();
        if (Float.isFinite(ui.lastLayoutW) && Math.abs(ui.lastLayoutW - width) <= 0.5f && ui.layoutOptionCount == count)
            return;
        ui.optionLayouts.clear();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 6.5f);
        float maxW = Math.max(1f, width - SettingSkinUtil.m(10f, 10f));
        float offsetX = 0f, offsetY = 0f, maxRowW = 0f;
        for (String id : setting.optionIds()) {
            String label = setting.getOptionDisplayName(id);
            float tw = UnifiedSettingsSkin.textWidth(font, label, size) + SettingSkinUtil.m(2f, 2f);
            if (offsetX + tw >= maxW && offsetX > 0f) {
                maxRowW = Math.max(maxRowW, offsetX);
                offsetX = 0f;
                offsetY += SettingSkinUtil.m(16f, 8f);
            }
            ui.optionLayouts.add(new ModeSetting.OptionLayout(id, label, SettingSkinUtil.m(7f, 6f) + offsetX, SettingSkinUtil.m(27f, 10.7f) + offsetY, tw));
            offsetX += tw;
        }
        maxRowW = Math.max(maxRowW, offsetX);
        ui.selectW = Math.max(0f, maxRowW + SettingSkinUtil.m(2f, 2f));
        ui.cachedHeight = SettingSkinUtil.m(45f, 22f) + offsetY;
        ui.lastLayoutW = width;
        ui.layoutOptionCount = count;
    }

    private static float optionContainerHeightMode(ModeSetting.UiState ui) {
        float max = SettingSkinUtil.m(10f, 10f);
        for (ModeSetting.OptionLayout opt : ui.optionLayouts) max = Math.max(max, opt.y() - SettingSkinUtil.m(19f, 9.7f) + SettingSkinUtil.m(16f, 8f));
        return max;
    }
}