package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import net.minecraft.client.Minecraft;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.tooltip.ClickGuiHintOverlay;
import dev.loki.lovisual.render.engine.math.HudScale;

/**
 * Подсказки внизу экрана для панели настроек: вариант HUD-редактора и
 * вариант обычной панели. Общая логика расчёта области — одна.
 */
final class SettingsPanelHints {

    private SettingsPanelHints() {}

    static void renderHudEditor(float fallbackX, float fallbackY, float fallbackW, float fallbackH,
                                float scale, float alpha) {
        ClickGuiHintOverlay.renderBottomLeft(
                0f, 0f, areaWidth(fallbackX, fallbackW), areaHeight(fallbackY, fallbackH),
                scale,
                alpha,
                ClickGuiI18n.tr("clickgui.hints.hud.alt", "Alt - drag without widget linking"),
                ClickGuiI18n.tr("clickgui.hints.hud.close", "Esc - close editor"),
                ClickGuiI18n.tr("clickgui.hints.hud.hide", "Alt+H - hide hints")
        );
    }

    static void renderSettingsPanel(float fallbackX, float fallbackY, float fallbackW, float fallbackH,
                                    float scale, float alpha) {
        ClickGuiHintOverlay.renderBottomLeft(
                0f, 0f, areaWidth(fallbackX, fallbackW), areaHeight(fallbackY, fallbackH),
                scale,
                alpha,
                ClickGuiI18n.tr("clickgui.hints.settings_panel.change", "LMB - change focused setting"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.drag", "Drag header - move this panel"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.scroll", "Wheel - scroll settings"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.back", "Esc - back to category"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.hide", "Alt+H - hide hints")
        );
    }

    private static float areaWidth(float fallbackX, float fallbackW) {
        float areaW = Math.max(1f, fallbackX + fallbackW);
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            areaW = Math.max(1f, HudScale.virtualWidth(mc.getWindow().getWidth(), mc.getWindow().getHeight()));
        }
        return areaW;
    }

    private static float areaHeight(float fallbackY, float fallbackH) {
        float areaH = Math.max(1f, fallbackY + fallbackH);
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            areaH = Math.max(1f, HudScale.virtualHeight(mc.getWindow().getWidth(), mc.getWindow().getHeight()));
        }
        return areaH;
    }
}
