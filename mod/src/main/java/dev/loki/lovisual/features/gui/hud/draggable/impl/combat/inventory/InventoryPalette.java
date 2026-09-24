package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class InventoryPalette {

    private InventoryPalette() {}

    static void updatePalette(Inventory host) {
        if (host.isThemeMode()) {
            int alpha = host.bgAlpha.get();
            int window = HudRenderUtil.setAlpha(theme().windowBg(), alpha);
            int header = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowHeader(), theme().surface(), 0.18f),
                    Math.min(255, alpha + 14)
            );
            int surface = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowBg(), 0.22f),
                    Math.min(255, alpha + 6)
            );
            int deep = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowHeader(), 0.42f),
                    Math.min(255, alpha + 18)
            );

            host.uiHeaderLeft = HudRenderUtil.mixColor(header, window, 0.22f);
            host.uiHeaderRight = HudRenderUtil.mixColor(surface, header, 0.52f);
            host.uiBodyLeft = HudRenderUtil.mixColor(window, surface, 0.24f);
            host.uiBodyRight = HudRenderUtil.mixColor(deep, surface, 0.18f);
            host.uiOutline = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowStroke(), theme().strokeSoft(), 0.18f),
                    Math.min(255, Math.max(182, alpha + 36))
            );
            host.uiText = theme().textPrimary();
            host.uiTitleText = HudRenderUtil.mixColor(theme().textPrimary(), theme().accent(), 0.14f);
            host.uiMuted = HudRenderUtil.mixColor(theme().textMuted(), theme().textPrimary(), 0.16f);
            host.uiCounter = HudRenderUtil.mixColor(theme().accent(), theme().textPrimary(), 0.38f);
            host.uiDivider = HudRenderUtil.scaleAlpha(host.uiMuted, 0.68f);
            host.uiGridDivider = HudRenderUtil.scaleAlpha(host.uiMuted, 0.28f);
            host.uiBlurTint = HudRenderUtil.mixColor(
                    HudRenderUtil.mixColor(host.uiHeaderRight, host.uiBodyRight, 0.5f),
                    theme().accent(),
                    0.14f
            );
            return;
        }

        host.uiHeaderLeft = host.bg.getArgb();
        host.uiHeaderRight = host.bg2.getArgb();
        host.uiBodyLeft = HudRenderUtil.mixColor(host.bg.getArgb(), host.bg2.getArgb(), 0.15f);
        host.uiBodyRight = host.bg2.getArgb();
        host.uiOutline = host.stroke.getArgb();
        host.uiText = host.text.getArgb();
        host.uiTitleText = host.uiText;
        host.uiMuted = host.muted.getArgb();
        host.uiCounter = HudRenderUtil.mixColor(host.text.getArgb(), 0xFFE1E1FF, 0.20f);
        host.uiDivider = HudRenderUtil.scaleAlpha(host.uiMuted, 0.55f);
        host.uiGridDivider = HudRenderUtil.scaleAlpha(host.uiMuted, 0.20f);
        host.uiBlurTint = HudRenderUtil.mixColor(host.uiHeaderLeft, host.uiBodyRight, 0.5f);
    }
}
