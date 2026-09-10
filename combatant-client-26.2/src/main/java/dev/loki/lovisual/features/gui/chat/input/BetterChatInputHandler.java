/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat.input;

import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import dev.loki.lovisual.features.gui.chat.model.BetterChatModel;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.suggestions.BetterChatSuggestionState;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;

/**
 * Chat-surface interaction: scroll, drag-selection movement, scrollbar drag and hit-testing.
 * The mouse-button dispatch lives in {@link BetterChatMouseClickHandler}.
 */
public final class BetterChatInputHandler {
    private BetterChatInputHandler() {
    }

    public static BetterChatModel.MousePos resolveMouse(double rawX, double rawY) {
        return new BetterChatModel.MousePos(rawX, rawY, rawX, rawY);
    }

    static int selectionCaretIndex(BetterChatModel.GlyphBox glyph) {
        if (glyph == null) return 0;
        return Math.max(glyph.charIndex(), glyph.charEndExclusive() - 1);
    }

    static BetterChatModel.PickResult nearestFramePick(double mx, double my) {
        if (BetterChatRenderer.frame == null || BetterChatRenderer.frame.lines() == null || BetterChatRenderer.frame.lines().isEmpty())
            return null;

        BetterChatModel.FrameLine bestLine = null;
        float bestDist = Float.MAX_VALUE;
        for (BetterChatModel.FrameLine line : BetterChatRenderer.frame.lines()) {
            float dy;
            if (my < line.y0()) {
                dy = (float) (line.y0() - my);
            } else if (my > line.y1() + BetterChatRenderer.LINE_SPACING) {
                dy = (float) (my - (line.y1() + BetterChatRenderer.LINE_SPACING));
            } else {
                dy = 0.0f;
            }
            if (dy < bestDist) {
                bestDist = dy;
                bestLine = line;
            }
        }

        if (bestLine == null || bestLine.glyphs().isEmpty()) return null;
        BetterChatModel.GlyphBox first = bestLine.glyphs().getFirst();
        BetterChatModel.GlyphBox last = bestLine.glyphs().getLast();
        BetterChatModel.GlyphBox glyph = mx <= first.x0() ? first : last;
        return new BetterChatModel.PickResult(bestLine, glyph);
    }

    public static boolean onScroll(double delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!BetterChat.isActive()) return false;
        if (!(ClientScreen.current() instanceof ChatScreen)) return false;
        BetterChatModel.MousePos mouse = resolveMouse(mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
        double mx = mouse.fx();
        double my = mouse.fy();
        double rawX = mouse.rawX();
        double rawY = mouse.rawY();
        boolean insideSuggest = BetterChatSuggestionState.isInsideSuggestWindow(mx, my, rawX, rawY);
        if (insideSuggest) {
            int step = delta > 0 ? -1 : 1;
            int scrollable = Math.max(0, BetterChatRenderer.suggestTotal - BetterChatRenderer.suggestVisible);
            BetterChatRenderer.suggestCurrentStart = net.minecraft.util.Mth.clamp(BetterChatRenderer.suggestCurrentStart + step, 0, scrollable);
            BetterChatSuggestionState.applySuggestionWindowState(BetterChatRenderer.suggestCurrentStart, BetterChatRenderer.suggestCurrentSelection);
            return true;
        }
        int step = delta > 0 ? 3 : -3;
        BetterChatRenderer.scrollOffsetLines = net.minecraft.util.Mth.clamp(BetterChatRenderer.scrollOffsetLines + step, 0, BetterChatRenderer.maxScrollLines);
        BetterChatRenderer.lastScrollInteractionMs = System.currentTimeMillis();
        return false;
    }

