/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

/**
 * Second render phase: turns the laid-out visual lines into a viewport + frame layout
 * (bubbles/lines) and animates smooth scrolling. Writes the resulting {@link BetterChatFrameModel.FrameLayout}
 * back onto the facade and populates the per-frame sizing context used by every later phase.
 */
final class BetterChatViewport {
    private BetterChatViewport() {
    }

    static int countMessageGroups(List<BetterChatModel.VisualLine> lines, int maxLines) {
        if (lines == null || lines.isEmpty() || maxLines <= 0) return 0;
        int groups = 0;
        int lastMessage = Integer.MIN_VALUE;
        int limit = Math.min(lines.size(), maxLines);
        for (int i = 0; i < limit; i++) {
            int messageGroup = lines.get(i).messageGroup();
            if (messageGroup != lastMessage) {
                groups++;
                lastMessage = messageGroup;
            }
        }
        return groups;
    }

    static float messageStackHeight(int lineCount, int groupCount, float lineHeight) {
        if (lineCount <= 0) return 0f;
        return lineCount * lineHeight
                - BetterChatRenderer.LINE_SPACING
                + Math.max(0, groupCount - 1) * BetterChatRenderer.MESSAGE_GAP
                + BetterChatRenderer.MESSAGE_PAD_Y * 2f;
    }

    static int expandStartToGroup(List<BetterChatModel.VisualLine> lines, int start) {
        if (lines == null || lines.isEmpty()) return 0;
        int safe = Mth.clamp(start, 0, lines.size());
        if (safe <= 0 || safe >= lines.size()) return safe;
        int group = lines.get(safe).messageGroup();
        while (safe > 0 && lines.get(safe - 1).messageGroup() == group) {
            safe--;
        }
        return safe;
    }

    static int expandEndToGroup(List<BetterChatModel.VisualLine> lines, int end) {
        if (lines == null || lines.isEmpty()) return 0;
        int safe = Mth.clamp(end, 0, lines.size());
        if (safe <= 0 || safe >= lines.size()) return safe;
        int group = lines.get(safe - 1).messageGroup();
        while (safe < lines.size() && lines.get(safe).messageGroup() == group) {
            safe++;
        }
        return safe;
    }

