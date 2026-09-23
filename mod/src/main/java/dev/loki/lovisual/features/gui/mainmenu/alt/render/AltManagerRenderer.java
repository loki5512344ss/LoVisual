/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.mainmenu.alt.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltManagerController;
import dev.loki.lovisual.features.gui.mainmenu.alt.auth.AltMicrosoftAuth;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.cards.AltCardsRenderer;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.Layout;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PanelColors;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.GAP;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.LEFT_BOTTOM_H;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.LEFT_TOP_H;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.LEFT_W;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PANEL_R;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PANEL_SHADOW;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.RIGHT_H;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.RIGHT_W;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.SCALE;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.animate;
import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.withAlpha;

/**
 * Root renderer for the alt manager screen: panel backgrounds, palette and frame orchestration.
 */
public final class AltManagerRenderer {
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;
    private final TextRenderer menuIconRenderer;
    private final TextRenderer guiIconRenderer;
    private final TextRenderer iconRenderer;
    private final AltPanelsRenderer panels;
    private final AltCardsRenderer cards;
    private float openAnim;

    private AltManagerRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer, TextRenderer menuIconRenderer,
                               TextRenderer guiIconRenderer, TextRenderer iconRenderer,
                               AltPanelsRenderer panels, AltCardsRenderer cards) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
        this.menuIconRenderer = menuIconRenderer;
        this.guiIconRenderer = guiIconRenderer;
        this.iconRenderer = iconRenderer;
        this.panels = panels;
        this.cards = cards;
    }

    public static AltManagerRenderer create() {
        TextRenderer title = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer body = Fonts.renderer("InterMedium", FontInfo.Type.Regular, title);
        TextRenderer menuIcon = Fonts.renderer("MainMenuIcons", FontInfo.Type.Regular, TextRenderer.get());
        TextRenderer guiIcon = Fonts.renderer("GuiIcons", FontInfo.Type.Regular, menuIcon);
        TextRenderer icon = Fonts.renderer("RichIcons", FontInfo.Type.Regular, menuIcon);
        return new AltManagerRenderer(title, body, menuIcon, guiIcon, icon,
                new AltPanelsRenderer(title, body, guiIcon, icon), new AltCardsRenderer(title, body, menuIcon, guiIcon));
    }

    public void drawFrame(GuiGraphicsExtractor context, AltManagerController controller, AltMicrosoftAuth auth,
                          float mouseX, float mouseY) {
        controller.clampScroll();
        controller.updateScroll();
        openAnim = animate(openAnim, 1f, AnimationUtility.deltaTime(), 7f);
        float open = AnimationUtility.easeOutCubic(openAnim);
        PanelColors c = colors(open);
        Layout l = controller.layout();

        ViewportContext.beginUnscaledLogical(context);
        Renderer2D.COLOR.begin();
        try {
            drawDimmer(controller);
            renderPanel(l.leftX(), l.topY(), LEFT_W, LEFT_TOP_H, c, open);
            renderPanel(l.leftX(), l.bottomY(), LEFT_W, LEFT_BOTTOM_H, c, open);
            renderPanel(l.rightX(), l.topY(), RIGHT_W, RIGHT_H, c, open);

            panels.draw(controller, auth, mouseX, mouseY, l, c);
            cards.draw(context, controller, mouseX, mouseY, l, c);
        } finally {
            Renderer2D.COLOR.render();
            ViewportContext.end(context);
        }
    }

    public boolean handleCardClick(float mouseX, float mouseY, AltManagerController controller) {
        return cards.handleClick(controller, mouseX, mouseY);
    }

    private void renderPanel(float x, float y, float w, float h, PanelColors c, float open) {
        Renderer2D.COLOR.blurRect(x, y, w, h, PANEL_R, 15f, 1.0f, 0.30f, 0xFFFFFF);
        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, h, PANEL_R, PANEL_SHADOW, 0.022f + open * 0.012f, c.shadow());
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, PANEL_R, 1f, c.bgTopLeft(), c.bgTopRight(), c.bgBottomRight(), c.bgBottomLeft());
        Renderer2D.COLOR.roundedRectMaskedQuad(x, y, w, 22f * SCALE, x, y, w, h, PANEL_R, 1f,
                c.headerTopLeft(), c.headerTopRight(), c.headerBottomRight(), c.headerBottomLeft());
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, PANEL_R, 1f, 1f, HudRenderUtil.mixColor(c.stroke(), 0xFFFFFFFF, 0.04f), c.stroke(), 90f);
    }

    private PanelColors colors(float alpha) {
        int bgAlpha = Math.round(alpha * 120f);
        int headerAlpha = Math.round(alpha * 150f);
        int strokeAlpha = Math.round(alpha * 100f);
        int shadowAlpha = Math.round(alpha * 80f);
        int titleAlpha = Math.round(alpha * 255f);
        int labelAlpha = Math.round(alpha * 155f);

        Themes.Theme theme = Theme.theme();
        int themeAccent = theme != null ? theme.accent() : 0xFFFFFFFF;
        int themeAccentSoft = theme != null ? theme.accentSoft() : themeAccent;

        int bgTopLeft = withAlpha(0x0D0F14, bgAlpha);
        int bgTopRight = withAlpha(0x101218, bgAlpha);
        int bgBottomLeft = withAlpha(0x08090C, bgAlpha);
        int bgBottomRight = withAlpha(0x0D0F14, bgAlpha);
        int headerTopLeft = withAlpha(0x14171F, headerAlpha);
        int headerTopRight = withAlpha(0x181B24, headerAlpha);
        int headerBottomLeft = withAlpha(0x10131A, headerAlpha);
        int headerBottomRight = withAlpha(0x14171F, headerAlpha);
        int stroke = withAlpha(0x252A36, strokeAlpha);
        int strokeSoft = withAlpha(0x3A4A5A, Math.round(alpha * 150f));
        int surface = withAlpha(0x1A1D24, Math.round(alpha * 160f));
        int surfaceHover = withAlpha(0x1A1F28, Math.round(alpha * 200f));
        int accent = withAlpha(themeAccent, titleAlpha);
        int accentSoft = withAlpha(themeAccentSoft, Math.round(alpha * 180f));
        return new PanelColors(
                bgTopLeft, bgTopRight, bgBottomRight, bgBottomLeft,
                headerTopLeft, headerTopRight, headerBottomRight, headerBottomLeft,
                stroke, strokeSoft,
                withAlpha(0x060810, shadowAlpha),
                withAlpha(0xFFFFFF, titleAlpha),
                withAlpha(0xFFFFFF, labelAlpha),
                withAlpha(0x808890, titleAlpha),
                withAlpha(0x606878, labelAlpha),
                surface, surfaceHover, accent, accentSoft
        );
    }

    private void drawDimmer(AltManagerController controller) {
        Themes.Theme t = Theme.theme();
        int soft = withAlpha(HudRenderUtil.mixColor(t.windowBg(), 0xFF000000, 0.46f), 140);
        int deep = withAlpha(HudRenderUtil.mixColor(t.windowBg(), 0xFF000000, 0.68f), 180);
        Renderer2D.COLOR.quad(0, 0, controller.getFixedWidth(), controller.getFixedHeight(), soft, soft, deep, deep);
    }
}
