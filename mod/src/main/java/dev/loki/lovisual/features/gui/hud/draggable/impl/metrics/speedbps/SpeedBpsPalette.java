package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class SpeedBpsPalette {

    private SpeedBpsPalette() {
    }

    static void update(SpeedBps host) {
        if (host.isThemeMode()) {
            int panelAlpha = host.bgAlpha.get();
            host.uiBgPrimary = HudRenderUtil.setAlpha(theme().windowBg(), panelAlpha);
            host.uiBgSecondary = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowHeader(), 0.35f),
                    panelAlpha
            );
            host.uiStroke = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowStroke(), theme().strokeSoft(), 0.4f),
                    Math.min(panelAlpha, 190)
            );
            host.uiIconColor = theme().textPrimary();
            host.uiValueColor = theme().textPrimary();
            host.uiMetaColor = theme().textMuted();
            return;
        }

        host.uiBgPrimary = host.bg.getArgb();
        host.uiBgSecondary = host.bg2.getArgb();
        host.uiStroke = host.stroke.getArgb();
        host.uiIconColor = host.iconColor.getArgb();
        host.uiValueColor = host.valueColor.getArgb();
        host.uiMetaColor = host.metaColor.getArgb();
    }
}
