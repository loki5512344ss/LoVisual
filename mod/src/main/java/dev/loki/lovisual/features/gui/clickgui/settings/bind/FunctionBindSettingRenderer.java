package dev.loki.lovisual.features.gui.clickgui.settings.bind;

import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.util.BindRendererUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.bind.FunctionBindSetting;

public final class FunctionBindSettingRenderer {
    private FunctionBindSettingRenderer() {
    }

    public static void render(FunctionBindSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        FunctionBindSetting.UiState ui = setting.ui();
        String pending = ClickGuiRenderer.getPendingBindDisplay();
        String display = ui.waiting ? (pending == null ? "..." : pending) : setting.get();
        String modeText = setting.mode() == BindMode.HOLD ? "Hold" : "Press";
        BindRendererUtil.renderBindBase(setting.getDisplayName(), display, modeText, ui.waiting, x, y, w, mx, my, ui);
    }

    public static void mouseClicked(FunctionBindSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        FunctionBindSetting.UiState ui = setting.ui();
        if (!UnifiedSettingsSkin.inside(mx, my, ui.lastBx, ui.lastBy, ui.lastBw, ui.lastBh)) return;
        ui.waiting = true;
        ClickGuiRenderer.beginKeyBind(setting);
    }

    public static float getHeight(FunctionBindSetting setting) {
        String modeText = setting.mode() == BindMode.HOLD ? "Hold" : "Press";
        return BindRendererUtil.bindHeight(setting.ui().lastW, setting.getDisplayName(), setting.get(), modeText);
    }
}
