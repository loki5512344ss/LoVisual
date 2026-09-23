package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.player.PlayerHeadRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class TabListTextRenderer {
    private static final float HEAD_SIZE = 30f;
    private static final float ROW_PAD_X = 10.5f;
    private static final float SHELL_PAD_X = 14f;
    private static final float TEXT_SIZE = 24.5f;
    private static final float PING_TEXT_SIZE = 17.5f;
    private static final float SCORE_TEXT_SIZE = 20.5f;
    private static final float HEADER_TEXT_SIZE = 24f;
    private static final float FOOTER_TEXT_SIZE = 20.5f;

    private TabListTextRenderer() {}

    static void renderHeadsAndText(GuiGraphicsExtractor ctx, Renderer2D renderer, float baseX, float baseY, float drawWidth, float panelAlpha, TabListModel.Snapshot snap, TabListAnimator animator, TabListPalette p) {
        float textH = TabRichTextRenderer.height(TEXT_SIZE);
        renderHeaderText(renderer, baseX, baseY, drawWidth, panelAlpha, snap);
        renderHeads(ctx, baseX, baseY, drawWidth, panelAlpha, snap, animator);
        renderPingTexts(renderer, baseX, baseY, drawWidth, panelAlpha, snap, animator, p);
        renderScoreTexts(renderer, baseX, baseY, drawWidth, panelAlpha, snap, animator, p);
        renderNameTexts(renderer, baseX, baseY, drawWidth, panelAlpha, snap, animator, p, textH);
        renderFooterText(renderer, baseX, baseY, drawWidth, panelAlpha, snap);
    }

    private static void renderHeads(GuiGraphicsExtractor ctx, float baseX, float baseY, float drawWidth, float panelAlpha, TabListModel.Snapshot snap, TabListAnimator animator) {
        float contentX = contentX(baseX, drawWidth, snap);
        float rowTop = snap.rowTop();
        for (int i = 0; i < snap.entries().size(); i++) {
            TabListModel.Entry entry = snap.entries().get(i);
            TabListAnimator.RowState state = animator.row(entry.id());
            float rowAlpha = panelAlpha * AnimationUtility.easeOutCubic(state.presence);
            if (rowAlpha <= 0.01f) continue;
            int col = i / Math.max(1, snap.rows());
            int row = i % Math.max(1, snap.rows());
            float rowX = contentX + col * (snap.columnWidth() + snap.columnGap());
            float rowY = baseY + rowTop + row * snap.rowHeight() + (1f - state.presence) * 10.0f;
            float rowH = snap.rowHeight() - 5f;
            float headX = rowX + ROW_PAD_X;
            float headY = rowY + (rowH - HEAD_SIZE) * 0.5f;
            int alpha = Math.max(0, Math.min(255, Math.round(255f * rowAlpha)));
            if (entry.skin() != null) {
                PlayerHeadRenderer.drawRounded(ctx, headX, headY, HEAD_SIZE, 5.25f, entry.skin(), new RenderColor(255, 255, 255, alpha), true, new RenderColor(255, 255, 255, Math.round(42f * rowAlpha)), 1.2f, false);
            }
        }
    }

    private static void renderPingTexts(Renderer2D renderer, float baseX, float baseY, float drawWidth, float panelAlpha, TabListModel.Snapshot snap, TabListAnimator animator, TabListPalette p) {
        float contentX = contentX(baseX, drawWidth, snap);
        float pingTextH = TabRichTextRenderer.height(PING_TEXT_SIZE);
        float rowTop = snap.rowTop();
        for (int i = 0; i < snap.entries().size(); i++) {
            TabListModel.Entry entry = snap.entries().get(i);
            TabListAnimator.RowState state = animator.row(entry.id());
            float rowAlpha = panelAlpha * AnimationUtility.easeOutCubic(state.presence);
            if (rowAlpha <= 0.01f) continue;
            int col = i / Math.max(1, snap.rows());
            int row = i % Math.max(1, snap.rows());
            float rowX = contentX + col * (snap.columnWidth() + snap.columnGap());
            float rowY = baseY + rowTop + row * snap.rowHeight() + (1f - state.presence) * 10.0f;
            float rowH = snap.rowHeight() - 5f;
            float right = rowX + snap.columnWidth() - 10.5f;
            float pingChipW = entry.pingChipWidth();
            float pingChipH = 22.5f;
            float pingChipX = right - pingChipW;
            float pingChipY = rowY + (rowH - pingChipH) * 0.5f;
            TabRichTextRenderer.drawPlain(renderer, entry.pingText(), pingChipX + 19.5f, pingChipY + (pingChipH - pingTextH) * 0.5f - 0.25f, pingChipW - 22f, PING_TEXT_SIZE, p.text, rowAlpha);
        }
    }

    private static void renderScoreTexts(Renderer2D renderer, float baseX, float baseY, float drawWidth, float panelAlpha, TabListModel.Snapshot snap, TabListAnimator animator, TabListPalette p) {
        float contentX = contentX(baseX, drawWidth, snap);
        float scoreTextH = TabRichTextRenderer.height(SCORE_TEXT_SIZE);
        float rowTop = snap.rowTop();
        for (int i = 0; i < snap.entries().size(); i++) {
            TabListModel.Entry entry = snap.entries().get(i);
            if (entry.scoreText() == null) continue;
            TabListAnimator.RowState state = animator.row(entry.id());
            float rowAlpha = panelAlpha * AnimationUtility.easeOutCubic(state.presence);
            if (rowAlpha <= 0.01f) continue;
            int col = i / Math.max(1, snap.rows());
            int row = i % Math.max(1, snap.rows());
            float rowX = contentX + col * (snap.columnWidth() + snap.columnGap());
            float rowY = baseY + rowTop + row * snap.rowHeight() + (1f - state.presence) * 10.0f;
            float rowH = snap.rowHeight() - 5f;
            float right = rowX + snap.columnWidth() - 10.5f;
            float occupiedLeft = right - entry.pingChipWidth() - 10f;
            float scoreBoxW = entry.scoreBoxWidth();
            float scoreBoxH = 23f;
            float scoreX = occupiedLeft - scoreBoxW - 8f;
            float scoreY = rowY + (rowH - scoreBoxH) * 0.5f;
            TabRichTextRenderer.draw(renderer, entry.scoreText(), scoreX + 8f, scoreY + (scoreBoxH - scoreTextH) * 0.5f - 0.5f, entry.scoreWidth() + 2f, SCORE_TEXT_SIZE, p.text, rowAlpha);
        }
    }

    private static void renderNameTexts(Renderer2D renderer, float baseX, float baseY, float drawWidth, float panelAlpha, TabListModel.Snapshot snap, TabListAnimator animator, TabListPalette p, float textH) {
        float contentX = contentX(baseX, drawWidth, snap);
        float rowTop = snap.rowTop();
        for (int i = 0; i < snap.entries().size(); i++) {
            TabListModel.Entry entry = snap.entries().get(i);
            TabListAnimator.RowState state = animator.row(entry.id());
            float rowAlpha = panelAlpha * AnimationUtility.easeOutCubic(state.presence);
            if (rowAlpha <= 0.01f) continue;
            int col = i / Math.max(1, snap.rows());
            int row = i % Math.max(1, snap.rows());
            float rowX = contentX + col * (snap.columnWidth() + snap.columnGap());
            float rowY = baseY + rowTop + row * snap.rowHeight() + (1f - state.presence) * 10.0f;
            float rowH = snap.rowHeight() - 5f;
            float right = rowX + snap.columnWidth() - 10.5f;
            float pingChipX = right - entry.pingChipWidth();
            float occupiedLeft = pingChipX - 10f;
            if (entry.scoreText() != null) occupiedLeft = occupiedLeft - entry.scoreBoxWidth() - 18f;
            float textX = rowX + ROW_PAD_X + HEAD_SIZE + 22f;
            float textY = rowY + (rowH - textH) * 0.5f - 1.0f;
            float textW = Math.max(40f, occupiedLeft - textX);
            int baseColor = entry.spectator() ? p.muted : p.text;
            TabRichTextRenderer.draw(renderer, entry.displayName(), textX, textY, textW, TEXT_SIZE, baseColor, rowAlpha);
        }
    }

    private static void renderHeaderText(Renderer2D renderer, float baseX, float baseY, float drawWidth, float alpha, TabListModel.Snapshot snap) {
        if (snap.headerLines().isEmpty() || alpha <= 0.01f) return;
        float lineH = 29f;
        float y = baseY + snap.headerTop() + 8f + (lineH - TabRichTextRenderer.height(HEADER_TEXT_SIZE)) * 0.5f;
        for (Component line : snap.headerLines()) {
            drawCentered(renderer, line, baseX + drawWidth * 0.5f, y, drawWidth - 56f, HEADER_TEXT_SIZE, 0xFFFFFFFF, alpha);
            y += lineH;
        }
    }

    private static void renderFooterText(Renderer2D renderer, float baseX, float baseY, float drawWidth, float alpha, TabListModel.Snapshot snap) {
        if (snap.footerLines().isEmpty() || alpha <= 0.01f) return;
        float lineH = 25f;
        float y = baseY + snap.footerTop() + 7f + (lineH - TabRichTextRenderer.height(FOOTER_TEXT_SIZE)) * 0.5f;
        for (Component line : snap.footerLines()) {
            drawCentered(renderer, line, baseX + drawWidth * 0.5f, y, drawWidth - 56f, FOOTER_TEXT_SIZE, 0xFFFFFFFF, alpha);
            y += lineH;
        }
    }

    private static void drawCentered(Renderer2D renderer, Component text, float centerX, float y, float maxWidth, float size, int color, float alpha) {
        if (text == null || alpha <= 0.01f || maxWidth <= 0f) return;
        float width = Math.min(maxWidth, TabRichTextRenderer.width(text, size));
        TabRichTextRenderer.draw(renderer, text, centerX - width * 0.5f, y, maxWidth, size, color, alpha);
    }

    private static float contentX(float baseX, float drawWidth, TabListModel.Snapshot snap) {
        float contentWidth = snap.columns() * snap.columnWidth() + Math.max(0, snap.columns() - 1) * snap.columnGap();
        return baseX + Math.max(SHELL_PAD_X, (drawWidth - contentWidth) * 0.5f);
    }
}