    public static void onMouseMove(double mx, double my) {
        if (!BetterChat.isActive()) return;
        BetterChatModel.MousePos mouse = resolveMouse(mx, my);
        double fx = mouse.fx();
        double fy = mouse.fy();
        if (BetterChatRenderer.draggingScrollbar) {
            BetterChatDrawUtil.scrollToPosition(fy);
            return;
        }
        if (BetterChatRenderer.suggestDraggingScrollbar) {
            BetterChatSuggestionState.scrollSuggestToPosition(fy);
            return;
        }
        if (BetterChatSuggestionState.isInsideSuggestWindow(fx, fy, mouse.rawX(), mouse.rawY())) return;

        if (BetterChatRenderer.leftPending && !BetterChatRenderer.selecting) {
            double dx = fx - BetterChatRenderer.leftDownFx;
            double dy = fy - BetterChatRenderer.leftDownFy;
            if (dx * dx + dy * dy >= BetterChatRenderer.DRAG_SELECT_THRESHOLD * BetterChatRenderer.DRAG_SELECT_THRESHOLD) {
                BetterChatRenderer.selection.update(BetterChatRenderer.leftDownMsgIndex, BetterChatRenderer.leftDownCharIndex);
                BetterChatRenderer.selecting = true;
            } else {
                return;
            }
        }

        if (!BetterChatRenderer.selecting) return;
        BetterChatModel.PickResult pick = BetterChatRenderer.frame.pick(mouse.fx(), mouse.fy());
        if (pick == null) {
            pick = nearestFramePick(mouse.fx(), mouse.fy());
        }
        if (pick == null) return;
        BetterChatRenderer.selection.updateCaret(pick.line().messageIndex(), selectionCaretIndex(pick.glyph()));
    }

    public static void resetScroll() {
        BetterChatRenderer.scrollOffsetLines = 0;
        BetterChatRenderer.smoothScrollOffsetLines = 0f;
        BetterChatRenderer.draggingScrollbar = false;
        BetterChatRenderer.sbEnabled = false;
        BetterChatRenderer.sbDragOffset = 0f;
    }

    public static void onNewMessage() {
        if (BetterChatRenderer.scrollOffsetLines > 0 && BetterChatRenderer.scrollOffsetLines <= 2) {
            BetterChatRenderer.scrollOffsetLines = 0;
            BetterChatRenderer.smoothScrollOffsetLines = 0f;
        }
    }

    public static boolean isInteractive(float mx, float my) {
        boolean inChat = BetterChatRenderer.chatHitW > 0f && mx >= BetterChatRenderer.chatHitX && mx <= BetterChatRenderer.chatHitX + BetterChatRenderer.chatHitW && my >= BetterChatRenderer.chatHitY && my <= BetterChatRenderer.chatHitY + BetterChatRenderer.chatHitH;
        boolean inSearch = BetterChatRenderer.searchHit && mx >= BetterChatRenderer.searchRectX && mx <= BetterChatRenderer.searchRectX + BetterChatRenderer.searchRectW && my >= BetterChatRenderer.searchRectY && my <= BetterChatRenderer.searchRectY + BetterChatRenderer.searchRectH;
        boolean inInput = BetterChatRenderer.hasInputBox && mx >= BetterChatRenderer.inputBoxX && mx <= BetterChatRenderer.inputBoxX + BetterChatRenderer.inputBoxW && my >= BetterChatRenderer.inputBoxY && my <= BetterChatRenderer.inputBoxY + BetterChatRenderer.inputBoxH;
        boolean inSuggest = BetterChatSuggestionState.isInsideSuggestWindow(mx, my);
        boolean inContext = BetterChatRenderer.contextMenu.open() && BetterChatRenderer.contextMenu.contains(mx, my);
        boolean inScrollbar = BetterChatRenderer.sbEnabled && BetterChatDrawUtil.isScrollbarHovered(mx, my);
        return inChat || inSearch || inInput || inSuggest || inContext || inScrollbar;
    }

    public static boolean contains(float mx, float my) {
        return isInteractive(mx, my);
    }

    public static void updateChatHit() {
        float hitBottom = BetterChatRenderer.hasInputBox ? BetterChatRenderer.inputBoxY + BetterChatRenderer.inputBoxH : BetterChatRenderer.ctxFixedInputY;
        BetterChatRenderer.chatHitX = BetterChatRenderer.ctxBaseX;
        BetterChatRenderer.chatHitY = BetterChatRenderer.frame.y();
        BetterChatRenderer.chatHitW = BetterChatRenderer.ctxWidth;
        BetterChatRenderer.chatHitH = Math.max(BetterChatRenderer.ctxBoxHeight, hitBottom - BetterChatRenderer.chatHitY);
    }

    public static void clearChatHit() {
        BetterChatRenderer.chatHitX = BetterChatRenderer.chatHitY = BetterChatRenderer.chatHitW = BetterChatRenderer.chatHitH = 0f;
        BetterChatRenderer.lastMouseValid = false;
    }
}
