package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class TabListPalette {
    int fillTop;
    int fillBottom;
    int fillHotspot;
    int stroke;
    int strokeSoft;
    int accent;
    int accentSoft;
    int accentGlow;
    int text;
    int muted;
    int rowFill;
    int rowAltFill;
    int rowHighlight;
    int pingChip;
    int divider;

    void update(boolean syncTheme, int themeAlpha, int bgArgb, int accentArgb, int textArgb, int mutedArgb) {
        if (syncTheme) {
            int windowBg = HudRenderUtil.setAlpha(theme().windowBg(), themeAlpha);
            fillTop = HudRenderUtil.mixRgb(windowBg, 0xFFFFFF, 0.060f);
            fillBottom = HudRenderUtil.mixRgb(windowBg, 0x000000, 0.105f);
            fillHotspot = HudRenderUtil.mixColor(fillTop, theme().accentSoft(), 0.12f);
            stroke = theme().windowStroke();
            strokeSoft = theme().strokeSoft();
            accent = theme().accent();
            accentSoft = theme().accentSoft();
            accentGlow = HudRenderUtil.setAlpha(theme().accent(), 255);
            text = theme().textPrimary();
            muted = theme().textMuted();
            rowFill = HudRenderUtil.setAlpha(theme().surface(), 122);
            rowAltFill = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(theme().surface(), 0xFFFFFF, 0.026f), 100);
            rowHighlight = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(theme().surface(), 0xFFFFFF, 0.070f), 130);
            pingChip = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(theme().surface(), 0x000000, 0.05f), 128);
            divider = HudRenderUtil.setAlpha(theme().strokeSoft(), 96);
            return;
        }
        int baseBg = bgArgb;
        fillTop = HudRenderUtil.mixRgb(baseBg, 0xFFFFFF, 0.060f);
        fillBottom = HudRenderUtil.mixRgb(baseBg, 0x000000, 0.105f);
        accent = accentArgb | 0xFF000000;
        accentSoft = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, baseBg, 0.35f), 0.55f);
        accentGlow = HudRenderUtil.setAlpha(accent, 255);
        fillHotspot = HudRenderUtil.mixColor(fillTop, accentSoft, 0.12f);
        text = textArgb | 0xFF000000;
        muted = mutedArgb | 0xFF000000;
        stroke = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFFFFFFFF, 0.18f), 0.62f);
        strokeSoft = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(muted, baseBg, 0.42f), 0.42f);
        rowFill = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(baseBg, 0xFFFFFF, 0.045f), 122);
        rowAltFill = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(baseBg, 0xFFFFFF, 0.024f), 100);
        rowHighlight = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(baseBg, 0xFFFFFF, 0.075f), 130);
        pingChip = HudRenderUtil.setAlpha(HudRenderUtil.mixRgb(baseBg, 0x000000, 0.06f), 128);
        divider = HudRenderUtil.setAlpha(muted, 72);
    }
}
