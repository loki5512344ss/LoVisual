package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class AddonManagerRenderer {
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;
    private final AddonPanelsRenderer panels;
    private final AddonCardsRenderer cards;
    private float openAnim;

    private AddonManagerRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer, AddonPanelsRenderer panels, AddonCardsRenderer cards) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
        this.panels = panels;
        this.cards = cards;
    }

    static AddonManagerRenderer create() {
        TextRenderer title = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer body = Fonts.renderer("InterMedium", FontInfo.Type.Regular, title);
        AddonCardsRenderer cards = new AddonCardsRenderer(title, body);
        AddonPanelsRenderer panels = new AddonPanelsRenderer(title, body);
        return new AddonManagerRenderer(title, body, panels, cards);
    }

    void drawFrame(GuiGraphicsExtractor context, AddonManagerController controller, float mouseX, float mouseY) {
        controller.clampScroll();
        openAnim = AddonUiMetrics.animate(openAnim, 1f, AnimationUtility.deltaTime(), 7f);
        float open = AnimationUtility.easeOutCubic(openAnim);
        AddonUiMetrics.PanelColors c = colors(open);
        AddonUiMetrics.Layout l = controller.layout();

        ViewportContext.beginUnscaledLogical(context);
        Renderer2D.COLOR.begin();
        try {
            renderDimmer(controller);
            renderPanel(l.leftX(), l.topY(), AddonUiMetrics.LEFT_W, AddonUiMetrics.LEFT_TOP_H, c, open);
            renderPanel(l.leftX(), l.bottomY(), AddonUiMetrics.LEFT_W, AddonUiMetrics.LEFT_BOTTOM_H, c, open);
            renderPanel(l.rightX(), l.topY(), AddonUiMetrics.RIGHT_W, AddonUiMetrics.RIGHT_H, c, open);

            panels.draw(controller, mouseX, mouseY, l, c);
            cards.draw(controller, mouseX, mouseY, l, c);
        } finally {
            Renderer2D.COLOR.render();
            ViewportContext.end(context);
        }
    }

    private void renderPanel(float x, float y, float w, float h, AddonUiMetrics.PanelColors c, float open) {
        Renderer2D.COLOR.blurRect(x, y, w, h, AddonUiMetrics.PANEL_R, 15f, 1.0f, 0.30f, 0xFFFFFF);
        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, h, AddonUiMetrics.PANEL_R, AddonUiMetrics.PANEL_SHADOW, 0.022f + open * 0.012f, c.shadow());
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, AddonUiMetrics.PANEL_R, 1f, c.bgTopLeft(), c.bgTopRight(), c.bgBottomRight(), c.bgBottomLeft());
        Renderer2D.COLOR.roundedRectMaskedQuad(x, y, w, AddonUiMetrics.HEADER_H, x, y, w, h, AddonUiMetrics.PANEL_R, 1f,
                c.headerTopLeft(), c.headerTopRight(), c.headerBottomRight(), c.headerBottomLeft());
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, AddonUiMetrics.PANEL_R, 1f, 1f, HudRenderUtil.mixColor(c.stroke(), 0xFFFFFFFF, 0.04f), c.stroke(), 90f);
    }

    private AddonUiMetrics.PanelColors colors(float alpha) {
        int bgAlpha = Math.round(alpha * 120f);
        int headerAlpha = Math.round(alpha * 150f);
        int strokeAlpha = Math.round(alpha * 100f);
        int shadowAlpha = Math.round(alpha * 80f);
        int titleAlpha = Math.round(alpha * 255f);
        int labelAlpha = Math.round(alpha * 155f);
        Themes.Theme theme = Theme.theme();
        int themeAccent = theme != null ? theme.accent() : 0xFFFFFFFF;
        int themeAccentSoft = theme != null ? theme.accentSoft() : themeAccent;
        return new AddonUiMetrics.PanelColors(
                AddonUiMetrics.withAlpha(0x0D0F14, bgAlpha),
                AddonUiMetrics.withAlpha(0x101218, bgAlpha),
                AddonUiMetrics.withAlpha(0x0D0F14, bgAlpha),
                AddonUiMetrics.withAlpha(0x08090C, bgAlpha),
                AddonUiMetrics.withAlpha(0x14171F, headerAlpha),
                AddonUiMetrics.withAlpha(0x181B24, headerAlpha),
                AddonUiMetrics.withAlpha(0x14171F, headerAlpha),
                AddonUiMetrics.withAlpha(0x10131A, headerAlpha),
                AddonUiMetrics.withAlpha(0x252A36, strokeAlpha),
                AddonUiMetrics.withAlpha(0x3A4A5A, Math.round(alpha * 150f)),
                AddonUiMetrics.withAlpha(0x060810, shadowAlpha),
                AddonUiMetrics.withAlpha(0xFFFFFF, titleAlpha),
                AddonUiMetrics.withAlpha(0xFFFFFF, labelAlpha),
                AddonUiMetrics.withAlpha(0x808890, titleAlpha),
                AddonUiMetrics.withAlpha(0x606878, labelAlpha),
                AddonUiMetrics.withAlpha(0x1A1D24, Math.round(alpha * 160f)),
                AddonUiMetrics.withAlpha(0x1A1F28, Math.round(alpha * 200f)),
                AddonUiMetrics.withAlpha(themeAccent, titleAlpha),
                AddonUiMetrics.withAlpha(themeAccentSoft, Math.round(alpha * 180f)),
                AddonUiMetrics.withAlpha(0x1A1D24, Math.round(alpha * 160f)),
                AddonUiMetrics.withAlpha(0x0D1118, Math.round(alpha * 185f)),
                AddonUiMetrics.withAlpha(0x1A1D24, Math.round(alpha * 150f)),
                AddonUiMetrics.withAlpha(0x0D1118, Math.round(alpha * 175f)),
                AddonUiMetrics.withAlpha(0x3A4A5A, Math.round(alpha * 132f)),
                AddonUiMetrics.withAlpha(themeAccent, titleAlpha),
                AddonUiMetrics.withAlpha(HudRenderUtil.mixColor(themeAccent, themeAccentSoft, 0.58f), titleAlpha),
                AddonUiMetrics.withAlpha(0x060810, Math.round(alpha * 150f))
        );
    }

    private void renderDimmer(AddonManagerController controller) {
        Themes.Theme t = Theme.theme();
        int soft = AddonUiMetrics.withAlpha(HudRenderUtil.mixColor(t.windowBg(), 0xFF000000, 0.46f), 140);
        int deep = AddonUiMetrics.withAlpha(HudRenderUtil.mixColor(t.windowBg(), 0xFF000000, 0.68f), 180);
        Renderer2D.COLOR.quad(0, 0, controller.fixedWidth(), controller.fixedHeight(), soft, soft, deep, deep);
    }
}
