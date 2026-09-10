/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat.model;

import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.List;
import dev.loki.lovisual.features.gui.chat.menu.BetterChatContextMenuModel;
import dev.loki.lovisual.features.gui.chat.render.BetterChatFrameModel;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;

/**
 * Shared package-private records extracted from {@link BetterChatRenderer}.
 * The only public-facing record kept nested on the facade is {@link BetterChatRenderer.Layout}.
 * Records that own non-trivial rendering logic ({@code FrameLayout}, {@code ContextMenu}) live in
 * their own files ({@link BetterChatFrameModel}, {@link BetterChatContextMenuModel}).
 */
public final class BetterChatModel {
    private BetterChatModel() {
    }

    public record MousePos(double rawX, double rawY, double fx, double fy) {
    }

    public record InputLine(int start, int end, float width) {
    }

    public record Segment(String text, Style style) {
    }

    public record VisualLine(ChatLine message, int messageIndex, int messageGroup, int startChar, int endChar, List<Glyph> glyphs) {
    }

    public record Glyph(
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

    public record GlyphBox(
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

    public record CachedLine(int startChar, int endChar, List<Glyph> glyphs) {
    }

    public record CachedMessageLayout(float fontSize, float maxWidth, List<CachedLine> lines) {
    }

    public record FrameLine(ChatLine message, int messageIndex, int messageGroup, float y0, float y1, List<GlyphBox> glyphs) {
    }

    public record MessageBubble(ChatLine message, int messageGroup, float x, float y, float w, float h, FrameLine firstLine, FrameLine lastLine) {
    }

    public record MessageClipBounds(float x, float y, float w, float h) {
    }

    public record PickResult(FrameLine line, GlyphBox glyph) {
    }

    public record LayoutResult(List<VisualLine> lines, int messagesSeen) {
    }
}
