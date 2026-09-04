/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.chat;
import dev.loki.lovisual.features.command.CommandOutput;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.BetterTooltips;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.Collections;
import java.util.List;
final class BetterChatLayoutEngine {
    private BetterChatLayoutEngine() {
    }
    static void markLayoutDirty() {
    }
    static BetterChatModel.LayoutResult layoutLinesTailCached(List<ChatLine> messages, int[] messageGroups, float fontSize, float maxWidth, int maxLines) {
        if (messages.isEmpty()) return new BetterChatModel.LayoutResult(Collections.emptyList(), 0);
        List<BetterChatModel.VisualLine> lines = new java.util.ArrayList<>();
        int seenMessages = 0;
        for (int mi = messages.size() - 1; mi >= 0; mi--) {
            ChatLine msg = messages.get(mi);
            int messageGroup = messageGroups != null && mi >= 0 && mi < messageGroups.length ? messageGroups[mi] : mi;
            List<BetterChatModel.CachedLine> cached = BetterChatLineLayout.cachedMessageLines(msg, fontSize, maxWidth);
            for (int j = cached.size() - 1; j >= 0; j--) {
                BetterChatModel.CachedLine cl = cached.get(j);
                lines.addFirst(new BetterChatModel.VisualLine(msg, mi, messageGroup, cl.startChar(), cl.endChar(), cl.glyphs()));
            }
            seenMessages++;
            if (lines.size() >= maxLines) break;
        }
        return new BetterChatModel.LayoutResult(lines, seenMessages);
    }
    static float computeWidth(int screenW, float ratio) {
        float clamped = Mth.clamp(ratio, 0.2f, 0.8f);
        float w = screenW * clamped;
        return Mth.clamp(w, 320f, 560f);
    }
    static BetterChatStore buildPreviewStore() {
        if (BetterChatRenderer.previewStore == null) {
            BetterChatStore store = new BetterChatStore();
            store.add(Component.literal("Better Chat"));
            store.add(Component.literal("Drag to position"));
            store.add(Component.literal("Messages appear here"));
            BetterChatRenderer.previewStore = store;
        }
        return BetterChatRenderer.previewStore;
    }
    /**
     * @return false when there is nothing to render this frame (caller must bail before draw phase).
     */
    static boolean prepareFrame(GuiGraphicsExtractor ctx, int screenW, float baseX, float baseY) {
        BetterChatRenderer.ctxGui = ctx;
        BetterChatRenderer.ctxBaseX = baseX;
        BetterChatRenderer.ctxBaseY = baseY;
        BetterChat settings = BetterChat.get();
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        if (settings == null || (!settings.isEnabled() && !preview)) {
            BetterChatRenderer.lastRenderWidth = 0f;
            BetterChatRenderer.lastRenderHeight = 0f;
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || (mc.player == null && !preview)) {
            BetterChatRenderer.lastRenderWidth = 0f;
            BetterChatRenderer.lastRenderHeight = 0f;
            return false;
        }
        if (!preview && mc.isPaused()) {
            BetterChatRenderer.selection.clear();
            BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
            BetterChatRenderer.selecting = false;
            BetterChatRenderer.lastRenderWidth = 0f;
            BetterChatRenderer.lastRenderHeight = 0f;
            return false;
        }
        BetterChatStore store = (preview && mc.player == null)
                ? buildPreviewStore()
                : BetterChatStoreManager.getActiveStore(mc);
        if (store == null) {
            if (preview) {
                store = buildPreviewStore();
            } else {
                BetterChatRenderer.lastRenderWidth = 0f;
                BetterChatRenderer.lastRenderHeight = 0f;
                return false;
            }
        }
        boolean chatOpen = ClientScreen.current() instanceof ChatScreen;
        boolean activeChatSurface = chatOpen || preview;
        if (!chatOpen && store.size() == 0) {
            if (preview) {
                store = buildPreviewStore();
            } else {
                BetterChatRenderer.selection.clear();
                BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
                BetterChatRenderer.lastRenderWidth = 0f;
                BetterChatRenderer.lastRenderHeight = 0f;
                return false;
            }
        }
        if (!chatOpen) {
            BetterChatRenderer.selection.clear();
            BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
            BetterChatRenderer.selecting = false;
            BetterChatRenderer.storedScreen = null;
            BetterChatRenderer.draggingScrollbar = false;
            BetterChatRenderer.sbEnabled = false;
            BetterChatRenderer.scrollOffsetLines = 0;
            BetterChatSearch.deactivate();
            BetterChatSuggestionState.resetSuggestionTracking();
        }
        if (chatOpen && ctx != null) {
            BetterTooltips.beginTooltipFrame();
            BetterTooltips.setDrawContext(ctx);
        }
        BetterChatRenderer.tooltipCaptured = false;
        int visibleLimit = Math.max(1, chatOpen ? settings.maxLines() : Math.min(settings.maxLines(), 12));
        List<CommandOutput.MessageLine> commandOutputMessages = settings.hideVanilla()
                ? CommandOutput.recentMessages()
                : List.of();
        int totalMessages = store.size() + commandOutputMessages.size();
        boolean autoFollow = BetterChatRenderer.scrollOffsetLines == 0;
        boolean tsEnabled = settings.timestampEnabled();
        boolean tsSeconds = settings.timestampSeconds();
        int tsColor = settings.timestampColor();
        boolean tsActiveOnly = settings.timestampToggles().get("active_only");
        boolean tsTipDate = settings.timestampToggles().get("hover_date");
        boolean tsTipUnix = settings.timestampToggles().get("hover_unix");
        boolean showTs = tsEnabled && (!tsActiveOnly || chatOpen);
        List<ChatLine> messages;
        if (BetterChatSearch.hasQuery() && BetterChatSearch.results() != null && chatOpen) {
            List<ChatLine> res = BetterChatSearch.results();
            totalMessages = res.size();
            int maxScrollSearch = Math.max(0, totalMessages - visibleLimit);
            BetterChatRenderer.scrollOffsetLines = Mth.clamp(BetterChatRenderer.scrollOffsetLines, 0, maxScrollSearch);
            int tailBudget = Math.min(totalMessages, Math.max(visibleLimit + BetterChatRenderer.scrollOffsetLines + 20, visibleLimit + 10));
            int endIdx = Math.min(totalMessages, Math.max(0, totalMessages - BetterChatRenderer.scrollOffsetLines));
            int startIdx = Math.max(0, endIdx - tailBudget);
            if (startIdx > endIdx) startIdx = endIdx;
            messages = res.subList(startIdx, endIdx);
        } else {
            int tailBudget = chatOpen
                    ? Math.max(visibleLimit + BetterChatRenderer.scrollOffsetLines + 80, visibleLimit)
                    : Math.max(visibleLimit + 8, visibleLimit);
            int needed = Math.min(BetterChatRenderer.MAX_HISTORY, Math.min(totalMessages, tailBudget));
            messages = BetterChatCommandMerge.mergeCommandOutput(store.tail(Math.min(store.size(), needed)), commandOutputMessages, needed);
        }
        BetterChatRenderer.lastMessages = messages;
        int[] messageGroups = BetterChatMessageGrouping.groupIds(messages);
        float fontSize = 16f * settings.fontScale();
        float width = computeWidth(screenW, settings.widthRatio());
        float tsWidth = 0f;
        if (showTs) {
            String sampleTs = tsSeconds ? "00:00:00" : "00:00";
            tsWidth = BetterChatFontUtil.textWidth(BetterChatFontUtil.getInterRegular(), sampleTs, Math.max(9f, fontSize * 0.72f)) + BetterChatRenderer.MESSAGE_TS_GAP;
        }
        float contentWidth = Math.max(32f, width - BetterChatRenderer.PADDING * 2f - BetterChatRenderer.MESSAGE_PAD_X * 2f - tsWidth);
        BetterChatRenderer.lastFontSizeForUi = fontSize;
        int lineTarget = visibleLimit + BetterChatRenderer.scrollOffsetLines + Math.max(10, visibleLimit / 2) + 2;
        long perfT0 = System.nanoTime();
        BetterChatModel.LayoutResult layoutResult = layoutLinesTailCached(messages, messageGroups, fontSize, contentWidth, lineTarget);
        long perfT1 = System.nanoTime();
        BetterChatRenderer.ctxSettings = settings;
        BetterChatRenderer.ctxMc = mc;
        BetterChatRenderer.ctxPreview = preview;
        BetterChatRenderer.ctxChatOpen = chatOpen;
        BetterChatRenderer.ctxActiveSurface = activeChatSurface;
        BetterChatRenderer.ctxShowTs = showTs;
        BetterChatRenderer.ctxHasSeconds = tsSeconds;
        BetterChatRenderer.ctxTsColor = tsColor;
        BetterChatRenderer.ctxTipDate = tsTipDate;
        BetterChatRenderer.ctxTipUnix = tsTipUnix;
        BetterChatRenderer.ctxAutoFollow = autoFollow;
        BetterChatRenderer.ctxWidth = width;
        BetterChatRenderer.ctxContentWidth = contentWidth;
        BetterChatRenderer.ctxTsWidth = tsWidth;
        BetterChatRenderer.ctxTotalMessages = totalMessages;
        BetterChatRenderer.ctxVisibleLimit = visibleLimit;
        BetterChatRenderer.ctxMessageGroups = messageGroups;
        BetterChatRenderer.ctxAllLines = layoutResult.lines();
        BetterChatRenderer.ctxMessagesSeen = layoutResult.messagesSeen();
        BetterChatRenderer.ctxPerfT0 = perfT0;
        BetterChatRenderer.ctxPerfT1 = perfT1;
        BetterChatRenderer.hasInputBox = false;
        return true;
    }
}