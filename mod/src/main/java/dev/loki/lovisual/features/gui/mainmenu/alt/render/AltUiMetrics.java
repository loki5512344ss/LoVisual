/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.mainmenu.alt.render;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Set;

/**
 * Shared layout metrics and small drawing helpers for the alt manager UI.
 */
public final class AltUiMetrics {
    public static final float SCALE = 2.28f;
    public static final float TEXT_SCALE = 1.30f;

    public static final float LEFT_W = 100f * SCALE;
    public static final float LEFT_TOP_H = 137f * SCALE;
    public static final float LEFT_BOTTOM_H = 58f * SCALE;
    public static final float RIGHT_W = 300f * SCALE;
    public static final float RIGHT_H = 165f * SCALE;
    public static final float GAP = 5f * SCALE;

    public static final float PANEL_R = 6f * SCALE;
    public static final float PANEL_SHADOW = 12f * SCALE;
    public static final float LEFT_CARD_R = 4f * SCALE;
    public static final float CARD_MARKER_W = 2.2f * SCALE;

    public static final float TITLE_FONT = 0.84f * TEXT_SCALE;
    public static final float LABEL_FONT = 0.58f * TEXT_SCALE;
    public static final float FIELD_FONT = 0.60f * TEXT_SCALE;
    public static final float BUTTON_FONT = 0.58f * TEXT_SCALE;
    public static final float ACTION_ICON_FONT = 1.02f * TEXT_SCALE;
    public static final float CARD_NAME_FONT = 0.72f * TEXT_SCALE;
    public static final float CARD_DATE_FONT = 0.61f * TEXT_SCALE;
    public static final float ACTIVE_NAME_FONT = 0.66f * TEXT_SCALE;
    public static final float ACTIVE_DATE_FONT = 0.50f * TEXT_SCALE;
    public static final float EMPTY_FONT = 0.56f * TEXT_SCALE;
    public static final float BADGE_FONT = 0.45f * TEXT_SCALE;
    public static final float PIN_ICON_FONT = 0.93f * TEXT_SCALE;
    public static final float DELETE_ICON_FONT = 1.05f * TEXT_SCALE;

    public static final float FIELD_H = 14f * SCALE;
    public static final float ADD_SIZE = 14f * SCALE;
    public static final float TOP_BUTTON_H = 16f * SCALE;
    public static final float CARD_H = 40f * SCALE;
    public static final float CARD_GAP = 5f * SCALE;
    public static final float CARD_BTN = 12f * SCALE;
    public static final float ACTIVE_HEAD = 24f * SCALE;
    public static final float CARD_HEAD = 25f * SCALE;

    private AltUiMetrics() {
    }

    public static float animate(float current, float target, float dt, float speed) {
        float next = AnimationUtility.approach(current, target, dt, speed);
        return AnimationUtility.snap(next, target, 0.001f);
    }

    public static void prune(Map<String, Float> map, Set<String> keys) {
        map.keySet().removeIf(key -> !keys.contains(key));
    }

    public static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
    }

    public static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static void draw(TextRenderer renderer, String text, float x, float y, float size, int argb) {
        if (renderer == null || text == null || text.isEmpty()) return;
        renderer.setAlpha(1.0);
        renderer.begin(size, false, false);
        renderer.render(text, x, y, new RenderColor(argb), false);
        renderer.end();
    }

    public static void drawCentered(TextRenderer renderer, String text, float centerX, float y, float size, int argb) {
        draw(renderer, text, centerX - width(renderer, text, size) * 0.5f, y, size, argb);
    }

    public static float width(TextRenderer renderer, String text, float size) {
        if (renderer == null || text == null) return 0f;
        renderer.begin(size, true, false);
        float w = (float) renderer.getWidth(text, false);
        renderer.end();
        return w;
    }

    public static String ellipsize(TextRenderer renderer, String text, float size, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (width(renderer, text, size) <= maxWidth) return text;
        String out = text;
        while (out.length() > 3 && width(renderer, out + "...", size) > maxWidth)
            out = out.substring(0, out.length() - 1);
        return out + "...";
    }

    public record Layout(float leftX, float topY, float rightX, float bottomY) {
    }

    public record PanelColors(
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
            int accentSoft
    ) {
    }
}
