package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

final class MediaPlayerConstants {

    static final float BASE_PADDING = 6f;
    static final float BASE_GAP = 6f;
    static final float BASE_ART_SIZE = 30f;
    static final float BASE_ART_BOOST = 3f;
    static final float BASE_ART_OVERHANG = 4f;
    static final float BASE_RADIUS = 6f;
    static final float BASE_BAR_H = 2.4f;
    static final float BASE_BUTTON_SIZE = 16f;
    static final float BASE_BUTTON_GAP = 8f;
    static final float BASE_TEXT_GAP = 2f;
    static final float BASE_BAR_GAP = 4f;
    static final float BASE_BOTTOM_GAP = 4f;
    static final float BASE_TITLE_SCROLL_SPEED = 22f;
    static final float BASE_TITLE_SCROLL_GAP = 14f;
    static final float BASE_TITLE_FADE = 12f;
    static final float TITLE_SCROLL_PAUSE_SEC = 0.8f;
    static final String EFFECT_NONE = "None";
    static final String EFFECT_BLUR = "Blur";
    static final String EFFECT_GLASS = "Glass";
    static final float BASE_CONTENT_W = 170f;
    static final int ICON_PAUSE = 0xEA02;
    static final int ICON_PLAY = 0xEA03;
    static final int ICON_PREV = 0xEA04;
    static final int ICON_NEXT = 0xEA05;
    static final int ICON_SHUFFLE = 0xEA06;

    private MediaPlayerConstants() {}

    static String iconString(int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    static TextRenderer getMediaIcons(TextRenderer fallback) {
        return Fonts.renderer("MediaPlayer", FontInfo.Type.Regular, fallback);
    }

    static String safe(String value) {
        if (value == null) return "";
        String v = value.trim();
        return v.isEmpty() ? "" : v;
    }

    static String formatTime(long seconds) {
        long sec = Math.max(0L, seconds);
        long m = sec / 60L;
        long s = sec % 60L;
        return String.format("%d:%02d", m, s);
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
