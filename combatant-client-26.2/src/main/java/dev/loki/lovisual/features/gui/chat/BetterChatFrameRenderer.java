/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.SystemCursor;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Single entry point for the chat HUD render pass; orchestrates the layout, message, input,
 * suggestion, scrollbar and tooltip phases. All real work lives in the other package-private helpers.
 */
final class BetterChatFrameRenderer {
    private BetterChatFrameRenderer() {
    }

    static void renderEngine(Renderer2D rendererIn,
                              TextRenderer fallback,
                              GuiGraphicsExtractor ctx,
                              int screenW,
                              float baseX,
                              float baseY) {
        BetterChatRenderer.renderer = rendererIn;
        SystemCursor.beginFrame(ctx);
        try {
            renderInternal(ctx, screenW, baseX, baseY);
        } finally {
            SystemCursor.endFrame();
        }
    }

    private static void renderInternal(GuiGraphicsExtractor ctx, int screenW, float baseX, float baseY) {
        if (!BetterChatLayoutEngine.prepareFrame(ctx, screenW, baseX, baseY)) return;
        BetterChatViewport.composeFrame();

        BetterChatRenderer.searchHit = false;
        if (!BetterChatRenderer.ctxChatOpen) {
            BetterChatSearch.deactivate();
        }
        BetterChatSuggestionState.updateSearchHotkey(BetterChatRenderer.ctxMc, BetterChatRenderer.ctxChatOpen);

        BetterChatModel.MousePos mouse = BetterChatInputHandler.resolveMouse(
                BetterChatRenderer.ctxMc.mouseHandler.xpos(), BetterChatRenderer.ctxMc.mouseHandler.ypos());
        BetterChatRenderer.lastMouseFx = mouse.fx();
        BetterChatRenderer.lastMouseFy = mouse.fy();
        BetterChatRenderer.lastMouseValid = true;
        BetterChatRenderer.hoverEntityUuid = null;
        BetterChatRenderer.ctxHoverTip = null;

        BetterChatModel.PickResult hover = BetterChatMessageRenderer.drawPhase();
        BetterChatTooltipRenderer.resolveHover(hover, BetterChatRenderer.ctxMc);

        if (BetterChatRenderer.ctxChatOpen) {
            BetterChatInputRenderer.renderInput(BetterChatRenderer.ctxMc, BetterChatRenderer.lastFontSizeForUi, BetterChatRenderer.ctxWidth, BetterChatRenderer.ctxBaseX, BetterChatRenderer.ctxFixedInputY);
            BetterChatInputHandler.updateChatHit();
            BetterChatSuggestionRenderer.renderSuggestions();
        } else {
            BetterChatInputHandler.clearChatHit();
        }

        if (BetterChatRenderer.ctxChatOpen && BetterChatRenderer.maxScrollLines > 0) {
            int scrollbarStart = BetterChatRenderer.ctxAutoFollow
                    ? Math.max(0, BetterChatRenderer.sbTotalLines - BetterChatRenderer.ctxVisibleLimit)
                    : Math.max(0, BetterChatRenderer.sbTotalLines - BetterChatRenderer.ctxVisibleLimit - BetterChatRenderer.scrollOffsetLines);
            BetterChatDrawUtil.renderScrollbar(BetterChatRenderer.frame.x(), BetterChatRenderer.frame.y(), BetterChatRenderer.frame.w(), BetterChatRenderer.frame.h(), scrollbarStart, BetterChatRenderer.ctxVisibleLimit, BetterChatRenderer.sbTotalLines, BetterChatRenderer.lastMouseFx, BetterChatRenderer.lastMouseFy);
        }

        if (BetterChatRenderer.ctxChatOpen && BetterChatRenderer.ctxHoverTip != null) {
            BetterChatTooltipRenderer.captureTooltip(BetterChatRenderer.ctxHoverTip, (float) BetterChatRenderer.lastMouseFx + BetterChatRenderer.CHAT_TOOLTIP_OFFSET_X, (float) BetterChatRenderer.lastMouseFy + BetterChatRenderer.CHAT_TOOLTIP_OFFSET_Y, ctx);
        }

        if (BetterChatRenderer.contextMenu.open()) {
            BetterChatRenderer.contextMenu.render((float) BetterChatRenderer.lastMouseFx, (float) BetterChatRenderer.lastMouseFy, BetterChatRenderer.lastFontSizeForUi);
        }
    }
}