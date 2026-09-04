/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import dev.loki.lovisual.render.engine.text.FontInfo;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextGlyphFallback;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.engine.text.VanillaTextRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

/**
 * Font resolution, glyph sizing and plain-text draw helpers for the chat renderer.
 * All public-facing metrics share the renderer singleton held on {@link BetterChatRenderer}.
 */
final class BetterChatFontUtil {
    private BetterChatFontUtil() {
    }

    static TextRenderer getInterRegular() {
        return Fonts.renderer("Inter", FontInfo.Type.Regular, TextRenderer.get());
    }

    static TextRenderer getIosevkaRegular() {
        return Fonts.renderer("Iosevka", FontInfo.Type.Regular, TextRenderer.get());
    }

    static TextRenderer getIosevkaItalic() {
        return Fonts.renderer("Iosevka", FontInfo.Type.Italic, getIosevkaRegular());
    }

    static TextRenderer getIosevkaBold() {
        return Fonts.renderer("Iosevka", FontInfo.Type.Bold, getIosevkaRegular());
    }

    static TextRenderer getIosevkaBoldItalic() {
        return Fonts.renderer("Iosevka", FontInfo.Type.BoldItalic, getIosevkaBold());
    }

    static TextRenderer fontRenderer(String key) {
        if (key == null) return getIosevkaRegular();
        return switch (key) {
            case "iosevka_bold" -> getIosevkaBold();
            case "iosevka_bold_italic" -> getIosevkaBoldItalic();
            case "iosevka_medium_italic" -> getIosevkaItalic();
            case "vanilla_symbols" -> TextGlyphFallback.vanillaSymbols(getIosevkaRegular());
            case "vanilla" -> VanillaTextRenderer.INSTANCE;
            default -> getIosevkaRegular();
        };
    }

    static TextRenderer rendererForGlyph(TextRenderer preferred, int codePoint) {
        return TextGlyphFallback.rendererForGlyph(preferred, codePoint);
    }

    static String fontForGlyph(String baseFont, int codePoint) {
        TextRenderer preferred = fontRenderer(baseFont);
        return TextGlyphFallback.fontKeyForGlyph(baseFont, preferred, codePoint, "iosevka_medium");
    }

    static String fontForStyle(Style style) {
        boolean bold = style.isBold();
        boolean italic = style.isItalic();
        if (bold && italic) return "iosevka_bold_italic";
        if (bold) return "iosevka_bold";
        if (italic) return "iosevka_medium_italic";
        return "iosevka_medium";
    }

    static float scaleForSize(float size) {
        return size / 18.0f;
    }

    static float svgGlyphSize(TextRenderer tr, float size, boolean shadow) {
        return Math.max(1.0f, textHeight(tr, size)) * 0.92f;
    }

    static float textHeight(TextRenderer tr, float size) {
        if (tr == null) return 0f;
        float scale = scaleForSize(size);
        tr.begin(scale, true, false);
        try {
            return (float) tr.getHeight(false);
        } finally {
            tr.end();
        }
    }

    static float textWidth(TextRenderer tr, String text, float size) {
        if (tr == null || text == null || text.isEmpty()) return 0f;
        float scale = scaleForSize(size);
        float svgAdvance = svgGlyphSize(tr, size, false);
        TextRenderer current = null;
        float width = 0f;
        try {
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (TextGlyphFallback.shouldUseVanillaSvg(tr, cp)) {
                    if (current != null) {
                        current.end();
                        current = null;
                    }
                    width += svgAdvance;
                    i += Character.charCount(cp);
                    continue;
                }
                String glyph = new String(Character.toChars(cp));
                TextRenderer next = rendererForGlyph(tr, cp);
                if (next != current) {
                    if (current != null) current.end();
                    current = next;
                    current.begin(scale, true, false);
                }
                width += (float) current.getWidth(glyph, false);
                i += Character.charCount(cp);
            }
            return width;
        } finally {
            if (current != null) current.end();
        }
    }

    static void drawText(TextRenderer tr, String text, float x, float y, float size, int argb, boolean shadow) {
        if (tr == null || text == null || text.isEmpty()) return;
        if (((argb >>> 24) & 0xFF) <= 0) return;
        float scale = scaleForSize(size);
        float svgSize = svgGlyphSize(tr, size, shadow);
        float svgY = y + (textHeight(tr, size) - svgSize) * 0.5f;
        BetterChatDrawUtil.applyColor(argb);
        TextRenderer current = null;
        float cursorX = x;
        try {
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (TextGlyphFallback.shouldUseVanillaSvg(tr, cp)) {
                    if (current != null) {
                        current.end();
                        current = null;
                    }
                    String svgName = TextGlyphFallback.vanillaSvgName(cp);
                    if (BetterChatRenderer.renderer != null && svgName != null) {
                        BetterChatRenderer.renderer.svg(svgName, cursorX, svgY, svgSize, svgSize,
                                dev.loki.lovisual.render.engine.svg.SvgRenderOptions.fromFile().withAlpha(((argb >>> 24) & 0xFF) / 255.0f));
                    }
                    cursorX += svgSize;
                    i += Character.charCount(cp);
                    continue;
                }
                String glyph = new String(Character.toChars(cp));
                TextRenderer next = rendererForGlyph(tr, cp);
                if (next != current) {
                    if (current != null) current.end();
                    current = next;
                    current.begin(scale, false, false);
                }
                cursorX = (float) current.render(glyph, cursorX, y, BetterChatRenderer.TMP_COLOR, shadow);
                i += Character.charCount(cp);
            }
        } finally {
            if (current != null) current.end();
        }
    }

    static String fitText(TextRenderer font, String text, float size, float maxWidth) {
        if (text == null) return "";
        if (textWidth(font, text, size) <= maxWidth) return text;
        String suffix = "...";
        float suffixW = textWidth(font, suffix, size);
        int end = text.length();
        while (end > 0 && textWidth(font, text.substring(0, end), size) + suffixW > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }

    static String safeSub(String s, int start, int end) {
        if (s == null) return "";
        int len = s.length();
        start = Math.max(0, Math.min(len, start));
        end = Math.max(0, Math.min(len, end));
        if (end < start) {
            int t = start;
            start = end;
            end = t;
        }
        return s.substring(start, end);
    }

    static int clampIndex(int idx, int len) {
        return Mth.clamp(idx, 0, Math.max(0, len));
    }
}