package dev.loki.lovisual.features.gui.clickgui.settings.skin;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.style.StyleSpec;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.material.SurfaceRenderer;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;

public enum ModulesSettingsSkin {
    ;
    static final float HEADER_H = 19f;

    static int TEXT_PRIMARY = 0xFFE8ECF7;
    static int TEXT_MUTED = 0xFFADB5C6;

    static int SURFACE = 0x301A1F27;
    static int SURFACE_HOVER = 0x40222932;
    static int SURFACE_SOFT = 0x28161B23;
    static int STROKE = 0x46FFFFFF;

    static int ACCENT = 0xFF9747FF;
    static int ACCENT_SOFT = 0x629747FF;
    static int CHECK_COLOR = 0xFFEFF3FF;

    static void syncTheme() {
        Themes.Theme theme = Theme.theme();
        if (theme == null) return;

        TEXT_PRIMARY = forceAlpha(theme.textPrimary(), 255);
        TEXT_MUTED = withAlpha(theme.textMuted(), 210);

        ACCENT = forceAlpha(theme.accent(), 255);
        ACCENT_SOFT = withAlpha(theme.accentSoft(), 135);
        CHECK_COLOR = ClickGuiRenderer.mixColor(TEXT_PRIMARY, ACCENT, 0.10f);

        int surface = forceAlpha(theme.surface(), 255);
        int hover = forceAlpha(theme.surfaceHover(), 255);
        int accentTint = forceAlpha(theme.accent(), 255);

        SURFACE = withAlpha(ClickGuiRenderer.mixColor(surface, accentTint, 0.075f), 86);
        SURFACE_HOVER = withAlpha(ClickGuiRenderer.mixColor(hover, accentTint, 0.13f), 112);
        SURFACE_SOFT = withAlpha(ClickGuiRenderer.mixColor(surface, accentTint, 0.055f), 62);
        STROKE = withAlpha(ClickGuiRenderer.mixColor(theme.strokeSoft(), accentTint, 0.16f), 118);
    }

    static float scale() {
        syncTheme();
        return Math.max(0.25f, Math.min(4.0f, SettingRenderContext.scale()));
    }

    static float s(float value) {
        return value * scale();
    }

    static TextRenderer fontRegular() {
        return Fonts.renderer("Onest", FontInfo.Type.Regular);
    }

    static TextRenderer fontMedium() {
        return Fonts.renderer("OnestMedium", FontInfo.Type.Regular);
    }

    static TextRenderer fontSemibold() {
        return Fonts.renderer("OnestBold", FontInfo.Type.Regular);
    }

    static TextRenderer fontLight() {
        return Fonts.renderer("OnestLight", FontInfo.Type.Regular);
    }

    static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    static int mix(int from, int to, float t) {
        t = clamp01(t);
        int a = (int) (((from >>> 24) & 0xFF) * (1f - t) + ((to >>> 24) & 0xFF) * t);
        int r = (int) (((from >>> 16) & 0xFF) * (1f - t) + ((to >>> 16) & 0xFF) * t);
        int g = (int) (((from >>> 8) & 0xFF) * (1f - t) + ((to >>> 8) & 0xFF) * t);
        int b = (int) (((from) & 0xFF) * (1f - t) + ((to) & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int withAlpha(int color, float alpha01) {
        int base = (color >>> 24) & 0xFF;
        int a = Math.round(base * clamp01(alpha01));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static int forceAlpha(int color, int alpha) {
        return withAlpha(color, alpha);
    }

    static void drawGlass(float x, float y, float w, float h, float radius, float alpha) {
        if (alpha <= 0.001f) return;
        float a = clamp01(alpha);
        float glassAlpha = a * 0.58f;
        StyleSpec spec = new StyleSpec(
                SurfaceStyle.GLASS,
                Math.max(3.0f * scale(), radius * 0.65f),
                -50.0f,
                0.95f,
                0.58f,
                0.10f,
                0.055f * a,
                0.0f,
                Theme.theme().accent(),
                0.6f,
                0.0f);
        SurfaceRenderer.plateSpec(Renderer2D.COLOR,
                x, y, w, h, radius, radius, radius, radius,
                spec, 1.0f, Theme.theme().accent(), glassAlpha, glassAlpha,
                SurfaceStyle.AUTO);
    }

    static String fit(TextRenderer font, String text, float size, float width) {
        return ClickGuiRenderer.fitText(font, text, size, Math.max(1f, width));
    }

    static void drawCheckIcon(float x, float y, float size, float activeAnim, float hoverAnim) {
        syncTheme();
        float active = clamp01(activeAnim);
        float hover = clamp01(hoverAnim);
        float alpha = 0.12f + active * 0.88f;
        int tint = mix(CHECK_COLOR, 0xFFFFFFFF, hover * 0.35f);
        int argb = withAlpha(tint, alpha);
        Renderer2D.COLOR.svg("check", x, y, size, size, SvgRenderOptions.overrideColor(argb));
    }
}
