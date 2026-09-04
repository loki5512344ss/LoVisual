/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;

final class KeyBindSettingRenderer {
    private KeyBindSettingRenderer() {
    }

    static void render(KeyBindSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        KeyBindSetting.UiState ui = setting.ui();
        String pending = ClickGuiRenderer.getPendingBindDisplay();
        String display = ui.waiting ? (pending == null ? "..." : pending) : setting.getValue().get();
        BindRendererUtil.renderBindBase(setting.getDisplayName(), display, null, ui.waiting, x, y, w, mx, my, ui);
    }

    static void mouseClicked(KeyBindSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        KeyBindSetting.UiState ui = setting.ui();
        if (!UnifiedSettingsSkin.inside(mx, my, ui.lastBx, ui.lastBy, ui.lastBw, ui.lastBh)) return;
        ui.waiting = true;
        ClickGuiRenderer.beginKeyBind(setting);
    }

    static float getHeight(KeyBindSetting setting) {
        return BindRendererUtil.bindHeight(setting.ui().lastW, setting.getDisplayName(), setting.getValue().get(), null);
    }
}