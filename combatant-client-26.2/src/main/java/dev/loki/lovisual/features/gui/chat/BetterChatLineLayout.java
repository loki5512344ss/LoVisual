/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

import dev.loki.lovisual.render.engine.text.TextGlyphFallback;
import dev.loki.lovisual.render.engine.text.TextRenderer;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Builds per-message wrapped glyph lines (cached on identity maps held by the facade).
 * Extracted from {@link BetterChatRenderer}; behavior unchanged.
 */
final class BetterChatLineLayout {
    private BetterChatLineLayout() {
    }

    static List<BetterChatModel.CachedLine> cachedMessageLines(ChatLine msg, float fontSize, float maxWidth) {
        BetterChatModel.CachedMessageLayout cached = BetterChatRenderer.MESSAGE_LAYOUT_CACHE.get(msg);
        if (cached != null && Math.abs(cached.fontSize() - fontSize) < 0.01f && Math.abs(cached.maxWidth() - maxWidth) < 0.5f) {
            return cached.lines();
        }

        List<BetterChatModel.Segment> segments = BetterChatRenderer.SEGMENT_CACHE.computeIfAbsent(msg, m -> BetterChatSegmentUtil.flatten(m.text()));
        List<BetterChatModel.CachedLine> rebuilt = new ArrayList<>();
        TextRenderer activeRenderer = null;
        String activeFont = null;
        float scale = BetterChatFontUtil.scaleForSize(fontSize);

        int charIndex = 0;
        float lineX = 0f;
        List<BetterChatModel.Glyph> glyphs = new ArrayList<>();
        int lineStartIndex = 0;
        int breakGlyphIdx = -1;
        int breakCharIdx = -1;

        for (BetterChatModel.Segment seg : segments) {
            Style style = seg.style();
            String font = BetterChatFontUtil.fontForStyle(style);
            int color = BetterChatSegmentUtil.resolveSegmentColor(seg, style, style.getHoverEvent());
            HoverEvent hover = style.getHoverEvent();
            String txt = seg.text();

            for (int c = 0; c < txt.length(); ) {
                int cp = txt.codePointAt(c);
                int cpLen = Character.charCount(cp);

                if (cp == '\n') {
                    rebuilt.add(new BetterChatModel.CachedLine(lineStartIndex, Math.max(lineStartIndex, charIndex - 1), glyphs));
                    glyphs = new ArrayList<>();
                    lineX = 0f;
                    lineStartIndex = charIndex;
                    breakGlyphIdx = -1;
                    breakCharIdx = -1;
                    charIndex += 1;
                    c += 1;
                    continue;
                }

                String glyphText = new String(Character.toChars(cp));

                String resolvedFont = BetterChatFontUtil.fontForGlyph(font, cp);
                boolean svgGlyph = TextGlyphFallback.isSvgFontKey(resolvedFont);
                TextRenderer tr = svgGlyph ? null : BetterChatFontUtil.fontRenderer(resolvedFont);

                if (svgGlyph) {
                    if (activeRenderer != null) {
                        activeRenderer.end();
                        activeRenderer = null;
                    }
                    activeFont = resolvedFont;
                } else if (tr != activeRenderer) {
                    if (activeRenderer != null) {
                        activeRenderer.end();
                    }
                    activeRenderer = tr;
                    activeFont = resolvedFont;
                    if (activeRenderer != null) {
                        activeRenderer.begin(scale, true, false);
                    }
                }
                float cw = svgGlyph
                        ? BetterChatFontUtil.svgGlyphSize(BetterChatFontUtil.fontRenderer(font), fontSize, false)
                        : activeRenderer != null ? (float) activeRenderer.getWidth(glyphText, false) : 0f;
                int glyphCharIndex = charIndex;

                if (lineX + cw > maxWidth && lineX > 0f) {
                    if (breakGlyphIdx >= 0) {
                        List<BetterChatModel.Glyph> before = new ArrayList<>(glyphs.subList(0, breakGlyphIdx));
                        int endChar = Math.max(lineStartIndex, breakCharIdx - 1);
                        rebuilt.add(new BetterChatModel.CachedLine(lineStartIndex, endChar, before));

                        List<BetterChatModel.Glyph> remaining = glyphs.subList(breakGlyphIdx + 1, glyphs.size());
                        glyphs = new ArrayList<>(remaining.size());
                        float nx = 0f;
                        for (BetterChatModel.Glyph g : remaining) {
                            float gw = g.x1() - g.x0();
                            glyphs.add(new BetterChatModel.Glyph(
                                    nx,
                                    nx + gw,
                                    g.charIndex(),
                                    g.charEndExclusive(),
                                    g.color(),
                                    g.hover(),
                                    g.font(),
                                    g.text(),
                                    g.style()
                            ));
                            nx += gw;
                        }
                        lineX = nx;
                        lineStartIndex = breakCharIdx + 1;
                        breakGlyphIdx = -1;
                        breakCharIdx = -1;
                    } else {
                        rebuilt.add(new BetterChatModel.CachedLine(lineStartIndex, Math.max(lineStartIndex, charIndex - 1), glyphs));
                        glyphs = new ArrayList<>();
                        lineX = 0f;
                        lineStartIndex = charIndex;
                    }
                }

                glyphs.add(new BetterChatModel.Glyph(
                        lineX,
                        lineX + cw,
                        glyphCharIndex,
                        glyphCharIndex + cpLen,
                        color,
                        hover,
                        activeFont != null ? activeFont : resolvedFont,
                        glyphText,
                        style
                ));
                lineX += cw;

                charIndex += cpLen;

                if (cp == ' ') {
                    breakGlyphIdx = glyphs.size() - 1;
                    breakCharIdx = glyphCharIndex;
                }

                c += cpLen;
            }
        }

        if (!glyphs.isEmpty()) {
            rebuilt.add(new BetterChatModel.CachedLine(lineStartIndex, charIndex - 1, glyphs));
        } else if (charIndex == 0) {
            rebuilt.add(new BetterChatModel.CachedLine(0, 0, List.of(new BetterChatModel.Glyph(
                    0f,
                    2f,
                    0,
                    1,
                    theme().textPrimary(),
                    null,
                    "iosevka_medium",
                    " ",
                    Style.EMPTY
            ))));
        }

        if (activeRenderer != null) {
            activeRenderer.end();
        }
        BetterChatRenderer.MESSAGE_LAYOUT_CACHE.put(msg, new BetterChatModel.CachedMessageLayout(fontSize, maxWidth, rebuilt));
        return rebuilt;
    }
}