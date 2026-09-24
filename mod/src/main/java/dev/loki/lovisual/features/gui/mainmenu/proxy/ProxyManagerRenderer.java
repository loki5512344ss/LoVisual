package dev.loki.lovisual.features.gui.mainmenu.proxy;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.proxy.model.ProxyType;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.features.gui.mainmenu.proxy.ProxyManagerController.*;

/**
 * Animation state and rendering for the proxy manager screen.
 */
final class ProxyManagerRenderer {
    private final float[] fieldHover = new float[FIELD_COUNT];
    private final float[] fieldFocus = new float[FIELD_COUNT];
    private float openAnim;
    private float enableHover, enablePress;
    private float socks4Hover, socks5Hover, socks4Press, socks5Press;
    private float saveHover, savePress, clearHover, clearPress, backHover, backPress;
    private TextRenderer titleRenderer;
    private TextRenderer bodyRenderer;
    void init() {
        titleRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        bodyRenderer = Fonts.renderer("InterMedium", FontInfo.Type.Regular, titleRenderer);
    }
    void updateAnimations(ProxyManagerController c, float mouseX, float mouseY) {
        float dt = AnimationUtility.deltaTime();
        openAnim = animate(openAnim, 1f, dt, 7f);
        float[] o = c.panelOrigin();
        float innerX = o[0] + 8f * SCALE;
        float innerW = PANEL_W - 16f * SCALE;
        float rowY = o[1] + 28f * SCALE;
        enableHover = animate(enableHover, inside(mouseX, mouseY, innerX, rowY, 66f * SCALE, 13f * SCALE) ? 1f : 0f, dt, 11f);
        enablePress = animate(enablePress, 0f, dt, 8f);
        float typeX = innerX + 72f * SCALE;
        float typeItemW = (innerW - 72f * SCALE - 2f * SCALE) * 0.5f;
        socks5Hover = animate(socks5Hover, inside(mouseX, mouseY, typeX, rowY, typeItemW, 13f * SCALE) ? 1f : 0f, dt, 11f);
        socks4Hover = animate(socks4Hover, inside(mouseX, mouseY, typeX + typeItemW + 2f * SCALE, rowY, typeItemW, 13f * SCALE) ? 1f : 0f, dt, 11f);
        socks5Press = animate(socks5Press, 0f, dt, 8f);
        socks4Press = animate(socks4Press, 0f, dt, 8f);
        float fieldY = o[1] + 53f * SCALE;
        for (Field field : Field.values()) {
            float y = fieldY + field.index * 27f * SCALE + 9f * SCALE;
            fieldHover[field.index] = animate(fieldHover[field.index], inside(mouseX, mouseY, innerX, y, innerW, FIELD_H) ? 1f : 0f, dt, 11f);
            fieldFocus[field.index] = animate(fieldFocus[field.index], c.focusedField() == field ? 1f : 0f, dt, 10f);
        }
        float buttonY = o[1] + PANEL_H - 26f * SCALE;
        float third = (innerW - GAP * 2f) / 3f;
        saveHover = animate(saveHover, inside(mouseX, mouseY, innerX, buttonY, third, BUTTON_H) ? 1f : 0f, dt, 11f);
        clearHover = animate(clearHover, inside(mouseX, mouseY, innerX + third + GAP, buttonY, third, BUTTON_H) ? 1f : 0f, dt, 11f);
        backHover = animate(backHover, inside(mouseX, mouseY, innerX + (third + GAP) * 2f, buttonY, third, BUTTON_H) ? 1f : 0f, dt, 11f);
        savePress = animate(savePress, 0f, dt, 8f);
        clearPress = animate(clearPress, 0f, dt, 8f);
        backPress = animate(backPress, 0f, dt, 8f);
    }
    void renderPanel(ProxyManagerController c, float mouseX, float mouseY) {
        float[] o = c.panelOrigin();
        float bX = o[0], bY = o[1];
        float open = AnimationUtility.easeOutCubic(openAnim);
        PanelColors col = colors(open);
        Renderer2D.COLOR.blurRect(bX, bY, PANEL_W, PANEL_H, PANEL_R, 15f, 1.0f, 0.30f, 0xFFFFFF);
        Renderer2D.COLOR.roundedRectSoftShadow(bX, bY, PANEL_W, PANEL_H, PANEL_R, 12f * SCALE, 0.022f + open * 0.012f, col.shadow);
        Renderer2D.COLOR.roundedRectGradientQuad(bX, bY, PANEL_W, PANEL_H, PANEL_R, 1f, col.bgTopLeft, col.bgTopRight, col.bgBottomRight, col.bgBottomLeft);
        Renderer2D.COLOR.roundedRectMaskedQuad(bX, bY, PANEL_W, 22f * SCALE, bX, bY, PANEL_W, PANEL_H, PANEL_R, 1f,
                col.headerTopLeft, col.headerTopRight, col.headerBottomRight, col.headerBottomLeft);
        Renderer2D.COLOR.roundedRectStrokeGradient(bX, bY, PANEL_W, PANEL_H, PANEL_R, 1f, 1f, HudRenderUtil.mixColor(col.stroke, 0xFFFFFFFF, 0.04f), col.stroke, 90f);
        drawCentered(titleRenderer, tr("screen.lovisual.proxy_manager.header"), bX + PANEL_W * 0.5f, bY + 7f * SCALE, TITLE_FONT, col.title);
        float innerX = bX + 8f * SCALE;
        float innerW = PANEL_W - 16f * SCALE;
        float rowY = bY + 28f * SCALE;
        renderChoice(innerX, rowY, 66f * SCALE, 13f * SCALE, tr("screen.lovisual.proxy_manager.enabled"), c.enabled(), enableHover, enablePress, col);
        renderTypeToggle(c, innerX + 72f * SCALE, rowY, innerW - 72f * SCALE, 13f * SCALE, col);
        float fieldY = bY + 53f * SCALE;
        renderField(c, Field.ADDRESS, tr("screen.lovisual.proxy_manager.address"), tr("screen.lovisual.proxy_manager.placeholder.address"), innerX, fieldY, innerW, col);
        renderField(c, Field.USERNAME, tr("screen.lovisual.proxy_manager.username"), tr("screen.lovisual.proxy_manager.placeholder.optional"), innerX, fieldY + 27f * SCALE, innerW, col);
        renderField(c, Field.PASSWORD, tr("screen.lovisual.proxy_manager.password"), tr("screen.lovisual.proxy_manager.placeholder.optional"), innerX, fieldY + 54f * SCALE, innerW, col);
        float buttonY = bY + PANEL_H - 26f * SCALE;
        float third = (innerW - GAP * 2f) / 3f;
        renderAction(innerX, buttonY, third, BUTTON_H, tr("screen.lovisual.proxy_manager.save"), false, saveHover, savePress, col);
        renderAction(innerX + third + GAP, buttonY, third, BUTTON_H, tr("screen.lovisual.proxy_manager.clear"), true, clearHover, clearPress, col);
        renderAction(innerX + (third + GAP) * 2f, buttonY, third, BUTTON_H, tr("screen.lovisual.proxy_manager.back"), false, backHover, backPress, col);
        String info = c.status().isBlank() ? c.currentSummary() : c.status();
        draw(bodyRenderer, ellipsize(info, FIELD_FONT, innerW), innerX, bY + PANEL_H - 42f * SCALE, FIELD_FONT, col.muted);
    }
    private void renderTypeToggle(ProxyManagerController c, float x, float y, float w, float h, PanelColors col) {
        float gap = 2f * SCALE;
        float itemW = (w - gap) * 0.5f;
        renderChoice(x, y, itemW, h, "SOCKS5", c.type() == ProxyType.SOCKS5, socks5Hover, socks5Press, col);
        renderChoice(x + itemW + gap, y, itemW, h, "SOCKS4", c.type() == ProxyType.SOCKS4, socks4Hover, socks4Press, col);
    }
    private void renderField(ProxyManagerController c, Field field, String label, String placeholder, float x, float y, float w, PanelColors col) {
        int i = field.index;
        draw(bodyRenderer, label, x, y, LABEL_FONT, col.label);
        float fy = y + 9f * SCALE;
        boolean focused = c.focusedField() == field;
        float hoverAnim = AnimationUtility.easeOutCubic(fieldHover[i]);
        float focusAnim = AnimationUtility.easeOutCubic(fieldFocus[i]);
        int fillTop = HudRenderUtil.mixColor(withAlpha(col.surface, 188), withAlpha(col.surfaceHover, 208), hoverAnim * 0.45f + focusAnim * 0.16f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(col.surface, 176), withAlpha(0xFF0D1118, 196), 0.18f);
        int stroke = focused ? HudRenderUtil.mixColor(col.strokeSoft, col.accentSoft, 0.55f) : HudRenderUtil.mixColor(col.stroke, col.strokeSoft, hoverAnim * 0.72f);
        Renderer2D.COLOR.roundedRectGradientQuad(x, fy, w, FIELD_H, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, fy, w, FIELD_H, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        if (focusAnim > 0.01f) {
            Renderer2D.COLOR.roundedRectGradientQuad(x + 1.2f * SCALE, fy + FIELD_H - 1.6f * SCALE, w - 2.4f * SCALE, 1.1f * SCALE, 0.6f * SCALE, 1f,
                    withAlpha(col.accentSoft, Math.round(48f * focusAnim)), withAlpha(col.accent, Math.round(28f * focusAnim)),
                    withAlpha(col.accent, Math.round(28f * focusAnim)), withAlpha(col.accentSoft, Math.round(48f * focusAnim)));
        }
        String value = c.values()[i];
        String display = field == Field.PASSWORD && !value.isEmpty() ? "*".repeat(value.length()) : value;
        if (display.isEmpty() && !focused) display = placeholder;
        if (focused && AnimationUtility.blink(500L)) display += "|";
        int color = value.isEmpty() && !focused ? col.mutedLabel : col.title;
        draw(bodyRenderer, ellipsize(display, FIELD_FONT, w - 8f * SCALE), x + 4f * SCALE, fy + 4.3f * SCALE, FIELD_FONT, color);
    }
    private void renderChoice(float x, float y, float w, float h, String label, boolean selected, float hover, float press, PanelColors col) {
        float hoverAnim = AnimationUtility.easeOutCubic(hover);
        float pressAnim = AnimationUtility.easeOutCubic(press);
        float active = selected ? 1f : 0f;
        int fillTop = HudRenderUtil.mixColor(withAlpha(col.surface, 170), withAlpha(col.surfaceHover, 210), hoverAnim * 0.42f + active * 0.38f + pressAnim * 0.10f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(col.surface, 158), withAlpha(0x1A2418, 210), active * 0.24f + hoverAnim * 0.16f);
        int stroke = HudRenderUtil.mixColor(col.stroke, selected ? col.accentSoft : col.strokeSoft, active * 0.70f + hoverAnim * 0.30f + pressAnim * 0.10f);
        int text = selected ? HudRenderUtil.mixColor(col.title, col.accent, 0.28f) : HudRenderUtil.mixColor(col.muted, col.title, hoverAnim * 0.35f);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        drawCentered(bodyRenderer, label, x + w * 0.5f, y + 4.1f * SCALE - pressAnim * 0.35f * SCALE, BUTTON_FONT, text);
    }
    private void renderAction(float x, float y, float w, float h, String label, boolean destructive, float hover, float press, PanelColors col) {
        float hoverAnim = AnimationUtility.easeOutCubic(hover);
        float pressAnim = AnimationUtility.easeOutCubic(press);
        int fillTop, fillBottom, stroke, text;
        if (destructive) {
            fillTop = HudRenderUtil.mixColor(withAlpha(0x1A1416, 170), withAlpha(0x4B2224, 212), 0.25f + hoverAnim * 0.45f + pressAnim * 0.12f);
            fillBottom = HudRenderUtil.mixColor(withAlpha(0x120E10, 165), withAlpha(0x261012, 220), 0.20f + hoverAnim * 0.40f + pressAnim * 0.10f);
            stroke = HudRenderUtil.mixColor(withAlpha(0x352A2A, 215), withAlpha(0x8C5054, 235), 0.22f + hoverAnim * 0.58f + pressAnim * 0.12f);
            text = HudRenderUtil.mixColor(withAlpha(0xD0A0A0, 255), withAlpha(0xFFB2B2, 255), 0.24f + hoverAnim * 0.56f + pressAnim * 0.08f);
        } else {
            fillTop = HudRenderUtil.mixColor(withAlpha(col.surface, 176), withAlpha(0x223746, 212), 0.20f + hoverAnim * 0.44f + pressAnim * 0.10f);
            fillBottom = HudRenderUtil.mixColor(withAlpha(col.surface, 166), withAlpha(0x13292F, 220), 0.16f + hoverAnim * 0.40f + pressAnim * 0.08f);
            stroke = HudRenderUtil.mixColor(col.stroke, withAlpha(0x466A76, 228), 0.18f + hoverAnim * 0.50f + pressAnim * 0.10f);
            text = HudRenderUtil.mixColor(withAlpha(0xD0D8E4, 255), withAlpha(0xEEF8FF, 255), 0.18f + hoverAnim * 0.38f + pressAnim * 0.08f);
        }
        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, h, 3f * SCALE, 7f * SCALE, 0.018f + hoverAnim * 0.015f, col.shadow);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        drawCentered(bodyRenderer, label, x + w * 0.5f, y + 5f * SCALE - (hoverAnim * 0.65f + pressAnim * 0.55f) * SCALE, BUTTON_FONT, text);
    }
    private PanelColors colors(float a) {
        int ta = Math.round(a * 255f), la = Math.round(a * 155f);
        int bg = Math.round(a * 120f), hd = Math.round(a * 150f), st = Math.round(a * 100f);
        return new PanelColors(
                withAlpha(0x0D0F14, bg), withAlpha(0x101218, bg), withAlpha(0x0D0F14, bg), withAlpha(0x08090C, bg),
                withAlpha(0x14171F, hd), withAlpha(0x181B24, hd), withAlpha(0x14171F, hd), withAlpha(0x10131A, hd),
                withAlpha(0x252A36, st), withAlpha(0x3A4A5A, hd), withAlpha(0x060810, Math.round(a * 80f)),
                withAlpha(0xFFFFFF, ta), withAlpha(0xFFFFFF, la), withAlpha(0x808890, ta), withAlpha(0x606878, la),
                withAlpha(0x1A1D24, Math.round(a * 160f)), withAlpha(0x1A1F28, Math.round(a * 200f)),
                withAlpha(0xFFD700, ta), withAlpha(0x4A3A10, Math.round(a * 180f)));
    }
    private void draw(TextRenderer r, String text, float x, float y, float size, int argb) {
        if (r == null || text == null || text.isEmpty()) return;
        r.setAlpha(1.0); r.begin(size, false, false);
        r.render(text, x, y, new RenderColor(argb), false); r.end();
    }
    private void drawCentered(TextRenderer r, String text, float cx, float y, float size, int argb) {
        draw(r, text, cx - width(r, text, size) * 0.5f, y, size, argb);
    }
    private float width(TextRenderer r, String text, float size) {
        if (r == null || text == null) return 0f;
        r.begin(size, true, false);
        float w = (float) r.getWidth(text, false); r.end(); return w;
    }
    private String ellipsize(String text, float size, float maxW) {
        if (text == null || text.isEmpty()) return "";
        if (width(bodyRenderer, text, size) <= maxW) return text;
        String out = text;
        while (out.length() > 3 && width(bodyRenderer, out + "...", size) > maxW) out = out.substring(0, out.length() - 1);
        return out + "...";
    }
    private static float animate(float c, float t, float dt, float sp) { return AnimationUtility.snap(AnimationUtility.approach(c, t, dt, sp), t, 0.001f); }
    private static int withAlpha(int rgb, int alpha) { return (rgb & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24); }
    private record PanelColors(
            int bgTopLeft, int bgTopRight, int bgBottomRight, int bgBottomLeft,
            int headerTopLeft, int headerTopRight, int headerBottomRight, int headerBottomLeft,
            int stroke, int strokeSoft, int shadow, int title, int label,
            int muted, int mutedLabel, int surface, int surfaceHover, int accent, int accentSoft) {}
}
