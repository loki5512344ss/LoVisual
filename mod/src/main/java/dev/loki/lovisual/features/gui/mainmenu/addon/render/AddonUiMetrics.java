package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.Set;

final class AddonUiMetrics {
    static final float SCALE = 2.28f;
    static final float TEXT_SCALE = 1.30f;
    static final float LEFT_W = 112f * SCALE;
    static final float LEFT_TOP_H = 137f * SCALE;
    static final float LEFT_BOTTOM_H = 58f * SCALE;
    static final float RIGHT_W = 300f * SCALE;
    static final float RIGHT_H = 165f * SCALE;
    static final float GAP = 5f * SCALE;
    static final float PANEL_R = 6f * SCALE;
    static final float PANEL_SHADOW = 12f * SCALE;
    static final float HEADER_H = 22f * SCALE;
    static final float LEFT_CARD_R = 4f * SCALE;
    static final float CARD_MARKER_W = 2.2f * SCALE;
    static final float MARQUEE_SPEED = 24f * SCALE;
    static final float MARQUEE_GAP = 18f * SCALE;
    static final float MARQUEE_PAUSE = 0.55f;
    static final float TITLE_FONT = 0.84f * TEXT_SCALE;
    static final float BODY_FONT = 0.60f * TEXT_SCALE;
    static final float SMALL_FONT = 0.49f * TEXT_SCALE;
    static final float METRIC_FONT = 0.56f * TEXT_SCALE;
    static final float DESCRIPTION_FONT = 0.53f * TEXT_SCALE;
    static final float SELECTED_DESCRIPTION_FONT = 0.54f * TEXT_SCALE;
    static final float ADDON_NAME_FONT = 0.66f * TEXT_SCALE;
    static final float SELECTED_NAME_FONT = 0.70f * TEXT_SCALE;
    static final float BUTTON_FONT = 0.52f * TEXT_SCALE;
    static final float BUTTON_H = 13f * SCALE;
    static final float METRIC_H = 13f * SCALE;
    static final float METRIC_GAP = 2.5f * SCALE;
    static final float CARD_H = 40f * SCALE;
    static final float CARD_GAP = 5f * SCALE;

    private AddonUiMetrics() {
    }

    static float animate(float current, float target, float dt, float speed) {
        float next = AnimationUtility.approach(current, target, dt, speed);
        return AnimationUtility.snap(next, target, 0.001f);
    }

    static void prune(Map<String, Float> map, Set<String> keys) {
        map.keySet().removeIf(key -> !keys.contains(key));
    }

    static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
    }

    static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    static void draw(TextRenderer renderer, String text, float x, float y, float size, int argb) {
        if (renderer == null || text == null || text.isEmpty()) return;
        renderer.setAlpha(1.0);
        renderer.begin(size, false, false);
        renderer.render(text, x, y, new RenderColor(argb), false);
        renderer.end();
    }

    static void drawCentered(TextRenderer renderer, String text, float centerX, float y, float size, int argb) {
        draw(renderer, text, centerX - width(renderer, text, size) * 0.5f, y, size, argb);
    }

    static float width(TextRenderer renderer, String text, float size) {
        if (renderer == null || text == null) return 0f;
        renderer.begin(size, true, false);
        float w = (float) renderer.getWidth(text, false);
        renderer.end();
        return w;
    }

    static String ellipsize(TextRenderer renderer, String text, float size, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (width(renderer, text, size) <= maxWidth) return text;
        String out = text;
        while (out.length() > 3 && width(renderer, out + "...", size) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }

    static SvgRenderOptions svgGradient(PanelColors c) {
        return SvgRenderOptions.linearGradient(c.iconTop, c.iconBottom, 92f)
                .withTextureCache(false)
                .withCurveFlatness(0.18f);
    }

    static void drawSvgGradient(String svg, float x, float y, float w, float h, PanelColors c) {
        Renderer2D.COLOR.svg(svg, x + 0.38f * SCALE, y + 0.42f * SCALE, w, h,
                SvgRenderOptions.overrideColor(c.iconShadow).withTextureCache(false).withCurveFlatness(0.18f));
        Renderer2D.COLOR.svg(svg, x, y, w, h, svgGradient(c));
    }

    static void drawFitOrMarquee(TextRenderer renderer, String text, float x, float y, float size, float maxWidth, int color, boolean marquee) {
        if (text == null || text.isEmpty() || maxWidth <= 1f) return;
        float textW = width(renderer, text, size);
        if (textW <= maxWidth) {
            draw(renderer, text, x, y, size, color);
            return;
        }
        if (!marquee) {
            draw(renderer, ellipsize(renderer, text, size, maxWidth), x, y, size, color);
            return;
        }
        drawScrollingText(renderer, text, x, y, textW, maxWidth, size, color);
    }

    static void drawScrollingText(TextRenderer renderer, String text, float x, float y, float fullWidth, float viewWidth, float size, int color) {
        if (renderer == null || text == null || text.isEmpty() || viewWidth <= 1f) return;
        float cycleDistance = fullWidth + MARQUEE_GAP;
        float cycleTime = MARQUEE_PAUSE + cycleDistance / Math.max(1f, MARQUEE_SPEED);
        float time = (Util.getMillis() / 1000.0f) % cycleTime;
        float offset = time <= MARQUEE_PAUSE ? 0f : -(time - MARQUEE_PAUSE) * MARQUEE_SPEED;
        float fade = Math.min(viewWidth * 0.25f, 8f * SCALE);
        RenderColor renderColor = new RenderColor(color);
        renderer.begin(size, false, false);
        try {
            renderer.renderHorizontalFadeClipped(text, x + offset, y, renderColor, x, x + viewWidth, fade, fade, false);
            if (cycleDistance > viewWidth * 0.5f) {
                renderer.renderHorizontalFadeClipped(text, x + offset + cycleDistance, y, renderColor, x, x + viewWidth, fade, fade, false);
            }
        } finally {
            renderer.end();
        }
    }

    static float miniStatusWidth(TextRenderer bodyRenderer, String label) {
        return Math.max(28f * SCALE, width(bodyRenderer, label, SMALL_FONT) + 11f * SCALE);
    }

    record Layout(float leftX, float topY, float rightX, float bottomY) {
    }

    record PanelColors(
            int bgTopLeft,
            int bgTopRight,
            int bgBottomRight,
            int bgBottomLeft,
            int headerTopLeft,
            int headerTopRight,
            int headerBottomRight,
            int headerBottomLeft,
            int stroke,
            int strokeSoft,
            int shadow,
            int title,
            int label,
            int muted,
            int mutedLabel,
            int surface,
            int surfaceHover,
            int accent,
            int accentSoft,
            int detailTop,
            int detailBottom,
            int statusTop,
            int statusBottom,
            int scrollTrack,
            int iconTop,
            int iconBottom,
            int iconShadow
    ) {
    }
}
