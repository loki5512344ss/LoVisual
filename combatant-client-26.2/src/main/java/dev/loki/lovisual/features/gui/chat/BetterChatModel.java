/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * Shared package-private records extracted from {@link BetterChatRenderer}.
 * The only public-facing record kept nested on the facade is {@link BetterChatRenderer.Layout}.
 * Records that own non-trivial rendering logic ({@code FrameLayout}, {@code ContextMenu}) live in
 * their own files ({@link BetterChatFrameModel}, {@link BetterChatContextMenuModel}).
 */
final class BetterChatModel {
    private BetterChatModel() {
    }

    record MousePos(double rawX, double rawY, double fx, double fy) {
    }

    record InputLine(int start, int end, float width) {
    }

    record Segment(String text, Style style) {
    }

    record VisualLine(ChatLine message, int messageIndex, int messageGroup, int startChar, int endChar, List<Glyph> glyphs) {
    }

    record Glyph(
            float x0,
            float x1,
            int charIndex,
            int charEndExclusive,
            int color,
            HoverEvent hover,
            String font,
            String text,
            Style style
    ) {
    }

    record GlyphBox(
            float x0,
            float y0,
            float x1,
            float y1,
            int charIndex,
            int charEndExclusive,
            int color,
            String font,
            HoverEvent hover,
            String text,
            Style style
    ) {
    }

    record CachedLine(int startChar, int endChar, List<Glyph> glyphs) {
    }

    record CachedMessageLayout(float fontSize, float maxWidth, List<CachedLine> lines) {
    }

    record FrameLine(ChatLine message, int messageIndex, int messageGroup, float y0, float y1, List<GlyphBox> glyphs) {
    }

    record MessageBubble(ChatLine message, int messageGroup, float x, float y, float w, float h, FrameLine firstLine, FrameLine lastLine) {
    }

    record MessageClipBounds(float x, float y, float w, float h) {
    }

    record PickResult(FrameLine line, GlyphBox glyph) {
    }

    record LayoutResult(List<VisualLine> lines, int messagesSeen) {
    }
}