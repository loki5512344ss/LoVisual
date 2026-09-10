/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.settings.skin;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;

public enum UnifiedSettingsSkin {
    ;
    static final float ROW_GAP = 3f;

    public static int TEXT_PRIMARY = 0xFFE8ECF7;
    public static int TEXT_MUTED = 0xFFADB5C6;
    public static int TEXT_FAINT = 0x88ADB5C6;

    public static int SURFACE = 0x301A1F27;
    public static int SURFACE_HOVER = 0x40222932;
    public static int SURFACE_SOFT = 0x28161B23;
    static int SURFACE_GRADIENT_START = SURFACE_SOFT;
    static int SURFACE_GRADIENT_END = SURFACE_HOVER;
    static float SURFACE_GRADIENT_ANGLE = 90f;
    public static boolean SURFACE_GRADIENT_ENABLED = false;
    static int CARD_GRADIENT_START = SURFACE_SOFT;
    static int CARD_GRADIENT_END = SURFACE_HOVER;
    static float CARD_GRADIENT_ANGLE = 90f;
    static boolean CARD_GRADIENT_ENABLED = false;
    static int STROKE_GRADIENT_START = TEXT_FAINT;
    static int STROKE_GRADIENT_END = TEXT_FAINT;
    static float STROKE_GRADIENT_ANGLE = 90f;
    public static boolean STROKE_GRADIENT_ENABLED = false;

    public static int ACCENT = 0xFF9747FF;
    public static int ACCENT_SOFT = 0x629747FF;
    static int ACCENT_GRADIENT_START = ACCENT;
    static int ACCENT_GRADIENT_END = ACCENT_SOFT;
    public static float ACCENT_GRADIENT_ANGLE = 45f;
    static int CHECK_COLOR = 0xFFEFF3FF;

    public static void syncTheme() {
        Themes.Theme theme = Theme.theme();
        if (theme == null) return;

        int accent = forceAlpha(theme.accent(), 255);
        int accentSoft = forceAlpha(theme.accentSoft(), 255);
        int surface = forceAlpha(theme.surface(), 255);
        int hover = forceAlpha(theme.surfaceHover(), 255);

        TEXT_PRIMARY = forceAlpha(theme.textPrimary(), 255);
        TEXT_MUTED = withAlpha(theme.textMuted(), 210);
        TEXT_FAINT = withAlpha(theme.textMuted(), 135);

        ACCENT = accent;
        ACCENT_SOFT = withAlpha(accentSoft, 135);
        CHECK_COLOR = ClickGuiRenderer.mixColor(TEXT_PRIMARY, accent, 0.10f);

        SURFACE = withAlpha(ClickGuiRenderer.mixColor(surface, accent, 0.055f), 82);
        SURFACE_HOVER = withAlpha(ClickGuiRenderer.mixColor(hover, accent, 0.095f), 104);
        SURFACE_SOFT = withAlpha(ClickGuiRenderer.mixColor(surface, accent, 0.040f), 56);

        Themes.ThemeEntry entry = Theme.currentEntry();
        Themes.GradientSpec surfaceGradient = entry == null ? null : entry.surfaceGradient();
        Themes.GradientSpec cardGradient = entry == null ? null : entry.cardGradient();
        Themes.GradientSpec strokeGradient = entry == null ? null : entry.strokeGradient();

        SURFACE_GRADIENT_ENABLED = surfaceGradient != null && surfaceGradient.enabled();
        SURFACE_GRADIENT_START = SURFACE_GRADIENT_ENABLED
                ? withAlpha(ClickGuiRenderer.mixColor(forceAlpha(surfaceGradient.start(), 255), accent, 0.045f), 62)
                : SURFACE_SOFT;
        SURFACE_GRADIENT_END = SURFACE_GRADIENT_ENABLED
                ? withAlpha(ClickGuiRenderer.mixColor(forceAlpha(surfaceGradient.end(), 255), accent, 0.075f), 98)
                : SURFACE_HOVER;
        SURFACE_GRADIENT_ANGLE = surfaceGradient == null ? 90f : surfaceGradient.angleDeg();

        CARD_GRADIENT_ENABLED = cardGradient != null && cardGradient.enabled();
        CARD_GRADIENT_START = CARD_GRADIENT_ENABLED
                ? withAlpha(forceAlpha(cardGradient.start(), 255), 86)
                : SURFACE_SOFT;
        CARD_GRADIENT_END = CARD_GRADIENT_ENABLED
                ? withAlpha(forceAlpha(cardGradient.end(), 255), 118)
                : SURFACE_HOVER;
        CARD_GRADIENT_ANGLE = cardGradient == null ? 90f : cardGradient.angleDeg();

        STROKE_GRADIENT_ENABLED = strokeGradient != null && strokeGradient.enabled();
        STROKE_GRADIENT_START = STROKE_GRADIENT_ENABLED ? forceAlpha(strokeGradient.start(), 255) : TEXT_FAINT;
        STROKE_GRADIENT_END = STROKE_GRADIENT_ENABLED ? forceAlpha(strokeGradient.end(), 255) : TEXT_FAINT;
        STROKE_GRADIENT_ANGLE = strokeGradient == null ? 90f : strokeGradient.angleDeg();

        ACCENT_GRADIENT_START = CARD_GRADIENT_ENABLED ? forceAlpha(cardGradient.start(), 255) : ACCENT;
        ACCENT_GRADIENT_END = CARD_GRADIENT_ENABLED ? forceAlpha(cardGradient.end(), 255) : forceAlpha(accentSoft, 255);
        ACCENT_GRADIENT_ANGLE = CARD_GRADIENT_ENABLED ? cardGradient.angleDeg() : 45f;
    }

