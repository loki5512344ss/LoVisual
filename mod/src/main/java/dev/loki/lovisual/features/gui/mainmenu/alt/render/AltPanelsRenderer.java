package dev.loki.lovisual.features.gui.mainmenu.alt.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltManagerController;
import dev.loki.lovisual.features.gui.mainmenu.alt.auth.AltMicrosoftAuth;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltPanelAnims;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PanelColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.*;

/**
 * Renderer for the alt manager top-left panel: mode toggle and action buttons.
 */
final class AltPanelsRenderer {
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;
    private final TextRenderer guiIconRenderer;
    private final TextRenderer iconRenderer;
    private final AltFieldsRenderer fields;

    AltPanelsRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer, TextRenderer guiIconRenderer, TextRenderer iconRenderer) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
        this.guiIconRenderer = guiIconRenderer;
        this.iconRenderer = iconRenderer;
        this.fields = new AltFieldsRenderer(bodyRenderer);
    }

    void draw(AltManagerController controller, AltMicrosoftAuth auth,
              float mouseX, float mouseY, AltUiMetrics.Layout l, PanelColors c) {
        controller.getAnims().updateHover(mouseX, mouseY, l, controller);
        renderTopPanel(controller, auth, mouseX, mouseY, l.leftX(), l.topY(), c);
    }

    private void renderTopPanel(AltManagerController controller, AltMicrosoftAuth auth,
                                float mouseX, float mouseY, float x, float y, PanelColors c) {
        boolean microsoftMode = controller.isMicrosoftMode();
        AltPanelAnims anims = controller.getAnims();

        drawCentered(titleRenderer, tr("screen.lovisual.alt_manager.account_panel"), x + LEFT_W * 0.5f - 8f * SCALE, y + 7f * SCALE, TITLE_FONT, c.title());
        renderModeToggle(microsoftMode, mouseX, mouseY, x + 5f * SCALE, y + 27f * SCALE, LEFT_W - 10f * SCALE, 13f * SCALE, c, anims);

        AltUiMetrics.draw(bodyRenderer, microsoftMode ? tr("screen.lovisual.alt_manager.microsoft_login") : tr("screen.lovisual.alt_manager.nickname"), x + 5f * SCALE, y + 47f * SCALE, LABEL_FONT, c.label());

        float fieldX = x + 5f * SCALE;
        float fieldY = y + 57f * SCALE;
        float fieldW = LEFT_W - 10f * SCALE - ADD_SIZE - 3f * SCALE;
        if (microsoftMode) {
            fields.renderMicrosoftField(anims, auth, fieldX, fieldY, fieldW, FIELD_H, c);
        } else {
            fields.renderField(controller, fieldX, fieldY, fieldW, FIELD_H, c);
        }

        float addX = fieldX + fieldW + 3f * SCALE;
        fields.renderAddButton(anims, addX, fieldY, c);

        float buttonX = x + 5f * SCALE;
        float buttonW = LEFT_W - 10f * SCALE;
        float randomY = fieldY + FIELD_H + 5f * SCALE;
        float openSiteY = randomY + TOP_BUTTON_H + 5f * SCALE;
        float clearY = (microsoftMode ? openSiteY : randomY) + TOP_BUTTON_H + 5f * SCALE;

        String authAction = microsoftMode
                ? (auth.getUserCode().isBlank()
                        ? (auth.isAuthPending() ? tr("screen.lovisual.alt_manager.waiting") : tr("screen.lovisual.alt_manager.authorize"))
                        : tr("screen.lovisual.alt_manager.copy_code"))
                : tr("screen.lovisual.alt_manager.random");
        if (microsoftMode) {
            renderSvgAction(buttonX, randomY, buttonW, TOP_BUTTON_H, c, false, authAction, "user-check", anims.randomHover(), anims.randomPress());
        } else {
            renderAction(buttonX, randomY, buttonW, TOP_BUTTON_H, c, false, authAction, iconRenderer, "R", anims.randomHover(), anims.randomPress());
        }
        if (microsoftMode) {
            renderSvgAction(buttonX, openSiteY, buttonW, TOP_BUTTON_H, c, false, tr("screen.lovisual.alt_manager.open_site"), "external-link", anims.openSiteHover(), anims.openSitePress());
        }
        renderAction(buttonX, clearY, buttonW, TOP_BUTTON_H, c, true, tr("screen.lovisual.alt_manager.clear_all"), guiIconRenderer, "O", anims.clearHover(), anims.clearPress());
    }

    private void renderModeToggle(boolean microsoftMode, float mouseX, float mouseY, float x, float y, float w, float h, PanelColors c, AltPanelAnims anims) {
        float gap = 2f * SCALE;
        float itemW = (w - gap) * 0.5f;
        renderModeButton(x, y, itemW, h, c, tr("screen.lovisual.alt_manager.mode.offline"), !microsoftMode,
                inside(mouseX, mouseY, x, y, itemW, h), anims.offlineModeHover(), anims.offlineModePress());
        renderModeButton(x + itemW + gap, y, itemW, h, c, tr("screen.lovisual.alt_manager.mode.microsoft"), microsoftMode,
                inside(mouseX, mouseY, x + itemW + gap, y, itemW, h), anims.microsoftModeHover(), anims.microsoftModePress());
    }

    private void renderModeButton(float x, float y, float w, float h, PanelColors c, String label, boolean selected,
                                  boolean hovered, float hoverValue, float pressValue) {
        float hoverAnim = AnimationUtility.easeOutCubic(hoverValue);
        float pressAnim = AnimationUtility.easeOutCubic(pressValue);
        float activeAnim = selected ? 1f : 0f;
        int fillTop = HudRenderUtil.mixColor(withAlpha(c.surface(), 170), withAlpha(c.surfaceHover(), 210), hoverAnim * 0.42f + activeAnim * 0.38f + pressAnim * 0.10f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(c.surface(), 158), withAlpha(0x1A2418, 210), activeAnim * 0.24f + hoverAnim * 0.16f);
        int stroke = HudRenderUtil.mixColor(c.stroke(), selected ? c.accentSoft() : c.strokeSoft(), activeAnim * 0.70f + hoverAnim * 0.30f + pressAnim * 0.10f);
        int text = selected
                ? HudRenderUtil.mixColor(c.title(), c.accent(), 0.28f)
                : HudRenderUtil.mixColor(c.muted(), c.title(), hoverAnim * 0.35f);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        float lift = (hovered ? hoverAnim : 0f) * 0.35f * SCALE + pressAnim * 0.35f * SCALE;
        drawCentered(bodyRenderer, label, x + w * 0.5f, y + 4.1f * SCALE - lift, BUTTON_FONT, text);
    }

    private void renderAction(float x, float y, float w, float h, PanelColors c, boolean destructive, String label, TextRenderer iconRenderer, String glyph, float hoverValue, float pressValue) {
        renderActionFull(x, y, w, h, c, destructive, label, iconRenderer, glyph, null, hoverValue, pressValue);
    }

    private void renderSvgAction(float x, float y, float w, float h, PanelColors c, boolean destructive, String label, String svgName, float hoverValue, float pressValue) {
        renderActionFull(x, y, w, h, c, destructive, label, null, "", svgName, hoverValue, pressValue);
    }

    private void renderActionFull(float x, float y, float w, float h, PanelColors c, boolean destructive, String label, TextRenderer iconRenderer, String glyph, String svgName, float hoverValue, float pressValue) {
        float hoverAnim = AnimationUtility.easeOutCubic(hoverValue);
        float pressAnim = AnimationUtility.easeOutCubic(pressValue);
        int fillTop;
        int fillBottom;
        int stroke;
        int text;
        if (destructive) {
            fillTop = HudRenderUtil.mixColor(withAlpha(0x1A1416, 170), withAlpha(0x4B2224, 212), 0.25f + hoverAnim * 0.45f + pressAnim * 0.12f);
            fillBottom = HudRenderUtil.mixColor(withAlpha(0x120E10, 165), withAlpha(0x261012, 220), 0.20f + hoverAnim * 0.40f + pressAnim * 0.10f);
            stroke = HudRenderUtil.mixColor(withAlpha(0x352A2A, 215), withAlpha(0x8C5054, 235), 0.22f + hoverAnim * 0.58f + pressAnim * 0.12f);
            text = HudRenderUtil.mixColor(withAlpha(0xD0A0A0, 255), withAlpha(0xFFB2B2, 255), 0.24f + hoverAnim * 0.56f + pressAnim * 0.08f);
        } else {
            fillTop = HudRenderUtil.mixColor(withAlpha(c.surface(), 176), withAlpha(0x223746, 212), 0.20f + hoverAnim * 0.44f + pressAnim * 0.10f);
            fillBottom = HudRenderUtil.mixColor(withAlpha(c.surface(), 166), withAlpha(0x13292F, 220), 0.16f + hoverAnim * 0.40f + pressAnim * 0.08f);
            stroke = HudRenderUtil.mixColor(c.stroke(), withAlpha(0x466A76, 228), 0.18f + hoverAnim * 0.50f + pressAnim * 0.10f);
            text = HudRenderUtil.mixColor(withAlpha(0xD0D8E4, 255), withAlpha(0xEEF8FF, 255), 0.18f + hoverAnim * 0.38f + pressAnim * 0.08f);
        }

        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, h, 3f * SCALE, 7f * SCALE, 0.018f + hoverAnim * 0.015f, c.shadow());
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        float contentLift = hoverAnim * 0.65f * SCALE + pressAnim * 0.55f * SCALE;
        AltUiMetrics.draw(bodyRenderer, label, x + 6f * SCALE, y + 5f * SCALE - contentLift, BUTTON_FONT, text);
        if (svgName != null && !svgName.isBlank()) {
            float iconSize = 7.2f * SCALE;
            Renderer2D.COLOR.svg(svgName, x + w - 11.3f * SCALE, y + (h - iconSize) * 0.5f - contentLift, iconSize, iconSize, SvgRenderOptions.overrideColor(text));
        } else {
            AltUiMetrics.draw(iconRenderer, glyph, x + (destructive ? 77f : 75f) * SCALE, y + (destructive ? 2.2f : 3.1f) * SCALE - contentLift, ACTION_ICON_FONT, text);
        }
    }
}
