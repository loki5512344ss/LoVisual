package dev.loki.lovisual.features.gui.clickgui.render;

import dev.loki.lovisual.config.style.ClickGuiLookConfig;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.style.StyleSpec;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;
import dev.loki.lovisual.render.helpers.material.SurfaceRenderer;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;

public final class ClickGuiDrawHelpers {

    private ClickGuiDrawHelpers() {
    }
    public static int mixColor(int from, int to, float t) {

        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) * (1 - t) + ((to >>> 24) & 0xFF) * t);
        int r = (int) (((from >>> 16) & 0xFF) * (1 - t) + ((to >>> 16) & 0xFF) * t);
        int g = (int) (((from >>> 8) & 0xFF) * (1 - t) + ((to >>> 8) & 0xFF) * t);
        int b = (int) (((from) & 0xFF) * (1 - t) + ((to) & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static int scaleAlpha(int color, float factor) {

        int a = (color >>> 24) & 0xFF;
        int na = (int) (a * Math.max(0f, Math.min(1f, factor)));
        return (color & 0x00FFFFFF) | (na << 24);
    }
    static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }
    static int applyAlpha(int argb) {
        if (ClickGuiRenderer.renderAlphaMultiplier >= 0.999f) return argb;
        int a = (argb >>> 24) & 0xFF;
        int na = Math.round(a * ClickGuiRenderer.renderAlphaMultiplier);
        return (argb & 0x00FFFFFF) | ((na & 0xFF) << 24);
    }
    static float setRenderAlphaMultiplier(float alpha) {
        float previous = ClickGuiRenderer.renderAlphaMultiplier;
        ClickGuiRenderer.renderAlphaMultiplier = Math.max(0.0f, Math.min(1.0f, alpha));
        return previous;
    }
    static float getRenderAlphaMultiplier() {
        return ClickGuiRenderer.renderAlphaMultiplier;
    }
    static void restoreRenderAlphaMultiplier(float previous) {
        ClickGuiRenderer.renderAlphaMultiplier = Math.max(0.0f, Math.min(1.0f, previous));
    }
    static void drawRoundedRect(float x, float y, float w, float h, float radius, int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRect(x, y, w, h, radius, 1.1f, argb);
    }
    static void drawRoundedRectStroke(float x, float y, float w, float h, float radius, float thickness, int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRectStroke(x, y, w, h, radius, 1.1f, thickness, argb);
    }
    public static void drawRoundedRectGlow(float x, float y, float w, float h,

                                    float radius, float glow, int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRectGlow(x, y, w, h, radius, 1.1f, glow, argb);
    }
    public static void drawRoundedRectShadow(float x, float y, float w, float h,

                                      float radius, float softness, float spread, int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRectShadow(x, y, w, h, radius, softness, spread, argb);
    }
    public static void drawRoundedRectGradient(float x, float y, float w, float h, float radius,

                                        int startArgb, int endArgb, float angleDeg) {
        if (ClickGuiRenderer.renderer == null) return;
        startArgb = applyAlpha(startArgb);
        endArgb = applyAlpha(endArgb);
        if (((startArgb >>> 24) & 0xFF) <= 0 && ((endArgb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRectGradient(x, y, w, h, radius, 1.1f, startArgb, endArgb, angleDeg);
    }
    public static void drawRoundedRectStrokeGradient(float x, float y, float w, float h, float radius, float thickness,

                                              int startArgb, int endArgb, float angleDeg) {
        if (ClickGuiRenderer.renderer == null) return;
        startArgb = applyAlpha(startArgb);
        endArgb = applyAlpha(endArgb);
        if (((startArgb >>> 24) & 0xFF) <= 0 && ((endArgb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRectStrokeGradient(x, y, w, h, radius, 1.1f, thickness, startArgb, endArgb, angleDeg);
    }
    static void drawRoundedRectCorners(float x, float y, float w, float h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.roundedRectCorners(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 1.1f, argb);
    }
    public static void drawRect(float x, float y, float w, float h, int argb) {

        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.quad(x, y, w, h, argb);
    }
    static void flushRenderer() {
        if (ClickGuiRenderer.renderer == null) return;
        if (Renderer2D.isBatching()) {
            Renderer2D.flushBatch();
        } else {
            ClickGuiRenderer.renderer.begin();
        }
    }
    static void drawGradientRect(float x, float y, float w, float h,
                                 int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        if (ClickGuiRenderer.renderer == null) return;
        cTopLeft = applyAlpha(cTopLeft);
        cTopRight = applyAlpha(cTopRight);
        cBottomRight = applyAlpha(cBottomRight);
        cBottomLeft = applyAlpha(cBottomLeft);
        ClickGuiRenderer.renderer.quadGradient(x, y, w, h, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }
    static void drawCircle(float cx, float cy, float r, int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.circle(cx, cy, r, argb);
    }
    static void drawLine(float x1, float y1, float x2, float y2, int argb) {
        if (ClickGuiRenderer.renderer == null) return;
        argb = applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        ClickGuiRenderer.renderer.line(x1, y1, x2, y2, argb);
    }
    static void drawBlur(float x, float y, float w, float h, float radius, int argb, float alpha) {
        alpha *= ClickGuiRenderer.renderAlphaMultiplier;
        alpha = AnimationUtility.clamp(alpha * clickGuiBlurDensityScale(), 0.0f, 1.0f);
        if (alpha <= 0.01f) return;
        float quality = clickGuiBlurQuality();
        float brightness = 1.0f;
        Renderer2D.COLOR.blurRect(x, y, w, h, radius, quality, brightness, alpha, 0xFFFFFF);
    }
    static void drawGlass(float x, float y, float w, float h, float radius, float alpha) {
        if (ClickGuiRenderer.renderer == null) return;
        alpha *= ClickGuiRenderer.renderAlphaMultiplier;
        if (alpha <= 0.01f) return;

        float pad = 5f;
        float gx = x - pad;
        float gy = y - pad;
        float gw = w + pad * 2f;
        float gh = h + pad * 2f;
        float gr = Math.max(0f, radius + pad);
        float blurAlpha = AnimationUtility.clamp(alpha * clickGuiGlassBlurScale(), 0.0f, 1.0f);
        float scale = Math.max(0.75f, gr / 10.0f);
        StyleSpec spec = new StyleSpec(
                SurfaceStyle.GLASS,
                15.0f,
                -18.0f,
                1.0f,
                0.78f,
                0.52f,
                0.190f * alpha,
                0.0f,
                0xFFFFFFFF,
                0.6f,
                clickGuiBlurQuality());
        SurfaceRenderer.plateSpec(ClickGuiRenderer.renderer,
                gx, gy, gw, gh, gr, gr, gr, gr,
                spec, scale, 0xFFFFFFFF, alpha, blurAlpha, SurfaceStyle.AUTO);
    }
    static float clickGuiBlurQuality() {
        return ClickGuiLookConfig.get().blurQuality();
    }
    static float clickGuiBlurDensityScale() {
        return ClickGuiLookConfig.get().blurDensity();
    }
    static float clickGuiGlassBlurScale() {
        return ClickGuiLookConfig.get().glassBlur();
    }
    static int getColorClipboard() {
        return ClickGuiRenderer.colorClipboard;
    }
    static void setColorClipboard(int argb) {
        ClickGuiRenderer.colorClipboard = argb;
    }
}
