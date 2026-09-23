/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

public enum ModulesMenuStyle {
    ;
    public static final int MODULE_LIST_ON = 0xFF5DE878;
    public static final int MODULE_LIST_OFF = 0xFFFF5E66;

    private static int text = 0xFFFFFFFF;
    private static int textMuted = 0xBFFFFFFF;
    private static int textFaint = 0x80FFFFFF;
    private static int panelBgGlassDark = 0x660C0C0C;
    private static int panelStroke = 0x2EFFFFFF;
    private static int split = 0x0DFFFFFF;
    private static int rowHover = 0x12FFFFFF;
    private static int rowHoverGlow = 0x4DFFFFFF;
    private static int shadow = 0x42000000;

    public static void syncTheme() {
        Themes.Theme theme = Theme.theme();
        if (theme == null) return;

        int accent = forceAlpha(theme.accent(), 255);
        int accentSoft = forceAlpha(theme.accentSoft(), 255);
        int surface = forceAlpha(theme.surface(), 255);
        int surfaceHover = forceAlpha(theme.surfaceHover(), 255);
        int stroke = forceAlpha(theme.strokeSoft(), 255);

        text = forceAlpha(theme.textPrimary(), 255);
        textMuted = withAlpha(theme.textMuted(), 210);
        textFaint = withAlpha(theme.textMuted(), 135);

        panelBgGlassDark = withAlpha(mix(surface, accent, 0.070f), 116);
        panelStroke = withAlpha(mix(stroke, accent, 0.155f), 76);
        split = withAlpha(mix(stroke, accent, 0.120f), 22);
        rowHover = withAlpha(mix(surfaceHover, accent, 0.125f), 34);
        rowHoverGlow = withAlpha(mix(accentSoft, accent, 0.42f), 92);
        shadow = withAlpha(0xFF000000, 72);
    }

    public static int text() {
        return text;
    }

    public static int textMuted() {
        return textMuted;
    }

    public static int textFaint() {
        return textFaint;
    }

    public static int panelBgGlassDark() {
        return panelBgGlassDark;
    }

    public static int panelStroke() {
        return panelStroke;
    }

    public static int split() {
        return split;
    }

    public static int rowHover() {
        return rowHover;
    }

    public static int rowHoverGlow(float alpha) {
        return withAlpha(rowHoverGlow, alpha);
    }

    public static int shadow() {
        return shadow;
    }

    public static int scrollTrackA(float alpha) {
        return withAlpha(SettingsGuiPalette.current().moduleScrollTrackA(), alpha);
    }

    public static int scrollTrackB(float alpha) {
        return withAlpha(SettingsGuiPalette.current().moduleScrollTrackB(), alpha);
    }

    public static int scrollHandleA(float alpha) {
        return withAlpha(SettingsGuiPalette.current().moduleScrollHandleA(), alpha);
    }

    public static int scrollHandleB(float alpha) {
        return withAlpha(SettingsGuiPalette.current().moduleScrollHandleB(), alpha);
    }

    public static int withAlpha(int color, float alpha) {
        float a = AnimationUtility.clamp(alpha, 0.0f, 1.0f);
        int ca = (color >>> 24) & 0xFF;
        int na = Math.round(ca * a);
        return (color & 0x00FFFFFF) | ((na & 0xFF) << 24);
    }

    public static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int forceAlpha(int color, int alpha) {
        return withAlpha(color, alpha);
    }

    public static int mix(int from, int to, float t) {
        t = AnimationUtility.clamp(t, 0.0f, 1.0f);

        int a = Math.round(((from >>> 24) & 0xFF) * (1.0f - t) + ((to >>> 24) & 0xFF) * t);
        int r = Math.round(((from >>> 16) & 0xFF) * (1.0f - t) + ((to >>> 16) & 0xFF) * t);
        int g = Math.round(((from >>> 8) & 0xFF) * (1.0f - t) + ((to >>> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1.0f - t) + (to & 0xFF) * t);

        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
