/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings;

import dev.loki.lovisual.config.values.BindMode;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;

final class FunctionBindSettingRenderer {
    private FunctionBindSettingRenderer() {
    }

    static void render(FunctionBindSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        FunctionBindSetting.UiState ui = setting.ui();
        String pending = ClickGuiRenderer.getPendingBindDisplay();
        String display = ui.waiting ? (pending == null ? "..." : pending) : setting.get();
        String modeText = setting.mode() == BindMode.HOLD ? "Hold" : "Press";
        BindRendererUtil.renderBindBase(setting.getDisplayName(), display, modeText, ui.waiting, x, y, w, mx, my, ui);
    }

    static void mouseClicked(FunctionBindSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        FunctionBindSetting.UiState ui = setting.ui();
        if (!UnifiedSettingsSkin.inside(mx, my, ui.lastBx, ui.lastBy, ui.lastBw, ui.lastBh)) return;
        ui.waiting = true;
        ClickGuiRenderer.beginKeyBind(setting);
    }

    static float getHeight(FunctionBindSetting setting) {
        String modeText = setting.mode() == BindMode.HOLD ? "Hold" : "Press";
        return BindRendererUtil.bindHeight(setting.ui().lastW, setting.getDisplayName(), setting.get(), modeText);
    }
}