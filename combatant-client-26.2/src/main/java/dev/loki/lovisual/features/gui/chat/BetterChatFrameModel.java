/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Frame layout record: builds screen-space line/bubble geometry from laid-out visual lines.
 * Pure geometry + picking, no rendering or font work.
 */
final class BetterChatFrameModel {
    private BetterChatFrameModel() {
    }

    record FrameLayout(float x, float y, float w, float h, List<BetterChatModel.FrameLine> lines, List<BetterChatModel.MessageBubble> bubbles) {
        static FrameLayout empty() {
            return new FrameLayout(0, 0, 0, 0, Collections.emptyList(), Collections.emptyList());
        }

        static FrameLayout build(List<BetterChatModel.VisualLine> lines, float x, float y, float w, float h, float fontSize, float lineHeight, float yOffset, float timestampReserve) {
            List<BetterChatModel.FrameLine> list = new ArrayList<>(lines.size());
            float cursorY = y + BetterChatRenderer.MESSAGE_PAD_Y + yOffset;
            int lastMessageGroup = Integer.MIN_VALUE;
            for (int i = 0; i < lines.size(); i++) {
                BetterChatModel.VisualLine vl = lines.get(i);
                if (i > 0 && vl.messageGroup() != lastMessageGroup) {
                    cursorY += BetterChatRenderer.MESSAGE_GAP;
                }
                lastMessageGroup = vl.messageGroup();
                float y0 = cursorY;
                float y1 = y0 + fontSize;
                List<BetterChatModel.GlyphBox> boxes = new ArrayList<>(vl.glyphs().size());
                for (BetterChatModel.Glyph g : vl.glyphs()) {
                    boxes.add(new BetterChatModel.GlyphBox(
                            x + BetterChatRenderer.PADDING + BetterChatRenderer.MESSAGE_PAD_X + g.x0(),
                            y0,
                            x + BetterChatRenderer.PADDING + BetterChatRenderer.MESSAGE_PAD_X + g.x1(),
                            y1,
                            g.charIndex(),
                            g.charEndExclusive(),
                            g.color(),
                            g.font(),
                            g.hover(),
                            g.text(),
                            g.style()
                    ));
                }
                list.add(new BetterChatModel.FrameLine(vl.message(), vl.messageIndex(), vl.messageGroup(), y0, y1, boxes));
                cursorY += lineHeight;
            }
            return new FrameLayout(x, y, w, h, list, buildBubbles(list, x, w, timestampReserve));
        }

        private static List<BetterChatModel.MessageBubble> buildBubbles(List<BetterChatModel.FrameLine> lines, float x, float w, float timestampReserve) {
            if (lines.isEmpty()) return Collections.emptyList();
            List<BetterChatModel.MessageBubble> bubbles = new ArrayList<>();
            int start = 0;
            while (start < lines.size()) {
                BetterChatModel.FrameLine first = lines.get(start);
                int end = start;
                float maxRight = x + BetterChatRenderer.PADDING + BetterChatRenderer.MESSAGE_PAD_X;
                while (end + 1 < lines.size() && lines.get(end + 1).messageGroup() == first.messageGroup()) {
                    end++;
                }
                for (int i = start; i <= end; i++) {
                    BetterChatModel.FrameLine line = lines.get(i);
                    if (!line.glyphs().isEmpty()) {
                        maxRight = Math.max(maxRight, line.glyphs().getLast().x1());
                    }
                }
                BetterChatModel.FrameLine last = lines.get(end);
                float bubbleX = x + BetterChatRenderer.PADDING;
                float bubbleY = first.y0() - BetterChatRenderer.MESSAGE_PAD_Y;
                float maxBubbleW = Math.max(32f, w - BetterChatRenderer.PADDING * 2f);
                float desiredW = maxRight - bubbleX + BetterChatRenderer.MESSAGE_PAD_X + Math.max(0f, timestampReserve);
                float bubbleW = Mth.clamp(desiredW, 76f, maxBubbleW);
                float bubbleH = last.y1() - first.y0() + BetterChatRenderer.MESSAGE_PAD_Y * 2f;
                bubbles.add(new BetterChatModel.MessageBubble(last.message(), first.messageGroup(), bubbleX, bubbleY, bubbleW, bubbleH, first, last));
                start = end + 1;
            }
            return bubbles;
        }

        BetterChatModel.MessageClipBounds messageClipBounds() {
            if (lines.isEmpty() || h <= 0f) return null;
            float clipX = x + BetterChatRenderer.PADDING;
            float clipW = Math.max(1f, w - BetterChatRenderer.PADDING * 2f);
            return new BetterChatModel.MessageClipBounds(clipX, y, clipW, h);
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        BetterChatModel.PickResult pick(double mx, double my) {
            if (!contains(mx, my)) return null;
            for (BetterChatModel.FrameLine line : lines) {
                if (my < line.y0() || my > line.y1() + BetterChatRenderer.LINE_SPACING) continue;
                if (line.glyphs().isEmpty()) continue;
                float minX = line.glyphs().getFirst().x0();
                float maxX = line.glyphs().getLast().x1();
                float margin = 2f;
                if (mx < minX - margin || mx > maxX + margin) continue;
                BetterChatModel.GlyphBox closest = null;
                for (BetterChatModel.GlyphBox g : line.glyphs()) {
                    if (mx >= g.x0() && mx <= g.x1()) {
                        return new BetterChatModel.PickResult(line, g);
                    }
                    if (closest == null || Math.abs(mx - g.x1()) < Math.abs(mx - closest.x1())) {
                        closest = g;
                    }
                }
                if (closest != null) {
                    return new BetterChatModel.PickResult(line, closest);
                }
            }
            return null;
        }
    }
}