    static boolean modules() {
        return SettingRenderContext.current() == SettingRenderSurface.MODULES;
    }

    public static float scale() {
        syncTheme();
        return Math.max(0.25f, Math.min(4.0f, SettingRenderContext.scale()));
    }

    public static float metric(float settings, float modules) {
        return (modules() ? modules : settings) * scale();
    }

    public static TextRenderer fontRegular() {
        return Fonts.renderer("Onest", FontInfo.Type.Regular, ClickGuiRenderer.getInterRegular());
    }

    public static TextRenderer fontMedium() {
        return Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fontRegular());
    }

    public static TextRenderer fontSemibold() {
        return Fonts.renderer("OnestBold", FontInfo.Type.Regular, fontMedium());
    }

    public static TextRenderer fontLight() {
        return Fonts.renderer("OnestLight", FontInfo.Type.Regular, fontRegular());
    }

    public static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static int mix(int from, int to, float t) {
        t = clamp01(t);
        int a = Math.round(((from >>> 24) & 0xFF) * (1f - t) + ((to >>> 24) & 0xFF) * t);
        int r = Math.round(((from >>> 16) & 0xFF) * (1f - t) + ((to >>> 16) & 0xFF) * t);
        int g = Math.round(((from >>> 8) & 0xFF) * (1f - t) + ((to >>> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1f - t) + (to & 0xFF) * t);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int withAlpha(int color, float alpha01) {
        int base = (color >>> 24) & 0xFF;
        int a = Math.round(base * clamp01(alpha01));
        return (color & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    public static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static int forceAlpha(int color, int alpha) {
        return withAlpha(color, alpha);
    }

    public static String fit(TextRenderer font, String text, float size, float width) {
        return ClickGuiRenderer.fitText(font, text, size, Math.max(1f, width));
    }

    public static float textHeight(TextRenderer font, float size) {
        return ClickGuiRenderer.textHeight(font, size);
    }

    public static float textWidth(TextRenderer font, String text, float size) {
        return ClickGuiRenderer.textWidth(font, text == null ? "" : text, size);
    }

    static void matte(float x, float y, float w, float h, float radius, int color) {
        ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, color);
    }

    static int surfaceGradientStart(float alpha) {
        return withAlpha(SURFACE_GRADIENT_START, alpha);
    }

    static int surfaceGradientEnd(float alpha) {
        return withAlpha(SURFACE_GRADIENT_END, alpha);
    }

    static int cardGradientStart(float alpha) {
        return withAlpha(CARD_GRADIENT_START, alpha);
    }

    static int cardGradientEnd(float alpha) {
        return withAlpha(CARD_GRADIENT_END, alpha);
    }

    static int strokeGradientStart(float alpha) {
        return withAlpha(STROKE_GRADIENT_START, alpha);
    }

    static int strokeGradientEnd(float alpha) {
        return withAlpha(STROKE_GRADIENT_END, alpha);
    }

    public static int accentGradientStart(float alpha) {
        return withAlpha(ACCENT_GRADIENT_START, alpha);
    }

    public static int accentGradientEnd(float alpha) {
        return withAlpha(ACCENT_GRADIENT_END, alpha);
    }

    public static void drawSurface(float x, float y, float w, float h, float radius, float alpha) {
        if (SURFACE_GRADIENT_ENABLED) {
            ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, radius, surfaceGradientStart(alpha), surfaceGradientEnd(alpha), SURFACE_GRADIENT_ANGLE);
        } else {
            ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, withAlpha(SURFACE_SOFT, alpha));
        }
    }

    public static void drawCard(float x, float y, float w, float h, float radius, float alpha) {
        if (CARD_GRADIENT_ENABLED) {
            ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, radius, cardGradientStart(alpha), cardGradientEnd(alpha), CARD_GRADIENT_ANGLE);
        } else {
            ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, withAlpha(SURFACE_SOFT, alpha));
        }
    }

    public static void drawAccent(float x, float y, float w, float h, float radius, float alpha) {
        ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, radius, accentGradientStart(alpha), accentGradientEnd(alpha), ACCENT_GRADIENT_ANGLE);
    }

    public static void drawStroke(float x, float y, float w, float h, float radius, float thickness, float alpha) {
        if (STROKE_GRADIENT_ENABLED) {
            ClickGuiRenderer.drawRoundedRectStrokeGradient(x, y, w, h, radius, thickness, strokeGradientStart(alpha), strokeGradientEnd(alpha), STROKE_GRADIENT_ANGLE);
        } else {
            ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, radius, thickness, withAlpha(TEXT_FAINT, alpha));
        }
    }

    static void checkIcon(float x, float y, float size, float activeAnim, float hoverAnim) {
        float active = clamp01(activeAnim);
        float hover = clamp01(hoverAnim);
        float alpha = 0.12f + active * 0.88f;
        int tint = mix(CHECK_COLOR, 0xFFFFFFFF, hover * 0.35f);
        int argb = withAlpha(tint, alpha);
        Renderer2D.COLOR.svg("check", x, y, size, size, SvgRenderOptions.overrideColor(argb));
    }
}
