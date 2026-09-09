/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.chat.messages;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.ClipFunction;
import net.minecraft.util.Mth;
import java.util.List;
import static dev.loki.lovisual.render.engine.text.TextGlyphFallback.isSvgFontKey;
import static dev.loki.lovisual.features.theme.Theme.theme;
import static dev.loki.lovisual.features.gui.chat.text.util.ChatHoverUtil.HoverTip;
import dev.loki.lovisual.features.gui.chat.menu.BetterChatTooltipRenderer;
import dev.loki.lovisual.features.gui.chat.model.BetterChatModel;
import dev.loki.lovisual.features.gui.chat.model.ChatLine;
import dev.loki.lovisual.features.gui.chat.render.BetterChatFrameModel;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.text.BetterChatGlyphRenderer;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatFontUtil;
/**
 * Message-area drawing: glass bubbles, selection rectangles, timestamp overlays and glyph pickup.
 * Extracted from {@link BetterChatRenderer}; behavior unchanged.
 */
public final class BetterChatMessageRenderer {
    private BetterChatMessageRenderer() {
    }
    public static float fade(float ageSeconds) {
        BetterChat cfg = BetterChat.get();
        float life = cfg != null ? cfg.fadeSeconds() : 12f;
        if (life <= 0f) return 0f;
        float fadeDur = Math.min(2.0f, Math.max(0.35f, life * 0.18f));
        float holdDur = Math.max(0f, life - fadeDur);
        if (ageSeconds <= holdDur) return 1f;
        float t = (ageSeconds - holdDur) / fadeDur;
        t = Mth.clamp(t, 0f, 1f);
        float inv = 1f - t;
        return inv * inv * inv;
    }
    public static float newMessageReveal(ChatLine message) {
        if (message == null) return 1f;
        float age = message.ageSeconds();
        if (age <= 0f) return 0f;
        if (age >= BetterChatRenderer.NEW_MESSAGE_REVEAL_SECONDS) return 1f;
        float t = Mth.clamp(age / BetterChatRenderer.NEW_MESSAGE_REVEAL_SECONDS, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
    public static BetterChatModel.PickResult drawPhase() {
        BetterChatFrameModel.FrameLayout frame = BetterChatRenderer.frame;
        boolean activeChatSurface = BetterChatRenderer.ctxActiveSurface;
        boolean showTs = BetterChatRenderer.ctxShowTs;
        TextRenderer tsRenderer = showTs ? BetterChatFontUtil.getInterRegular() : null;
        float tsFontSize = Math.max(9f, BetterChatRenderer.lastFontSizeForUi * 0.72f);
        float tsTextHeight = 0f;
        if (showTs && tsRenderer != null) {
            float tsScale = BetterChatFontUtil.scaleForSize(tsFontSize);
            tsRenderer.begin(tsScale, false, false);
            tsTextHeight = (float) tsRenderer.getHeight(false);
            tsRenderer.end();
        }
        boolean chatClip = false;
        BetterChatModel.MessageClipBounds clipBounds = frame.messageClipBounds();
        if (clipBounds != null) {
            if (!frame.bubbles().isEmpty()) {
                Renderer2D.requestLiquidGlassBlurBeforeNextShapeClip();
            }
            float clipRadius = Math.min(BetterChatRenderer.RADIUS, clipBounds.h() * 0.5f);
            chatClip = ClipFunction.pushRoundedRect(
                    clipBounds.x(), clipBounds.y(), clipBounds.w(), clipBounds.h(),
                    clipRadius, clipRadius, 0.0f, 0.0f
            );
        }
        drawMessageBubbles(frame.bubbles(), activeChatSurface);
        for (BetterChatModel.FrameLine line : frame.lines()) {
            float alpha = activeChatSurface ? 1f : fade(line.message().ageSeconds());
            if (alpha <= 0.01f) continue;
            drawSelection(line, alpha, BetterChatRenderer.ctxLineHeight);
        }
        BetterChatGlyphRenderer.renderGlyphs(frame.lines(), BetterChatRenderer.lastFontSizeForUi, BetterChatRenderer.ctxLineHeight, activeChatSurface);
        if (showTs && tsRenderer != null) {
            BetterChatRenderer.ctxHoverTip = renderBubbleTimestamps(
                    tsRenderer,
                    frame.bubbles(),
                    tsFontSize,
                    tsTextHeight,
                    BetterChatRenderer.ctxHasSeconds,
                    BetterChatRenderer.ctxTsColor,
                    BetterChatRenderer.ctxTipDate,
                    BetterChatRenderer.ctxTipUnix,
                    activeChatSurface,
                    BetterChatRenderer.ctxChatOpen,
                    BetterChatRenderer.ctxHoverTip
            );
        }
        if (chatClip) ClipFunction.pop();
        return frame.pick(BetterChatRenderer.lastMouseFx, BetterChatRenderer.lastMouseFy);
    }
    private static void drawMessageBubbles(List<BetterChatModel.MessageBubble> bubbles, boolean activeChatSurface) {
        if (bubbles == null || bubbles.isEmpty()) return;
        for (BetterChatModel.MessageBubble bubble : bubbles) {
            float alpha = activeChatSurface ? 1f : fade(bubble.message().ageSeconds());
            if (alpha <= 0.01f) continue;
            float reveal = newMessageReveal(bubble.message());
            float x = bubble.x() - (1f - reveal) * BetterChatRenderer.NEW_MESSAGE_SLIDE_PX;
            float y = bubble.y();
            float w = bubble.w();
            float h = bubble.h();
            drawMessageBubbleGlass(x, y, w, h, Math.min(BetterChatRenderer.MESSAGE_RADIUS, h * 0.5f), alpha);
        }
    }
    private static void drawMessageBubbleGlass(float x, float y, float w, float h, float radius, float alpha) {
        if (BetterChatRenderer.renderer == null || w <= 0f || h <= 0f) return;
        float drawAlpha = Mth.clamp(alpha, 0.0f, 1.0f);
        if (drawAlpha <= 0.001f) return;
        BetterChat settings = BetterChat.get();
        float configuredAlpha = settings != null ? settings.liquidGlassAlphaFactor() : (230f / 255f);
        float blurStrength = configuredAlpha * drawAlpha;
        float glassScale = BetterChatRenderer.MESSAGE_RADIUS <= 0.0f ? 1.0f : radius / BetterChatRenderer.MESSAGE_RADIUS;
        HudRenderUtil.drawLiquidGlassCorners(
                x, y, w, h,
                radius, radius, radius, radius,
                glassScale, false,
                blurStrength, drawAlpha * 0.74f,
                theme().accent()
        );
        BetterChatDrawUtil.drawRoundedRect(x, y, w, h, radius,
                BetterChatDrawUtil.withAlpha(theme().windowBg(), Math.round(0x08 * drawAlpha)));
    }
    static HoverTip renderBubbleTimestamps(TextRenderer tsRenderer,
                                           List<BetterChatModel.MessageBubble> bubbles,
                                           float tsFontSize,
                                           float tsTextHeight,
                                           boolean withSeconds,
                                           int tsColor,
                                           boolean tsTipDate,
                                           boolean tsTipUnix,
                                           boolean activeChatSurface,
                                           boolean chatOpen,
                                           HoverTip currentHover) {
        if (bubbles == null || bubbles.isEmpty()) return currentHover;
        HoverTip hover = currentHover;
        float tsScale = BetterChatFontUtil.scaleForSize(tsFontSize);
        float shadowOffset = Math.max(0.75f, tsFontSize * 0.07f);
        tsRenderer.begin(tsScale, false, false);
        try {
            for (BetterChatModel.MessageBubble bubble : bubbles) {
                float alpha = activeChatSurface ? 1f : fade(bubble.message().ageSeconds());
                if (alpha <= 0.01f) continue;
                String ts = BetterChatTooltipRenderer.formatTimestamp(bubble.message().timestampMs(), withSeconds);
                float textW = (float) tsRenderer.getWidth(ts, false);
                float reveal = newMessageReveal(bubble.message());
                float tx = bubble.x() + bubble.w() - BetterChatRenderer.MESSAGE_PAD_X - textW - (1f - reveal) * BetterChatRenderer.NEW_MESSAGE_SLIDE_PX;
                float ty = bubble.lastLine().y0() + (bubble.lastLine().y1() - bubble.lastLine().y0() - tsTextHeight) * 0.5f;
                int shadowColor = BetterChatDrawUtil.mulAlpha(0xFF000000, alpha * 0.46f);
                BetterChatDrawUtil.applyColor(shadowColor);
                tsRenderer.render(ts, tx + shadowOffset, ty, BetterChatRenderer.TMP_COLOR, false);
                tsRenderer.render(ts, tx, ty + shadowOffset, BetterChatRenderer.TMP_COLOR, false);
                BetterChatDrawUtil.applyColor(BetterChatDrawUtil.mulAlpha(tsColor, alpha * 0.82f));
                tsRenderer.render(ts, tx, ty, BetterChatRenderer.TMP_COLOR, false);
                if (hover == null && chatOpen && BetterChatRenderer.lastMouseValid) {
                    float hitPad = 2f;
                    if (BetterChatRenderer.lastMouseFx >= tx - hitPad && BetterChatRenderer.lastMouseFx <= tx + textW + hitPad
                            && BetterChatRenderer.lastMouseFy >= ty - hitPad && BetterChatRenderer.lastMouseFy <= ty + tsTextHeight + hitPad) {
                        hover = BetterChatTooltipRenderer.buildTimestampTooltip(bubble.message().timestampMs(), tsTipDate, tsTipUnix);
                    }
                }
            }
        } finally {
            tsRenderer.end();
        }
        return hover;
    }
    private static void drawSelection(BetterChatModel.FrameLine line, float alpha, float lineHeight) {
        if (!BetterChatRenderer.selection.appliesToLine(line.messageIndex())) return;
        int selStart = BetterChatRenderer.selection.startForLine(line.messageIndex());
        int selEnd = BetterChatRenderer.selection.endForLine(line.messageIndex());
        if (selStart < 0 && selEnd < 0) return;
        float y0 = line.y0() - 1.5f;
        float h = lineHeight + 3f;
        float startX = Float.NaN;
        float endX = Float.NaN;
        for (BetterChatModel.GlyphBox g : line.glyphs()) {
            if (g.charEndExclusive() <= selStart || g.charIndex() > selEnd) continue;
            if (Float.isNaN(startX)) startX = g.x0();
            endX = g.x1();
        }
        if (Float.isNaN(startX) || Float.isNaN(endX)) return;
        float w = Math.max(1f, endX - startX);
        BetterChatDrawUtil.drawRoundedRect(startX, y0, w, h, 1.8f, BetterChatDrawUtil.mulAlpha(theme().accent(), alpha * 0.55f));
    }
    static boolean isSvgGlyphKey(String fontKey) {
        return isSvgFontKey(fontKey);
    }
}