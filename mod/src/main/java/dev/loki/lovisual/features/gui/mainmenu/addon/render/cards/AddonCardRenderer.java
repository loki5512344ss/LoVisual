package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

final class AddonCardRenderer {
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;

    AddonCardRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
    }

    void renderCard(AddonSnapshot addon, float x, float y, float w, float h, AddonUiMetrics.PanelColors c, boolean hovered, AddonManagerController controller) {
        float hover = AnimationUtility.easeOutCubic(controller.cardHover().getOrDefault(addon.id(), 0f));
        float active = AnimationUtility.easeOutCubic(controller.cardActive().getOrDefault(addon.id(), 0f));
        float press = AnimationUtility.easeOutCubic(controller.cardPress().getOrDefault(addon.id(), 0f));
        Themes.Theme theme = Theme.theme();
        int activeAccent = AddonUiMetrics.withAlpha(theme.accent(), Math.round(165f * active));
        int fillBase = HudRenderUtil.mixColor(c.surface(), c.surfaceHover(), 0.38f * hover + 0.10f * press);
        int fillTop = HudRenderUtil.mixColor(fillBase, c.surfaceHover(), 0.03f * press);
        int fillBottom = HudRenderUtil.mixColor(fillBase, 0xFF05070A, 0.18f);
        int stroke = HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), 0.45f * hover);
        stroke = HudRenderUtil.mixColor(stroke, activeAccent, 0.38f * active + 0.10f * press);
        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, h, 4f * AddonUiMetrics.SCALE, 8f * AddonUiMetrics.SCALE, 0.018f + hover * 0.018f + active * 0.010f, c.shadow());
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 4f * AddonUiMetrics.SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        renderActiveCardMarker(x, y, w, h, active, theme);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 4f * AddonUiMetrics.SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f + hover * 0.03f), stroke, 90f);
        float iconBox = 24f * AddonUiMetrics.SCALE;
        float iconX = x + 7f * AddonUiMetrics.SCALE;
        float iconY = y + 8f * AddonUiMetrics.SCALE;
        Renderer2D.COLOR.roundedRectGradientQuad(iconX, iconY, iconBox, iconBox, 3f * AddonUiMetrics.SCALE, 1f, AddonUiMetrics.withAlpha(c.surface(), 170), AddonUiMetrics.withAlpha(c.surfaceHover(), 180), AddonUiMetrics.withAlpha(0xFF0D1118, 184), AddonUiMetrics.withAlpha(0xFF0D1118, 184));
        Renderer2D.COLOR.roundedRectStrokeGradient(iconX, iconY, iconBox, iconBox, 3f * AddonUiMetrics.SCALE, 1f, 0.5f, HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), 0.35f), c.stroke(), 90f);
        AddonUiMetrics.drawSvgGradient(AddonManagerOps.addonIcon(addon), iconX + 6.2f * AddonUiMetrics.SCALE, iconY + 6.2f * AddonUiMetrics.SCALE, 11.6f * AddonUiMetrics.SCALE, 11.6f * AddonUiMetrics.SCALE, c);
        String state = AddonManagerOps.statusText(addon);
        float stateW = AddonUiMetrics.miniStatusWidth(bodyRenderer, state);
        float stateX = x + w - stateW - 6f * AddonUiMetrics.SCALE;
        renderMiniStatusPill(stateX, y + 7f * AddonUiMetrics.SCALE, state, AddonManagerOps.statusColor(addon, c), c);
        float textX = x + 37f * AddonUiMetrics.SCALE;
        float titleW = Math.max(1f, stateX - textX - 5f * AddonUiMetrics.SCALE);
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, addon.name(), textX, y + 8.3f * AddonUiMetrics.SCALE, AddonUiMetrics.ADDON_NAME_FONT, titleW, c.title(), hovered);
        String description = AddonManagerOps.addonInfo(addon);
        if (!description.isBlank()) {
            float descW = Math.max(1f, x + w - 7f * AddonUiMetrics.SCALE - textX);
            AddonUiMetrics.drawFitOrMarquee(bodyRenderer, description, textX, y + 20.3f * AddonUiMetrics.SCALE, AddonUiMetrics.DESCRIPTION_FONT, descW, addon.restartRequired() ? c.label() : c.muted(), hovered);
        }
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, AddonManagerOps.countsText(addon), textX, y + 29.6f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, Math.max(1f, x + w - 7f * AddonUiMetrics.SCALE - textX), c.mutedLabel(), hovered);
    }

    private void renderActiveCardMarker(float x, float y, float w, float h, float active, Themes.Theme theme) {
        float reveal = AnimationUtility.clamp01(active);
        if (reveal <= 0.001f) return;
        float markerW = AddonUiMetrics.CARD_MARKER_W * reveal;
        if (markerW <= 0.01f) return;
        int accent = AddonUiMetrics.withAlpha(theme.accent(), Math.round(255f * reveal));
        boolean clipped = ScissorFunction.pushRaw(x, y, markerW, h);
        if (!clipped) return;
        try {
            Renderer2D.COLOR.roundedRectCornersQuad(x, y, w, h, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, 1f, accent, accent, accent, accent);
        } finally {
            ScissorFunction.pop();
        }
    }

    private void renderMiniStatusPill(float x, float y, String label, int color, AddonUiMetrics.PanelColors c) {
        float w = AddonUiMetrics.miniStatusWidth(bodyRenderer, label);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, 9f * AddonUiMetrics.SCALE, 4.5f * AddonUiMetrics.SCALE, 1f, HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(c.surface(), 170), color, 0.08f), AddonUiMetrics.withAlpha(c.surface(), 170), AddonUiMetrics.withAlpha(0xFF0D1118, 178), AddonUiMetrics.withAlpha(0xFF0D1118, 178));
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, 9f * AddonUiMetrics.SCALE, 4.5f * AddonUiMetrics.SCALE, 1f, 0.42f, AddonUiMetrics.withAlpha(color, 112), c.stroke(), 90f);
        Renderer2D.COLOR.roundedRect(x + 3.6f * AddonUiMetrics.SCALE, y + 3.2f * AddonUiMetrics.SCALE, 2.4f * AddonUiMetrics.SCALE, 2.4f * AddonUiMetrics.SCALE, 1.2f * AddonUiMetrics.SCALE, 1f, color);
        AddonUiMetrics.draw(bodyRenderer, label, x + 8f * AddonUiMetrics.SCALE, y + 1.75f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, c.label());
    }
}
