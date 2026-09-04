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

import java.util.HashSet;
import java.util.Set;

final class GroupSettingRenderer {
    private GroupSettingRenderer() {
    }

    static void render(GroupSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        GroupSetting.UiState ui = setting.ui();
        ui.baseX = x;
        ui.baseY = y;
        ui.baseW = w;
        layoutGroup(setting);

        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 6.5f);
        ClickGuiRenderer.drawText(font, setting.getDisplayName(), x + SettingSkinUtil.m(5f, 5f), y + SettingSkinUtil.m(4f, 1.7f), size, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        SettingSkinUtil.renderOptionContainer(x + SettingSkinUtil.m(6f, 6f), y + SettingSkinUtil.m(24f, 9.7f), Math.max(1f, containerWidth(ui.optionLayouts)), optionContainerHeightGroup(ui), false);

        for (GroupSetting.OptionLayout opt : ui.optionLayouts) {
            float ox = x + opt.x();
            float oy = y + opt.y();
            boolean active = setting.value().get(opt.getId());
            boolean hover = UnifiedSettingsSkin.inside(mx, my, ox - SettingSkinUtil.m(2f, 2f), oy - SettingSkinUtil.m(1f, 1f), opt.w() + SettingSkinUtil.m(4f, 4f), SettingSkinUtil.m(16f, 8f));
            float hoverAnim = AnimationUtility.approach(ui.hoverAnims.getOrDefault(opt.getId(), 0f), hover ? 1f : 0f, 0.20f);
            float activeAnim = AnimationUtility.approach(ui.selectAnims.getOrDefault(opt.getId(), active ? 1f : 0f), active ? 1f : 0f, 0.22f);
            ui.hoverAnims.put(opt.getId(), hoverAnim);
            ui.selectAnims.put(opt.getId(), activeAnim);
            int color = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, 0.72f + hoverAnim * 0.28f);
            color = UnifiedSettingsSkin.mix(color, UnifiedSettingsSkin.ACCENT, activeAnim);
            ClickGuiRenderer.drawText(font, opt.label(), ox, oy, size, color, false);
        }
    }

    static void mouseClicked(GroupSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        GroupSetting.UiState ui = setting.ui();
        for (GroupSetting.OptionLayout opt : ui.optionLayouts) {
            float ox = ui.baseX + opt.x();
            float oy = ui.baseY + opt.y();
            if (!UnifiedSettingsSkin.inside(mx, my, ox - SettingSkinUtil.m(2f, 2f), oy - SettingSkinUtil.m(1f, 1f), opt.w() + SettingSkinUtil.m(4f, 4f), SettingSkinUtil.m(16f, 8f)))
                continue;
            setting.value().set(opt.getId(), !setting.value().get(opt.getId()));
            ClickGuiSounds.changeMode();
            if (setting.getParent() != null) setting.getParent().saveConfig();
            return;
        }
    }

    static float getHeight(GroupSetting setting) {
        layoutGroup(setting);
        return setting.ui().cachedHeight;
    }

    private static void layoutGroup(GroupSetting setting) {
        GroupSetting.UiState ui = setting.ui();
        float width = ui.baseW > 0f ? ui.baseW : SettingSkinUtil.m(160f, 115f);
        int count = visibleGroupCount(setting);
        if (Float.isFinite(ui.lastLayoutW) && Math.abs(ui.lastLayoutW - width) <= 0.5f && ui.layoutOptionCount == count)
            return;
        ui.optionLayouts.clear();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 6.5f);
        float maxW = Math.max(1f, width - SettingSkinUtil.m(10f, 10f));
        float offsetX = 0f, offsetY = 0f, maxRowW = 0f;
        Set<String> alive = new HashSet<>();
        for (String id : setting.value().getAll().keySet()) {
            if (!setting.isOptionVisible(id)) continue;
            String label = setting.getOptionDisplayName(id);
            float tw = UnifiedSettingsSkin.textWidth(font, label, size) + SettingSkinUtil.m(4f, 4f);
            if (offsetX + tw >= maxW && offsetX > 0f) {
                maxRowW = Math.max(maxRowW, offsetX);
                offsetX = 0f;
                offsetY += SettingSkinUtil.m(16f, 8f);
            }
            ui.optionLayouts.add(new GroupSetting.OptionLayout(id, label, SettingSkinUtil.m(8f, 8f) + offsetX, SettingSkinUtil.m(27f, 10.7f) + offsetY, tw));
            offsetX += tw;
            alive.add(id);
        }
        maxRowW = Math.max(maxRowW, offsetX);
        ui.cachedHeight = SettingSkinUtil.m(45f, 22f) + offsetY;
        ui.lastLayoutW = width;
        ui.layoutOptionCount = count;
        ui.hoverAnims.keySet().removeIf(key -> !alive.contains(key));
        ui.selectAnims.keySet().removeIf(key -> !alive.contains(key));
    }

    private static int visibleGroupCount(GroupSetting setting) {
        int count = 0;
        for (String id : setting.value().getAll().keySet()) if (setting.isOptionVisible(id)) count++;
        return count;
    }

    private static float containerWidth(java.util.List<GroupSetting.OptionLayout> layouts) {
        float max = 0f;
        for (GroupSetting.OptionLayout opt : layouts) max = Math.max(max, opt.x() - SettingSkinUtil.m(6f, 6f) + opt.w());
        return max + SettingSkinUtil.m(0.5f, 0.5f);
    }

    private static float optionContainerHeightGroup(GroupSetting.UiState ui) {
        float max = SettingSkinUtil.m(10f, 10f);
        for (GroupSetting.OptionLayout opt : ui.optionLayouts) max = Math.max(max, opt.y() - SettingSkinUtil.m(19f, 9.7f) + SettingSkinUtil.m(16f, 8f));
        return max;
    }
}