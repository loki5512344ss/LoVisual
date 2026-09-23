package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;

final class TabListShellRenderer {
    private TabListShellRenderer() {}

    static void renderShell(Renderer2D renderer, float x, float y, float w, float h, float alpha, TabListPalette p) {
        if (renderer == null || alpha <= 0.01f) return;
        renderer.roundedRectShadow(x, y, w, h, 14.5f, 8f, 15f, HudRenderUtil.scaleAlpha(0xFF000000, 0.30f * alpha));
        renderer.roundedRectSoftShadow(x + 4f, y + 4f, Math.max(0f, w - 8f), Math.max(0f, h - 2f), 12.5f, 12f, 0.20f, HudRenderUtil.scaleAlpha(p.accentGlow, 0.16f * alpha));
        renderer.roundedRectGlow(x + 2f, y + 1.5f, Math.max(0f, w - 4f), Math.max(0f, h - 3f), 13f, 0f, 8f, HudRenderUtil.scaleAlpha(p.accentGlow, 0.14f * alpha));
        renderer.roundedRectGradientQuad(x, y, w, h, 14.5f, HudRenderUtil.scaleAlpha(p.fillHotspot, 0.96f * alpha), HudRenderUtil.scaleAlpha(p.fillTop, 0.96f * alpha), HudRenderUtil.scaleAlpha(p.fillBottom, 0.97f * alpha), HudRenderUtil.scaleAlpha(p.fillBottom, 0.97f * alpha));
        renderer.radialGlowMasked(x, y, w, h, 14.5f, 0f, Math.max(90f, w * 0.30f), x + w * 0.18f, y + 9f, HudRenderUtil.scaleAlpha(p.accentGlow, 0.26f * alpha));
        renderer.radialGlowMasked(x, y, w, h, 14.5f, 0f, Math.max(70f, w * 0.22f), x + w * 0.86f, y + h - 8f, HudRenderUtil.scaleAlpha(p.accentSoft, 0.16f * alpha));
        renderer.roundedRectStrokeGradient(x, y, w, h, 14.5f, 1.25f, HudRenderUtil.scaleAlpha(p.stroke, 0.82f * alpha), HudRenderUtil.scaleAlpha(p.strokeSoft, 0.48f * alpha), 115f);
        float accentW = Math.max(62f, w * 0.34f);
        renderer.roundedRectGradient(x + 21f, y + 11f, accentW, 1.45f, 0.8f, HudRenderUtil.scaleAlpha(p.accent, 0.58f * alpha), HudRenderUtil.scaleAlpha(p.accentSoft, 0.10f * alpha), 0f);
        renderer.roundedRectGradient(x + w - 14f - accentW * 0.72f, y + h - 15.5f, accentW * 0.62f, 2.5f, 1.25f, HudRenderUtil.scaleAlpha(p.accentSoft, 0.06f * alpha), HudRenderUtil.scaleAlpha(p.accent, 0.25f * alpha), 0f);
    }

    static void renderHeaderFooterBlocks(Renderer2D renderer, float baseX, float baseY, float width, float alpha, TabListModel.Snapshot snapshot, TabListPalette p) {
        if (!snapshot.headerLines().isEmpty() && snapshot.headerHeight() > 0f) {
            float x = baseX + 14f;
            float y = baseY + snapshot.headerTop();
            float w = width - 28f;
            float h = snapshot.headerHeight();
            renderer.roundedRectGradientQuad(x, y, w, h, 10.5f, HudRenderUtil.scaleAlpha(p.rowHighlight, 0.78f * alpha), HudRenderUtil.scaleAlpha(p.rowHighlight, 0.52f * alpha), HudRenderUtil.scaleAlpha(p.rowFill, 0.48f * alpha), HudRenderUtil.scaleAlpha(p.rowFill, 0.55f * alpha));
            renderer.roundedRectStrokeGradient(x, y, w, h, 10.5f, 0.95f, HudRenderUtil.scaleAlpha(p.stroke, 0.44f * alpha), HudRenderUtil.scaleAlpha(p.strokeSoft, 0.18f * alpha), 96f);
            renderer.roundedRectGradient(x + 12f, y + h - 5.5f, Math.max(30f, w * 0.20f), 1.25f, 0.65f, HudRenderUtil.scaleAlpha(p.accent, 0.44f * alpha), HudRenderUtil.scaleAlpha(p.accentSoft, 0.06f * alpha), 0f);
        }
        if (!snapshot.footerLines().isEmpty() && snapshot.footerHeight() > 0f) {
            float x = baseX + 14f;
            float y = baseY + snapshot.footerTop();
            float w = width - 28f;
            float h = snapshot.footerHeight();
            renderer.roundedRectGradientQuad(x, y, w, h, 10f, HudRenderUtil.scaleAlpha(p.rowFill, 0.34f * alpha), HudRenderUtil.scaleAlpha(p.rowFill, 0.46f * alpha), HudRenderUtil.scaleAlpha(p.rowFill, 0.46f * alpha), HudRenderUtil.scaleAlpha(p.rowFill, 0.34f * alpha));
            renderer.roundedRectStroke(x, y, w, h, 10f, 0f, 0.8f, HudRenderUtil.scaleAlpha(p.strokeSoft, 0.30f * alpha));
            float accentW = Math.max(42f, w * 0.24f);
            renderer.roundedRectGradient(x + w - accentW - 12f, y + h - 5.5f, accentW, 1.15f, 0.6f, HudRenderUtil.scaleAlpha(p.accentSoft, 0.04f * alpha), HudRenderUtil.scaleAlpha(p.accent, 0.32f * alpha), 0f);
        }
    }

    static void renderChrome(Renderer2D renderer, float drawX, float drawY, float width, float height, float alpha, TabListModel.Snapshot snapshot, TabListPalette p) {
        if (renderer == null || alpha <= 0.01f) return;
        renderShell(renderer, drawX, drawY, width, height, alpha, p);
        renderHeaderFooterBlocks(renderer, drawX, drawY, width, alpha, snapshot, p);
    }
}
