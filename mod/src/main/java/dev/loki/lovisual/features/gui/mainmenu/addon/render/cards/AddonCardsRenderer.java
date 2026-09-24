package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.addon.core.AddonManager;
import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.List;

final class AddonCardsRenderer {
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;
    private final AddonCardRenderer cardRenderer;

    AddonCardsRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
        this.cardRenderer = new AddonCardRenderer(titleRenderer, bodyRenderer);
    }

    void draw(AddonManagerController controller, float mouseX, float mouseY, AddonUiMetrics.Layout l, AddonUiMetrics.PanelColors c) {
        renderListHeader(l.rightX() + 8f * AddonUiMetrics.SCALE, l.topY(), AddonUiMetrics.RIGHT_W - 16f * AddonUiMetrics.SCALE, c, mouseX, mouseY, controller);
        List<AddonSnapshot> addons = AddonManager.snapshots();
        float listX = l.rightX() + 5f * AddonUiMetrics.SCALE;
        float listY = controller.bodyY(l);
        float listW = AddonUiMetrics.RIGHT_W - 10f * AddonUiMetrics.SCALE;
        float listH = controller.listHeight(l);
        if (addons.isEmpty()) {
            renderEmptyState(listX, listY, listW, listH, c);
            return;
        }
        int columns = AddonManagerOps.listColumns(addons.size(), listW);
        float cardW = AddonManagerOps.cardWidth(listW, columns);
        boolean clipped = ScissorFunction.pushRaw(listX, listY - 3f * AddonUiMetrics.SCALE, listW, listH + 6f * AddonUiMetrics.SCALE);
        try {
            for (int i = 0; i < addons.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                float cardX = listX + col * (cardW + AddonUiMetrics.CARD_GAP);
                float cardY = listY + row * (AddonUiMetrics.CARD_H + AddonUiMetrics.CARD_GAP) - controller.scrollOffset();
                if (cardY + AddonUiMetrics.CARD_H < listY - 10f || cardY > listY + listH + 10f) continue;
                cardRenderer.renderCard(addons.get(i), cardX, cardY, cardW, AddonUiMetrics.CARD_H, c, AddonUiMetrics.inside(mouseX, mouseY, cardX, cardY, cardW, AddonUiMetrics.CARD_H), controller);
            }
        } finally {
            if (clipped) ScissorFunction.pop();
        }
        int rows = AddonManagerOps.rowsFor(addons.size(), columns);
        float contentH = Math.max(0f, rows * AddonUiMetrics.CARD_H + Math.max(0, rows - 1) * AddonUiMetrics.CARD_GAP);
        if (contentH > listH + 1f) {
            float trackX = listX + listW + 2.5f * AddonUiMetrics.SCALE;
            float thumbH = Math.max(18f * AddonUiMetrics.SCALE, listH * listH / contentH);
            float thumbY = listY + (listH - thumbH) * (controller.scrollOffset() / Math.max(1f, contentH - listH));
            Renderer2D.COLOR.roundedRect(trackX, listY, 1.4f * AddonUiMetrics.SCALE, listH, 0.7f * AddonUiMetrics.SCALE, 1f, c.scrollTrack());
            Renderer2D.COLOR.roundedRect(trackX - 0.2f * AddonUiMetrics.SCALE, thumbY, 1.8f * AddonUiMetrics.SCALE, thumbH, 0.9f * AddonUiMetrics.SCALE, 1f, c.accent());
        }
    }

    private void renderListHeader(float x, float headerTop, float w, AddonUiMetrics.PanelColors c, float mouseX, float mouseY, AddonManagerController controller) {
        float centerY = headerTop + AddonUiMetrics.HEADER_H * 0.5f;
        float iconSize = 13.0f * AddonUiMetrics.SCALE;
        float iconX = x + 7.0f * AddonUiMetrics.SCALE;
        float iconY = centerY - iconSize * 0.5f;
        AddonUiMetrics.drawSvgGradient("folder-tree", iconX, iconY, iconSize, iconSize, c);
        String title = AddonUiMetrics.tr("screen.lovisual.addon_manager.list");
        float titleX = x + 25.0f * AddonUiMetrics.SCALE;
        float titleW = Math.max(1f, w - 33f * AddonUiMetrics.SCALE);
        float titleY = centerY - 4.8f * AddonUiMetrics.SCALE;
        AddonUiMetrics.drawFitOrMarquee(titleRenderer, title, titleX, titleY, AddonUiMetrics.TITLE_FONT, titleW, c.title(), AddonUiMetrics.inside(mouseX, mouseY, titleX, titleY - 1.5f * AddonUiMetrics.SCALE, titleW, 12f * AddonUiMetrics.SCALE));
        if (!controller.scanState().scanIssues().isEmpty()) {
            String label = AddonUiMetrics.tr("screen.lovisual.addon_manager.status.scanned", controller.scanState().scanIssues().size());
            float labelW = Math.min(w * 0.34f, Math.max(42f * AddonUiMetrics.SCALE, AddonUiMetrics.width(bodyRenderer, label, AddonUiMetrics.SMALL_FONT)));
            float labelX = x + w - labelW - 8f * AddonUiMetrics.SCALE;
            AddonUiMetrics.drawFitOrMarquee(bodyRenderer, label, labelX, centerY - 3.6f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, labelW, c.muted(), AddonUiMetrics.inside(mouseX, mouseY, labelX, centerY - 5f * AddonUiMetrics.SCALE, labelW, 10f * AddonUiMetrics.SCALE));
        }
    }

    private void renderEmptyState(float x, float y, float w, float h, AddonUiMetrics.PanelColors c) {
        float boxW = Math.min(w - 18f * AddonUiMetrics.SCALE, 110f * AddonUiMetrics.SCALE);
        float boxH = 44f * AddonUiMetrics.SCALE;
        float boxX = x + w * 0.5f - boxW * 0.5f;
        float boxY = y + h * 0.5f - boxH * 0.5f;
        Renderer2D.COLOR.roundedRectGradientQuad(boxX, boxY, boxW, boxH, 7f * AddonUiMetrics.SCALE, 1f, c.detailTop(), c.detailTop(), c.detailBottom(), c.detailBottom());
        Renderer2D.COLOR.roundedRectStrokeGradient(boxX, boxY, boxW, boxH, 7f * AddonUiMetrics.SCALE, 1.2f, 0.5f, c.strokeSoft(), c.stroke(), 90f);
        AddonUiMetrics.drawSvgGradient("folder-open", boxX + boxW * 0.5f - 8f * AddonUiMetrics.SCALE, boxY + 7f * AddonUiMetrics.SCALE, 16f * AddonUiMetrics.SCALE, 16f * AddonUiMetrics.SCALE, c);
        AddonUiMetrics.drawCentered(bodyRenderer, AddonUiMetrics.tr("screen.lovisual.addon_manager.empty"), boxX + boxW * 0.5f, boxY + 27f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, c.muted());
    }
}
