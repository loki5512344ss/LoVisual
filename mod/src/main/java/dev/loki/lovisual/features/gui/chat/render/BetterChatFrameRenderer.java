package dev.loki.lovisual.features.gui.chat.render;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.features.gui.chat.BetterChatViewport;
import dev.loki.lovisual.features.gui.chat.input.BetterChatInputHandler;
import dev.loki.lovisual.features.gui.chat.input.BetterChatInputRenderer;
import dev.loki.lovisual.features.gui.chat.menu.BetterChatTooltipRenderer;
import dev.loki.lovisual.features.gui.chat.messages.BetterChatMessageRenderer;
import dev.loki.lovisual.features.gui.chat.messages.BetterChatSearch;
import dev.loki.lovisual.features.gui.chat.model.BetterChatModel;
import dev.loki.lovisual.features.gui.chat.suggestions.BetterChatSuggestionRenderer;
import dev.loki.lovisual.features.gui.chat.suggestions.BetterChatSuggestionState;
import dev.loki.lovisual.features.gui.chat.text.BetterChatLayoutEngine;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;

/**
 * Single entry point for the chat HUD render pass; orchestrates the layout, message, input,
 * suggestion, scrollbar and tooltip phases. All real work lives in the other package-private helpers.
 */
public final class BetterChatFrameRenderer {
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
