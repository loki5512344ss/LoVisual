package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.relations.category.CategoryType;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;

import java.util.ArrayList;
import java.util.List;

final class TabListRowRenderer {
    private static final float HEAD_SIZE = 30f;
    private static final float ROW_PAD_X = 10.5f;
    private static final float SHELL_PAD_X = 14f;

    private TabListRowRenderer() {}

    static List<RowRender> collect(float baseX, float baseY, float drawWidth, float panelAlpha, TabListModel.Snapshot snap, TabListAnimator animator, TabListPalette p) {
        List<TabListModel.Entry> entries = snap.entries();
        if (entries.isEmpty() || panelAlpha <= 0.01f) return List.of();
        int rowsPerColumn = Math.max(1, snap.rows());
        float contentX = contentX(baseX, drawWidth, snap);
        float rowTop = snap.rowTop();
        float rowHeight = snap.rowHeight();
        float columnStride = snap.columnWidth() + snap.columnGap();
        List<RowRender> out = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            TabListModel.Entry entry = entries.get(i);
            TabListAnimator.RowState state = animator.row(entry.id());
            float presence = state.presence;
            float rowAlpha = panelAlpha * AnimationUtility.easeOutCubic(presence);
            if (rowAlpha <= 0.01f) continue;
            int col = i / rowsPerColumn;
            int row = i % rowsPerColumn;
            float x = contentX + col * columnStride;
            float y = baseY + rowTop + row * rowHeight + (1f - presence) * 10.0f;
            float w = snap.columnWidth();
            float h = rowHeight - 5f;
            int rowFill = (row & 1) == 0 ? p.rowFill : p.rowAltFill;
            int relationColor = entry.relationColor() == 0 ? p.accent : entry.relationColor() | 0xFF000000;
            boolean relation = entry.relation() != CategoryType.DEFAULT;
            out.add(new RowRender(entry, x, y, w, h, rowAlpha, rowFill, relationColor, relation));
        }
        return out;
    }

    static void renderRows(Renderer2D renderer, List<RowRender> rows, TabListPalette p) {
        if (rows.isEmpty()) return;
        renderShadows(renderer, rows);
        renderFills(renderer, rows, p);
        renderRelationFills(renderer, rows);
        renderRelationBars(renderer, rows);
        renderStrokes(renderer, rows, p);
        renderHeadPlates(renderer, rows, p);
        renderHeadPlateStrokes(renderer, rows, p);
        renderDividers(renderer, rows, p);
        renderSpectatorBadges(renderer, rows, p);
        renderScoreChipFills(renderer, rows, p);
        renderScoreChipStrokes(renderer, rows, p);
        renderPingChipFills(renderer, rows, p);
        renderPingChipStrokes(renderer, rows, p);
        renderPingChipGlows(renderer, rows);
        renderPingChipOuterDots(renderer, rows);
        renderPingChipInnerDots(renderer, rows);
        renderPingChipDotStrokes(renderer, rows);
        renderPingChipArcs(renderer, rows);
    }

    private static void renderShadows(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) r.roundedRectSoftShadow(rr.x + 1.5f, rr.y + 2f, rr.w - 3f, rr.h, 9.5f, 6f, 0.18f, HudRenderUtil.scaleAlpha(0xFF000000, 0.14f * rr.alpha));
    }
    private static void renderFills(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) r.roundedRectGradientQuad(rr.x, rr.y, rr.w, rr.h, 9.5f, HudRenderUtil.scaleAlpha(p.rowHighlight, rr.alpha), HudRenderUtil.scaleAlpha(rr.rowFill, rr.alpha), HudRenderUtil.scaleAlpha(rr.rowFill, rr.alpha), HudRenderUtil.scaleAlpha(p.rowHighlight, rr.alpha));
    }
    private static void renderRelationFills(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) {
            if (!rr.relation) continue;
            int f = HudRenderUtil.scaleAlpha(rr.relationColor, 0.16f * rr.alpha);
            r.roundedRectGradientQuad(rr.x + 1f, rr.y + 1f, rr.w - 2f, rr.h - 2f, 8.5f, f, HudRenderUtil.scaleAlpha(f, 0.42f), HudRenderUtil.scaleAlpha(f, 0.28f), HudRenderUtil.scaleAlpha(f, 0.68f));
        }
    }
    private static void renderRelationBars(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) if (rr.relation) r.roundedRectGradient(rr.x + 1.8f, rr.y + 6f, 2.25f, rr.h - 12f, 1.15f, HudRenderUtil.scaleAlpha(rr.relationColor, 0.62f * rr.alpha), HudRenderUtil.scaleAlpha(rr.relationColor, 0.18f * rr.alpha), 90f);
    }
    private static void renderStrokes(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) {
            int strokeA = rr.relation ? HudRenderUtil.scaleAlpha(rr.relationColor, 0.52f * rr.alpha) : HudRenderUtil.scaleAlpha(p.stroke, 0.40f * rr.alpha);
            r.roundedRectStrokeGradient(rr.x, rr.y, rr.w, rr.h, 9.5f, rr.relation ? 1.25f : 0.85f, strokeA, HudRenderUtil.scaleAlpha(p.strokeSoft, 0.24f * rr.alpha), 92f);
        }
    }
    private static void renderHeadPlates(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) {
            float px = rr.x + ROW_PAD_X - 3.5f;
            float py = rr.y + (rr.h - HEAD_SIZE) * 0.5f - 3.5f;
            r.roundedRectGradient(px, py, 37f, 37f, 7.75f, HudRenderUtil.scaleAlpha(p.rowHighlight, 0.72f * rr.alpha), HudRenderUtil.scaleAlpha(p.rowFill, 0.56f * rr.alpha), 92f);
        }
    }
    private static void renderHeadPlateStrokes(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) {
            float px = rr.x + ROW_PAD_X - 3.5f;
            float py = rr.y + (rr.h - HEAD_SIZE) * 0.5f - 3.5f;
            r.roundedRectStroke(px, py, 37f, 37f, 7.75f, 0f, 0.75f, HudRenderUtil.scaleAlpha(p.strokeSoft, 0.30f * rr.alpha));
        }
    }
    private static void renderDividers(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) r.roundedRectGradient(rr.x + ROW_PAD_X + HEAD_SIZE + 10.5f, rr.y + 8.5f, 1.15f, Math.max(6f, rr.h - 17f), 0.6f, HudRenderUtil.scaleAlpha(p.accentSoft, 0.20f * rr.alpha), HudRenderUtil.scaleAlpha(p.divider, rr.alpha), 90f);
    }
    private static void renderSpectatorBadges(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) if (rr.entry.spectator()) r.roundedRectGradient(rr.x + rr.w - 57f, rr.y + 8.5f, 19f, 4.5f, 2.25f, HudRenderUtil.scaleAlpha(p.muted, 0.40f * rr.alpha), HudRenderUtil.scaleAlpha(p.muted, 0.08f * rr.alpha), 0f);
    }
    private static void renderScoreChipFills(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) if (rr.entry.scoreText() != null) { ScoreChip c = scoreChip(rr); r.roundedRectGradient(c.x, c.y, c.w, c.h, 7.25f, HudRenderUtil.scaleAlpha(p.pingChip, 0.82f * rr.alpha), HudRenderUtil.scaleAlpha(p.pingChip, 0.50f * rr.alpha), 90f); }
    }
    private static void renderScoreChipStrokes(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) if (rr.entry.scoreText() != null) { ScoreChip c = scoreChip(rr); r.roundedRectStroke(c.x, c.y, c.w, c.h, 7.25f, 0f, 0.75f, HudRenderUtil.scaleAlpha(p.strokeSoft, 0.30f * rr.alpha)); }
    }
    private static void renderPingChipFills(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); r.roundedRectGradient(c.x, c.y, c.w, c.h, 7.5f, HudRenderUtil.scaleAlpha(p.pingChip, 0.84f * rr.alpha), HudRenderUtil.scaleAlpha(p.pingChip, 0.52f * rr.alpha), 90f); }
    }
    private static void renderPingChipStrokes(Renderer2D r, List<RowRender> rows, TabListPalette p) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); r.roundedRectStrokeGradient(c.x, c.y, c.w, c.h, 7.5f, 0.8f, HudRenderUtil.scaleAlpha(p.strokeSoft, 0.40f * rr.alpha), HudRenderUtil.scaleAlpha(p.strokeSoft, 0.18f * rr.alpha), 0f); }
    }
    private static void renderPingChipGlows(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); r.circleSoftShadow(c.cx, c.cy, 5.4f, 7f, 0.34f, HudRenderUtil.scaleAlpha(pingBaseColor(rr.entry.latency()), 0.48f * rr.alpha)); }
    }
    private static void renderPingChipOuterDots(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); r.circle(c.cx, c.cy, 4.4f, 0.95f, HudRenderUtil.scaleAlpha(pingBaseColor(rr.entry.latency()), 0.30f * rr.alpha)); }
    }
    private static void renderPingChipInnerDots(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); r.circle(c.cx, c.cy, 3.35f, 0.9f, HudRenderUtil.scaleAlpha(pingBaseColor(rr.entry.latency()), rr.alpha)); }
    }
    private static void renderPingChipDotStrokes(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); r.circleStroke(c.cx, c.cy, 4.15f, 0.7f, 0.8f, HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.36f * rr.alpha)); }
    }
    private static void renderPingChipArcs(Renderer2D r, List<RowRender> rows) {
        for (RowRender rr : rows) { PingChip c = pingChip(rr); int base = pingBaseColor(rr.entry.latency()); r.arcStrokeGradient(c.cx, c.cy, 5.6f, 1.25f, -140f, 70f, 0.45f, HudRenderUtil.scaleAlpha(base, 0.68f * rr.alpha), HudRenderUtil.scaleAlpha(0xFFFFFFFF, 0.16f * rr.alpha), 35f); }
    }

    static ScoreChip scoreChip(RowRender rr) {
        float pingChipW = rr.entry.pingChipWidth();
        float occupiedLeft = rr.x + rr.w - 10.5f - pingChipW - 10f;
        float scoreBoxW = rr.entry.scoreBoxWidth();
        float scoreBoxH = 23f;
        return new ScoreChip(occupiedLeft - scoreBoxW - 8f, rr.y + (rr.h - scoreBoxH) * 0.5f, scoreBoxW, scoreBoxH);
    }
    static PingChip pingChip(RowRender rr) {
        float chipW = rr.entry.pingChipWidth();
        float chipH = 22.5f;
        float chipX = rr.x + rr.w - 10.5f - chipW;
        float chipY = rr.y + (rr.h - chipH) * 0.5f;
        return new PingChip(chipX, chipY, chipW, chipH, chipX + 10.2f, chipY + chipH * 0.5f);
    }
    static int pingBaseColor(int ping) {
        if (ping < 0) return 0xFF8993A3;
        if (ping < 90) return 0xFF52F2A1;
        if (ping < 180) return 0xFFFFD166;
        return 0xFFFF5A6E;
    }
    static float contentX(float baseX, float drawWidth, TabListModel.Snapshot snap) {
        float contentWidth = snap.columns() * snap.columnWidth() + Math.max(0, snap.columns() - 1) * snap.columnGap();
        return baseX + Math.max(SHELL_PAD_X, (drawWidth - contentWidth) * 0.5f);
    }
    record RowRender(TabListModel.Entry entry, float x, float y, float w, float h, float alpha, int rowFill, int relationColor, boolean relation) {}
    record ScoreChip(float x, float y, float w, float h) {}
    record PingChip(float x, float y, float w, float h, float cx, float cy) {}
}
