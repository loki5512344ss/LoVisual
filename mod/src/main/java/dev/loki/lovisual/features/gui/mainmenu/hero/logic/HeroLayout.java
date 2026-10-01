package dev.loki.lovisual.features.gui.mainmenu.hero.logic;

/**
 * Geometry of the hero main menu, computed from the logical viewport and measured text heights,
 * so lines never overlap whatever the resolution or GUI scale. Free of Minecraft types.
 */
public final class HeroLayout {
    public static final int BUTTON_COUNT = 6;
    public static final int MULTIPLAYER = 0;
    public static final int SINGLEPLAYER = 1;
    public static final int ACCOUNTS = 2;
    public static final int ADDONS = 3;
    public static final int OPTIONS = 4;
    public static final int QUIT = 5;
    private static final float FONT_UNIT_PX = 12f;
    private static final float MIN_K = 0.45f;

    /** Font sizes in logical pixels. */
    public record Sizes(float tag, float title, float sub, float label, float small, float footer, float time) {
    }

    /** @param titleY1 first title line; @param titleY2 second title line */
    public record Layout(float k, Rect account, float avatarSize, Rect widget, float pad,
                         float dotsStartX, float dotY, float dotSpacing, float dotRadius,
                         Rect bar, Rect[] buttons, Sizes sizes,
                         float heroX, float tagY, float titleY1, float titleY2, float subY,
                         float footerY, float footerLine) {
    }

    private HeroLayout() {
    }

    public static float scale(float viewportHeight) {
        return Math.max(0.7f, viewportHeight / 1080f * 1.25f);
    }

    /** Converts logical pixels to the text renderer's size units. */
    public static float font(float px) {
        return px / FONT_UNIT_PX;
    }

    /** @param rise downward slide offset while fading in */
    public static Layout compute(float width, float height, int dotCount, float rise, TextMetrics m) {
        float k = scale(height);
        Layout l = build(width, height, dotCount, rise, m, k);
        for (int i = 0; i < 3 && !fits(l); i++) {
            k = Math.max(MIN_K, k * 0.92f);
            l = build(width, height, dotCount, rise, m, k);
        }
        return l;
    }

    private static boolean fits(Layout l) {
        Rect last = l.buttons()[QUIT];
        return last.y() + last.h() + 14f * l.k() <= l.footerY() || l.k() <= MIN_K;
    }

    private static Layout build(float width, float height, int dotCount, float rise, TextMetrics m, float k) {
        Sizes s = new Sizes(9.5f * k, 26f * k, 11f * k, 12.5f * k, 11.5f * k, 9f * k, 16f * k);
        float avatar = 30f * k;
        float topY = 24f * k + rise * 0.3f;
        Rect account = new Rect(32f * k, topY, avatar + 140f * k, avatar);

        float pad = 10f * k;
        float ww = 215f * k;
        float timeH = m.height(s.time());
        float barH = 10f * k;
        float wh = pad + timeH + 12f * k + barH + pad;
        Rect widget = new Rect(width - ww - 24f * k, topY, ww, wh);
        float spacing = 13f * k;
        float dotsStart = widget.x() + ww - pad - dotCount * spacing + 6f * k;
        float dotY = widget.y() + pad + timeH * 0.5f;
        Rect bar = new Rect(widget.x() + pad, widget.y() + wh - pad - barH, ww - pad * 2f, barH);

        float footerLine = m.height(s.footer()) + 3f * k;
        float footerY = height - 26f * k - footerLine * 3f;
        float tagH = m.height(s.tag());
        float titleH = m.height(s.title());
        float subH = m.height(s.sub());
        float buttonH = Math.max(38f * k, m.height(s.label()) + 22f * k);
        float gap = 8f * k;
        float tagGap = 10f * k;
        float subGap = 14f * k;
        float buttonsGap = 22f * k;
        float blockH = tagH + tagGap + titleH * 2f + 2f * k + subGap + subH + buttonsGap + buttonH * 5f + gap * 4f;

        float top = account.y() + avatar + 30f * k;
        float room = footerY - 14f * k - top;
        float heroY = top + Math.max(0f, (room - blockH) * 0.42f) + rise;
        float heroX = width * 0.045f;
        float titleY1 = heroY + tagH + tagGap;
        float titleY2 = titleY1 + titleH + 2f * k;
        float subY = titleY2 + titleH + subGap;
        Rect[] buttons = buttons(heroX, subY + subH + buttonsGap, k, buttonH, gap);
        return new Layout(k, account, avatar, widget, pad, dotsStart, dotY, spacing, 4.2f * k, bar, buttons, s,
                heroX, heroY, titleY1, titleY2, subY, footerY, footerLine);
    }

    private static Rect[] buttons(float x, float top, float k, float h, float gap) {
        float w = 290f * k;
        Rect[] out = new Rect[BUTTON_COUNT];
        for (int i = 0; i <= ADDONS; i++) {
            out[i] = new Rect(x, top + i * (h + gap), w, h);
        }
        float rowY = top + (ADDONS + 1) * (h + gap);
        float optionsW = (w - gap) * 0.55f;
        out[OPTIONS] = new Rect(x, rowY, optionsW, h);
        out[QUIT] = new Rect(x + optionsW + gap, rowY, w - gap - optionsW, h);
        return out;
    }

    /** Index of the button under the cursor, or -1. */
    public static int buttonAt(Layout layout, float mx, float my) {
        for (int i = 0; i < layout.buttons().length; i++) {
            if (layout.buttons()[i].contains(mx, my)) return i;
        }
        return -1;
    }

    /** Index of the theme dot under the cursor, or -1. */
    public static int dotAt(Layout layout, int dotCount, float mx, float my) {
        float r = layout.dotRadius() + 3f * layout.k();
        for (int i = 0; i < dotCount; i++) {
            float dx = mx - (layout.dotsStartX() + i * layout.dotSpacing());
            float dy = my - layout.dotY();
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }
}