    static void composeFrame() {
        List<BetterChatModel.VisualLine> allLines = BetterChatRenderer.ctxAllLines;
        BetterChatModel.LayoutResult layoutResult = new BetterChatModel.LayoutResult(allLines, BetterChatRenderer.ctxMessagesSeen);
        List<ChatLine> messages = BetterChatRenderer.lastMessages;
        int totalMessages = BetterChatRenderer.ctxTotalMessages;
        int visibleLimit = BetterChatRenderer.ctxVisibleLimit;
        boolean autoFollow = BetterChatRenderer.ctxAutoFollow;
        boolean showTs = BetterChatRenderer.ctxShowTs;
        float tsWidth = BetterChatRenderer.ctxTsWidth;
        float width = BetterChatRenderer.ctxWidth;
        float fontSize = BetterChatRenderer.lastFontSizeForUi;
        float baseX = BetterChatRenderer.ctxBaseX;
        float baseY = BetterChatRenderer.ctxBaseY;
        boolean chatOpen = BetterChatRenderer.ctxChatOpen;
        long perfT1 = BetterChatRenderer.ctxPerfT1;
        long perfT0 = BetterChatRenderer.ctxPerfT0;

        float lineHeight = fontSize + BetterChatRenderer.LINE_SPACING;
        float boxHeight;
        List<BetterChatModel.VisualLine> visible;
        int localStart;
        float lineYOffset = 0f;
        int targetVisibleForBox = visibleLimit;
        if (allLines.isEmpty()) {
            BetterChatRenderer.maxScrollLines = 0;
            BetterChatRenderer.sbTotalLines = 0;
            BetterChatRenderer.sbVisibleLines = 0;
            boxHeight = 0f;
            visible = Collections.emptyList();
            BetterChatRenderer.smoothScrollOffsetLines = 0f;
        } else {
            int lineCount = allLines.size();
            int observedMessages = Math.max(1, layoutResult.messagesSeen());
            float avgLinesPerMsg = Math.max(1f, (float) lineCount / (float) observedMessages);
            int estimatedTotalLines = Math.max(lineCount, (int) Math.ceil(avgLinesPerMsg * totalMessages));
            BetterChatRenderer.maxScrollLines = Math.max(0, estimatedTotalLines - visibleLimit);
            BetterChatRenderer.scrollOffsetLines = Mth.clamp(BetterChatRenderer.scrollOffsetLines, 0, BetterChatRenderer.maxScrollLines);
            if (autoFollow) {
                BetterChatRenderer.smoothScrollOffsetLines = 0f;
            } else {
                float s = Mth.clamp(BetterChatRenderer.smoothScrollOffsetLines, 0f, BetterChatRenderer.maxScrollLines);
                s = AnimationUtility.approach(s, BetterChatRenderer.scrollOffsetLines, BetterChatRenderer.draggingScrollbar ? 0.55f : 0.22f);
                s = AnimationUtility.snap(s, BetterChatRenderer.scrollOffsetLines, BetterChatRenderer.draggingScrollbar ? 0.01f : 0.035f);
                BetterChatRenderer.smoothScrollOffsetLines = s;
            }

            int lastMsgIndex = messages.isEmpty() ? -1 : messages.size() - 1;
            int linesInLastMsg = 0;
            if (lastMsgIndex >= 0) {
                for (int i = lineCount - 1; i >= 0; i--) {
                    if (allLines.get(i).messageIndex() == lastMsgIndex) {
                        linesInLastMsg++;
                    } else if (linesInLastMsg > 0) {
                        break;
                    }
                }
            }
            int targetVisible = autoFollow ? Math.max(visibleLimit, linesInLastMsg) : visibleLimit;
            targetVisibleForBox = Math.max(1, Math.min(targetVisible, lineCount));

            float globalStartFloat = autoFollow
                    ? Math.max(0f, lineCount - targetVisible)
                    : Math.max(0f, lineCount - visibleLimit - BetterChatRenderer.smoothScrollOffsetLines);
            int globalStart = Math.max(0, (int) Math.floor(globalStartFloat) - (autoFollow ? 0 : 1));
            int viewportStart = autoFollow
                    ? Math.max(0, lineCount - targetVisibleForBox)
                    : Mth.clamp((int) Math.floor(globalStartFloat), 0, Math.max(0, lineCount - 1));
            int viewportEnd = Math.min(lineCount, viewportStart + targetVisibleForBox);
            int groupsForBox = countMessageGroups(allLines.subList(viewportStart, viewportEnd), targetVisibleForBox);

            localStart = Math.max(0, Math.min(globalStart, Math.max(0, lineCount - 1)));
            int endLine = Math.min(lineCount, localStart + targetVisibleForBox + (autoFollow ? 0 : 3));
            localStart = expandStartToGroup(allLines, localStart);
            endLine = expandEndToGroup(allLines, endLine);
            visible = allLines.subList(localStart, endLine);
            BetterChatRenderer.sbTotalLines = lineCount;
            BetterChatRenderer.sbVisibleLines = targetVisibleForBox;

            boxHeight = messageStackHeight(targetVisibleForBox, groupsForBox, lineHeight);
            if (autoFollow) {
                int actualGroups = countMessageGroups(visible, visible.size());
                float actualHeight = messageStackHeight(visible.size(), actualGroups, lineHeight);
                lineYOffset = boxHeight - actualHeight;
            } else {
                lineYOffset = (localStart - globalStartFloat) * lineHeight;
            }
        }
        float stableBoxHeight = messageStackHeight(visibleLimit, visibleLimit, lineHeight);
        float fixedInputY = baseY + stableBoxHeight + BetterChatRenderer.MESSAGE_INPUT_GAP;
        float frameY = baseY + stableBoxHeight - boxHeight;
        float visualTop = Math.min(baseY, frameY);
        float visualBottom = chatOpen ? fixedInputY + Math.max(36f, fontSize + 16f) : frameY + boxHeight;
        float totalHeight = Math.max(0f, visualBottom - visualTop);
        BetterChatRenderer.lastRenderWidth = width;
        BetterChatRenderer.lastRenderHeight = totalHeight;

        long perfT2Start = System.nanoTime();
        BetterChatRenderer.frame = BetterChatFrameModel.FrameLayout.build(visible, baseX, frameY, width, boxHeight, fontSize, lineHeight, lineYOffset, showTs ? tsWidth : 0f);
        long perfT2 = System.nanoTime();
        BetterChatRenderer.debugPerfAccumLayoutNs += (perfT1 - perfT0);
        BetterChatRenderer.debugPerfAccumBuildNs += (perfT2 - perfT2Start);
        BetterChatRenderer.debugPerfAccumFrames++;
        if (BetterChatRenderer.debugPerfWindowStartNs == 0L) BetterChatRenderer.debugPerfWindowStartNs = perfT2;
        long windowNs = perfT2 - BetterChatRenderer.debugPerfWindowStartNs;
        if (windowNs >= 1_000_000_000L && BetterChatRenderer.debugPerfAccumFrames > 0 && chatOpen) {
            BetterChatRenderer.debugPerfWindowStartNs = perfT2;
            BetterChatRenderer.debugPerfAccumLayoutNs = 0L;
            BetterChatRenderer.debugPerfAccumBuildNs = 0L;
            BetterChatRenderer.debugPerfAccumFrames = 0;
        }

        BetterChatRenderer.ctxLineHeight = lineHeight;
        BetterChatRenderer.ctxBoxHeight = boxHeight;
        BetterChatRenderer.ctxVisible = visible;
        BetterChatRenderer.ctxLineYOffset = lineYOffset;
        BetterChatRenderer.ctxTargetVisible = targetVisibleForBox;
        BetterChatRenderer.ctxStableBoxHeight = stableBoxHeight;
        BetterChatRenderer.ctxFixedInputY = fixedInputY;
        BetterChatRenderer.ctxFrameY = frameY;
    }
}