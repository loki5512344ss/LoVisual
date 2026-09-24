package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.addon.core.AddonIssue;
import dev.loki.lovisual.addon.core.AddonManager;
import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;

final class AddonPanelsRenderer {
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;

    AddonPanelsRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
    }

    void draw(AddonManagerController controller, float mouseX, float mouseY, AddonUiMetrics.Layout l, AddonUiMetrics.PanelColors c) {
        renderDetails(l, c, mouseX, mouseY, controller);
        renderActionsPanel(l, c, controller);
    }

    private void renderDetails(AddonUiMetrics.Layout l, AddonUiMetrics.PanelColors c, float mouseX, float mouseY, AddonManagerController controller) {
        List<AddonSnapshot> addons = AddonManager.snapshots();
        AddonSnapshot selected = controller.selected(addons);
        int active = 0;
        int warnings = 0;
        int errors = 0;
        for (AddonSnapshot addon : addons) {
            if (addon.enabled()) active++;
            for (AddonIssue issue : addon.issues()) {
                if (issue.severity() == AddonIssue.Severity.ERROR) errors++;
                else if (issue.severity() == AddonIssue.Severity.WARNING) warnings++;
            }
        }
        for (AddonIssue issue : controller.scanState().scanIssues()) {
            if (issue.severity() == AddonIssue.Severity.ERROR) errors++;
            else if (issue.severity() == AddonIssue.Severity.WARNING) warnings++;
        }
        float x = l.leftX() + 5f * AddonUiMetrics.SCALE;
        float y = l.topY();
        float w = AddonUiMetrics.LEFT_W - 10f * AddonUiMetrics.SCALE;
        AddonUiMetrics.draw(titleRenderer, AddonUiMetrics.tr("screen.lovisual.addon_manager.header"), l.leftX() + 8f * AddonUiMetrics.SCALE, y + 7f * AddonUiMetrics.SCALE, AddonUiMetrics.TITLE_FONT, c.title());
        renderSelectedDetails(x, y + 27f * AddonUiMetrics.SCALE, w, c, selected, mouseX, mouseY);
        float metricsY = y + 75f * AddonUiMetrics.SCALE;
        float metricStep = AddonUiMetrics.METRIC_H + AddonUiMetrics.METRIC_GAP;
        renderMetricLine(x, metricsY, w, AddonUiMetrics.tr("screen.lovisual.addon_manager.installed"), String.valueOf(addons.size()), "package", c, AddonUiMetrics.inside(mouseX, mouseY, x, metricsY, w, AddonUiMetrics.METRIC_H));
        renderMetricLine(x, metricsY + metricStep, w, AddonUiMetrics.tr("screen.lovisual.addon_manager.active"), String.valueOf(active), "check", c, AddonUiMetrics.inside(mouseX, mouseY, x, metricsY + metricStep, w, AddonUiMetrics.METRIC_H));
        renderMetricLine(x, metricsY + metricStep * 2f, w, AddonUiMetrics.tr("screen.lovisual.addon_manager.issues"), errors + "/" + warnings, errors > 0 ? "folder-x" : "folder-clock", c, AddonUiMetrics.inside(mouseX, mouseY, x, metricsY + metricStep * 2f, w, AddonUiMetrics.METRIC_H));
        if (controller.statusVisible()) {
            float statusY = y + AddonUiMetrics.LEFT_TOP_H - 15f * AddonUiMetrics.SCALE;
            renderStatusLine(x, statusY, w, controller.status(), c, AddonUiMetrics.inside(mouseX, mouseY, x, statusY, w, 9f * AddonUiMetrics.SCALE));
        }
    }

    private void renderActionsPanel(AddonUiMetrics.Layout l, AddonUiMetrics.PanelColors c, AddonManagerController controller) {
        List<AddonSnapshot> addons = AddonManager.snapshots();
        AddonSnapshot selected = controller.selected(addons);
        float x = l.leftX() + 5f * AddonUiMetrics.SCALE;
        float y = l.bottomY() + 6f * AddonUiMetrics.SCALE;
        float w = AddonUiMetrics.LEFT_W - 10f * AddonUiMetrics.SCALE;
        float half = (w - AddonUiMetrics.GAP) * 0.5f;
        AddonPanelAnims a = controller.anims();
        renderAction(x, y, half, AddonUiMetrics.BUTTON_H, selected != null && selected.enabled()
                ? AddonUiMetrics.tr("screen.lovisual.addon_manager.disable")
                : AddonUiMetrics.tr("screen.lovisual.addon_manager.enable"), selected != null && selected.enabled(), selected != null && selected.enabled() ? "toggle-left" : "toggle-right", a.toggleHover(), a.togglePress(), c);
        renderAction(x + half + AddonUiMetrics.GAP, y, half, AddonUiMetrics.BUTTON_H, AddonUiMetrics.tr("screen.lovisual.addon_manager.scan"), false, "folder-sync", a.scanHover(), a.scanPress(), c);
        renderAction(x, y + 17f * AddonUiMetrics.SCALE, half, AddonUiMetrics.BUTTON_H, AddonUiMetrics.tr("screen.lovisual.addon_manager.mods"), false, "folder-open", a.modsHover(), a.modsPress(), c);
        renderAction(x + half + AddonUiMetrics.GAP, y + 17f * AddonUiMetrics.SCALE, half, AddonUiMetrics.BUTTON_H, AddonUiMetrics.tr("screen.lovisual.addon_manager.config"), false, "folder-cog", a.configHover(), a.configPress(), c);
        renderAction(x, y + 34f * AddonUiMetrics.SCALE, w, AddonUiMetrics.BUTTON_H, AddonUiMetrics.tr("screen.lovisual.addon_manager.back"), false, "x", a.backHover(), a.backPress(), c);
    }

    private void renderMetricLine(float x, float y, float w, String label, String value, String svg, AddonUiMetrics.PanelColors c, boolean hovered) {
        float r = 3.5f * AddonUiMetrics.SCALE;
        int fillTop = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(c.surface(), 144), AddonUiMetrics.withAlpha(c.surfaceHover(), 166), hovered ? 0.34f : 0.12f);
        int fillBottom = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(0xFF0D1118, 158), AddonUiMetrics.withAlpha(c.surface(), 150), hovered ? 0.18f : 0.05f);
        int stroke = HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), hovered ? 0.42f : 0.20f);
        Renderer2D.COLOR.roundedRectCornersQuad(x, y, w, AddonUiMetrics.METRIC_H, r, r, r, r, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeCorners(x, y, w, AddonUiMetrics.METRIC_H, r, r, r, r, 0.55f, stroke);
        AddonUiMetrics.drawSvgGradient(svg, x + 4.0f * AddonUiMetrics.SCALE, y + 3.0f * AddonUiMetrics.SCALE, 7.0f * AddonUiMetrics.SCALE, 7.0f * AddonUiMetrics.SCALE, c);
        float valueW = AddonUiMetrics.width(bodyRenderer, value, AddonUiMetrics.METRIC_FONT);
        float textY = y + 3.15f * AddonUiMetrics.SCALE;
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, label, x + 14.0f * AddonUiMetrics.SCALE, textY, AddonUiMetrics.METRIC_FONT, Math.max(1f, w - valueW - 23f * AddonUiMetrics.SCALE), c.muted(), hovered);
        AddonUiMetrics.draw(bodyRenderer, value, x + w - valueW - 5f * AddonUiMetrics.SCALE, textY, AddonUiMetrics.METRIC_FONT, c.title());
    }

    private void renderSelectedDetails(float x, float y, float w, AddonUiMetrics.PanelColors c, AddonSnapshot selected, float mouseX, float mouseY) {
        float h = 42f * AddonUiMetrics.SCALE;
        Renderer2D.COLOR.roundedRectCornersQuad(x, y, w, h, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, 1f,
                AddonUiMetrics.withAlpha(c.surface(), 160), AddonUiMetrics.withAlpha(c.surfaceHover(), 170), AddonUiMetrics.withAlpha(0xFF0D1118, 178), AddonUiMetrics.withAlpha(0xFF0D1118, 178));
        Renderer2D.COLOR.roundedRectStrokeCorners(x, y, w, h, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, AddonUiMetrics.LEFT_CARD_R, 0.55f, HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), 0.36f));
        if (selected == null) {
            AddonUiMetrics.drawCentered(bodyRenderer, AddonUiMetrics.tr("screen.lovisual.addon_manager.empty"), x + w * 0.5f, y + 17f * AddonUiMetrics.SCALE, AddonUiMetrics.BODY_FONT, c.mutedLabel());
            return;
        }
        float iconBox = 18f * AddonUiMetrics.SCALE;
        float iconX = x + 6f * AddonUiMetrics.SCALE;
        float iconY = y + 7f * AddonUiMetrics.SCALE;
        Renderer2D.COLOR.roundedRectGradientQuad(iconX, iconY, iconBox, iconBox, 3f * AddonUiMetrics.SCALE, 1f, AddonUiMetrics.withAlpha(c.surface(), 176), AddonUiMetrics.withAlpha(c.surfaceHover(), 186), AddonUiMetrics.withAlpha(0xFF0D1118, 190), AddonUiMetrics.withAlpha(0xFF0D1118, 190));
        Renderer2D.COLOR.roundedRectStrokeGradient(iconX, iconY, iconBox, iconBox, 3f * AddonUiMetrics.SCALE, 1f, 0.5f, c.stroke(), c.strokeSoft(), 90f);
        AddonUiMetrics.drawSvgGradient(AddonManagerOps.addonIcon(selected), iconX + 4.3f * AddonUiMetrics.SCALE, iconY + 4.3f * AddonUiMetrics.SCALE, 9.4f * AddonUiMetrics.SCALE, 9.4f * AddonUiMetrics.SCALE, c);
        String state = AddonManagerOps.statusText(selected);
        float stateW = AddonUiMetrics.miniStatusWidth(bodyRenderer, state);
        float titleX = iconX + iconBox + 5f * AddonUiMetrics.SCALE;
        float stateX = x + w - stateW - 6f * AddonUiMetrics.SCALE;
        boolean drawState = stateX > titleX + 30f * AddonUiMetrics.SCALE;
        if (drawState) renderMiniStatusPill(stateX, y + 6f * AddonUiMetrics.SCALE, state, AddonManagerOps.statusColor(selected, c), c);
        float titleW = Math.max(1f, (drawState ? stateX - titleX - 5f * AddonUiMetrics.SCALE : x + w - titleX - 7f * AddonUiMetrics.SCALE));
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, selected.name(), titleX, y + 7.4f * AddonUiMetrics.SCALE, AddonUiMetrics.SELECTED_NAME_FONT, titleW, c.title(), AddonUiMetrics.inside(mouseX, mouseY, titleX, y + 6f * AddonUiMetrics.SCALE, titleW, 10f * AddonUiMetrics.SCALE));
        String info = AddonManagerOps.addonInfo(selected);
        if (!info.isBlank()) {
            AddonUiMetrics.drawFitOrMarquee(bodyRenderer, info, titleX, y + 19.4f * AddonUiMetrics.SCALE, AddonUiMetrics.SELECTED_DESCRIPTION_FONT, Math.max(1f, x + w - titleX - 7f * AddonUiMetrics.SCALE), selected.restartRequired() ? c.label() : c.muted(), AddonUiMetrics.inside(mouseX, mouseY, titleX, y + 18f * AddonUiMetrics.SCALE, w - 35f * AddonUiMetrics.SCALE, 10f * AddonUiMetrics.SCALE));
        }
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, AddonManagerOps.countsText(selected), x + 7f * AddonUiMetrics.SCALE, y + 31.0f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, w - 14f * AddonUiMetrics.SCALE, c.mutedLabel(), AddonUiMetrics.inside(mouseX, mouseY, x + 7f * AddonUiMetrics.SCALE, y + 30f * AddonUiMetrics.SCALE, w - 14f * AddonUiMetrics.SCALE, 10f * AddonUiMetrics.SCALE));
    }

    private void renderStatusLine(float x, float y, float w, String text, AddonUiMetrics.PanelColors c, boolean hovered) {
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, 9f * AddonUiMetrics.SCALE, 4.5f * AddonUiMetrics.SCALE, 1f, c.statusTop(), c.statusTop(), c.statusBottom(), c.statusBottom());
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, 9f * AddonUiMetrics.SCALE, 4.5f * AddonUiMetrics.SCALE, 1.2f, 0.4f, c.strokeSoft(), c.stroke(), 90f);
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, text, x + 5f * AddonUiMetrics.SCALE, y + 1.8f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, w - 10f * AddonUiMetrics.SCALE, c.muted(), hovered);
    }

    private void renderMiniStatusPill(float x, float y, String label, int color, AddonUiMetrics.PanelColors c) {
        float w = AddonUiMetrics.miniStatusWidth(bodyRenderer, label);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, 9f * AddonUiMetrics.SCALE, 4.5f * AddonUiMetrics.SCALE, 1f, HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(c.surface(), 170), color, 0.08f), AddonUiMetrics.withAlpha(c.surface(), 170), AddonUiMetrics.withAlpha(0xFF0D1118, 178), AddonUiMetrics.withAlpha(0xFF0D1118, 178));
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, 9f * AddonUiMetrics.SCALE, 4.5f * AddonUiMetrics.SCALE, 1f, 0.42f, AddonUiMetrics.withAlpha(color, 112), c.stroke(), 90f);
        Renderer2D.COLOR.roundedRect(x + 3.6f * AddonUiMetrics.SCALE, y + 3.2f * AddonUiMetrics.SCALE, 2.4f * AddonUiMetrics.SCALE, 2.4f * AddonUiMetrics.SCALE, 1.2f * AddonUiMetrics.SCALE, 1f, color);
        AddonUiMetrics.draw(bodyRenderer, label, x + 8f * AddonUiMetrics.SCALE, y + 1.75f * AddonUiMetrics.SCALE, AddonUiMetrics.SMALL_FONT, c.label());
    }

    private void renderAction(float x, float y, float w, float h, String label, boolean destructive, String svgName, float hover, float press, AddonUiMetrics.PanelColors c) {
        float hoverAnim = AnimationUtility.easeOutCubic(hover);
        float pressAnim = AnimationUtility.easeOutCubic(press);
        int fillTop;
        int fillBottom;
        int stroke;
        int text;
        if (destructive) {
            fillTop = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(0x1A1416, 170), AddonUiMetrics.withAlpha(0x4B2224, 212), 0.25f + hoverAnim * 0.45f + pressAnim * 0.12f);
            fillBottom = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(0x120E10, 165), AddonUiMetrics.withAlpha(0x261012, 220), 0.20f + hoverAnim * 0.40f + pressAnim * 0.10f);
            stroke = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(0x352A2A, 215), AddonUiMetrics.withAlpha(0x8C5054, 235), 0.22f + hoverAnim * 0.58f + pressAnim * 0.12f);
            text = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(0xD0A0A0, 255), AddonUiMetrics.withAlpha(0xFFB2B2, 255), 0.24f + hoverAnim * 0.56f + pressAnim * 0.08f);
        } else {
            fillTop = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(c.surface(), 176), AddonUiMetrics.withAlpha(0x223746, 212), 0.20f + hoverAnim * 0.44f + pressAnim * 0.10f);
            fillBottom = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(c.surface(), 166), AddonUiMetrics.withAlpha(0x13292F, 220), 0.16f + hoverAnim * 0.40f + pressAnim * 0.08f);
            stroke = HudRenderUtil.mixColor(c.stroke(), AddonUiMetrics.withAlpha(0x466A76, 228), 0.18f + hoverAnim * 0.50f + pressAnim * 0.10f);
            text = HudRenderUtil.mixColor(AddonUiMetrics.withAlpha(0xD0D8E4, 255), AddonUiMetrics.withAlpha(0xEEF8FF, 255), 0.18f + hoverAnim * 0.38f + pressAnim * 0.08f);
        }
        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, h, 3f * AddonUiMetrics.SCALE, 7f * AddonUiMetrics.SCALE, 0.018f + hoverAnim * 0.015f, c.shadow());
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * AddonUiMetrics.SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * AddonUiMetrics.SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        float contentLift = hoverAnim * 0.65f * AddonUiMetrics.SCALE + pressAnim * 0.55f * AddonUiMetrics.SCALE;
        AddonUiMetrics.drawFitOrMarquee(bodyRenderer, label, x + 5f * AddonUiMetrics.SCALE, y + 3.7f * AddonUiMetrics.SCALE - contentLift, AddonUiMetrics.BUTTON_FONT, w - 18f * AddonUiMetrics.SCALE, text, hoverAnim > 0.55f);
        float iconSize = 6.7f * AddonUiMetrics.SCALE;
        Renderer2D.COLOR.svg(svgName, x + w - 10.7f * AddonUiMetrics.SCALE, y + (h - iconSize) * 0.5f - contentLift, iconSize, iconSize, SvgRenderOptions.overrideColor(text));
    }
}
