/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.BetterTooltips;
import dev.loki.lovisual.util.text.ChatNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.loki.lovisual.features.gui.chat.ChatHoverUtil.HoverTip;
import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Tooltip capture/render, timestamp formatting and post-pick hover inference.
 */
final class BetterChatTooltipRenderer {
    private BetterChatTooltipRenderer() {
    }

    static String formatTimestamp(long ms, boolean withSeconds) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(withSeconds ? "HH:mm:ss" : "HH:mm");
        return dt.format(fmt);
    }

    static String formatDate(long ms) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    static HoverTip buildTimestampTooltip(long ms, boolean showDate, boolean showUnix) {
        List<ChatHoverUtil.ColoredLine> lines = new ArrayList<>();
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        if (showDate) {
            lines.add(new ChatHoverUtil.ColoredLine(net.minecraft.client.resources.language.I18n.get(
                    "better_chat.hover.date",
                    dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ), 0));
        }
        if (showUnix) {
            lines.add(new ChatHoverUtil.ColoredLine("Unix: " + (ms / 1000L), 0xFF888888));
        }
        if (lines.isEmpty()) return null;
        return new HoverTip(lines, null, false, net.minecraft.world.item.ItemStack.EMPTY);
    }

    static void captureTooltip(HoverTip tip, float x, float y, GuiGraphicsExtractor ctx) {
        if (tip == null || ctx == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        float scale = (float) mc.getWindow().getGuiScale();
        int mx = Math.round(x / scale);
        int my = Math.round(y / scale);
        List<FormattedCharSequence> lines = buildOrderedTooltipLines(tip);
        if (lines.isEmpty()) return;

        BetterTooltips.setTooltipScaleOverride(0.8f);
        if (tip.item() != null && !tip.item().isEmpty()) {
            BetterTooltips.captureItemTooltipOrdered(lines, DefaultTooltipPositioner.INSTANCE, mx, my, tip.item(), ctx);
        } else {
            BetterTooltips.captureTooltipOrdered(lines, DefaultTooltipPositioner.INSTANCE, mx, my);
        }
        BetterChatRenderer.tooltipCaptured = true;
    }

    static void renderCapturedTooltip(GuiGraphicsExtractor ctx) {
        if (!BetterChatRenderer.tooltipCaptured) return;
        BetterTooltips.renderTooltipWithContext(ctx);
        BetterTooltips.resetTooltipScaleOverride();
        BetterChatRenderer.tooltipCaptured = false;
    }

    private static List<FormattedCharSequence> buildOrderedTooltipLines(HoverTip tip) {
        if (tip == null || tip.lines() == null || tip.lines().isEmpty()) return Collections.emptyList();
        List<FormattedCharSequence> out = new ArrayList<>(tip.lines().size());
        for (ChatHoverUtil.ColoredLine line : tip.lines()) {
            int argb = line.color() != 0 ? line.color() : theme().textPrimary();
            int rgb = argb & 0x00FFFFFF;
            Component text = Component.literal(line.text()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
            out.add(text.getVisualOrderText());
        }
        return out;
    }

    /**
     * Mirror of the original post-pick hover/suggest-inference block (lines 466-518).
     */
    static void resolveHover(BetterChatModel.PickResult hover, Minecraft mc) {
        boolean overSuggestWindow = BetterChatSuggestionState.isInsideSuggestWindow(
                BetterChatRenderer.lastMouseFx, BetterChatRenderer.lastMouseFy,
                BetterChatRenderer.lastMouseFx, BetterChatRenderer.lastMouseFy);
        if (overSuggestWindow && BetterChatRenderer.lastHoverWasOutsideSuggest) {
            BetterChatRenderer.lastHoverWasOutsideSuggest = false;
        } else if (!overSuggestWindow && !BetterChatRenderer.lastHoverWasOutsideSuggest) {
            BetterChatRenderer.lastHoverWasOutsideSuggest = true;
        }

        HoverTip hoverTip = BetterChatRenderer.ctxHoverTip;
        if (hover != null && hover.glyph().hover() != null && !overSuggestWindow) {
            hoverTip = ChatHoverUtil.fromHover(hover.glyph().hover(), mc);
            if (hoverTip != null) BetterChatRenderer.hoverEntityUuid = hoverTip.uuid();
        }

        if (BetterChatRenderer.selecting) {
            hoverTip = null;
            BetterChatRenderer.hoverEntityUuid = null;
        }

        if (!BetterChatRenderer.selecting && !overSuggestWindow && hoverTip == null && hover != null) {
            String raw = hover.line().message().text().getString();
            String word = BetterChatEditBoxUtil.wordAt(raw, hover.glyph().charIndex());
            if (!word.isEmpty()) {
                boolean looksLikeItem = (word.startsWith("[") && word.contains("]"));
                HoverTip cached = ChatHoverUtil.inferFromDisplay(word, looksLikeItem, mc);
                if (cached != null) {
                    hoverTip = cached;
                    BetterChatRenderer.hoverEntityUuid = cached.uuid();
                }
            }
            if (hoverTip == null) {
                List<String> nickCandidates = ChatNameUtil.extractNicks(raw);
                String normalized = ChatNameUtil.normalizeNickCandidate(word);
                String targetNick = null;
                if (!normalized.isEmpty()
                        && ChatNameUtil.isNickLike(normalized)
                        && nickCandidates.stream().anyMatch(n -> n.equalsIgnoreCase(normalized))) {
                    targetNick = normalized;
                } else if (nickCandidates.size() == 1) {
                    targetNick = nickCandidates.getFirst();
                }
                if (targetNick != null) {
                    hoverTip = ChatHoverUtil.inferFromNick(targetNick, mc);
                    if (hoverTip != null) BetterChatRenderer.hoverEntityUuid = hoverTip.uuid();
                }
            }
        }

        if (overSuggestWindow || BetterChatRenderer.contextMenu.open()) {
            hoverTip = null;
            BetterChatRenderer.hoverEntityUuid = null;
        }

        BetterChatRenderer.ctxHoverTip = hoverTip;
    }
}