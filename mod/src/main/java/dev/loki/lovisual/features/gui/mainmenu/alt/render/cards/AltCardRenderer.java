package dev.loki.lovisual.features.gui.mainmenu.alt.render.cards;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.account.AccountEntry;
import dev.loki.lovisual.features.account.SkinManager;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PanelColors;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.player.PlayerHeadRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.*;

/**
 * Renderer for account widgets: active session panel and a single account card.
 */
final class AltCardRenderer {
    private final AccountConfig accountConfig;
    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;
    private final TextRenderer menuIconRenderer;
    private final TextRenderer guiIconRenderer;

    AltCardRenderer(AccountConfig accountConfig, TextRenderer titleRenderer, TextRenderer bodyRenderer,
                    TextRenderer menuIconRenderer, TextRenderer guiIconRenderer) {
        this.accountConfig = accountConfig;
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
        this.menuIconRenderer = menuIconRenderer;
        this.guiIconRenderer = guiIconRenderer;
    }

    void drawActivePanel(GuiGraphicsExtractor context, float x, float y, PanelColors c) {
        drawCentered(titleRenderer, tr("screen.lovisual.alt_manager.active_session"), x + LEFT_W * 0.5f - 15f * SCALE, y + 6f * SCALE, TITLE_FONT, c.title());

        float cardX = x + 5f * SCALE;
        float cardY = y + 24f * SCALE;
        float cardW = LEFT_W - 10f * SCALE;
        float cardH = 29f * SCALE;
        Renderer2D.COLOR.roundedRectCornersQuad(cardX, cardY, cardW, cardH, LEFT_CARD_R, LEFT_CARD_R, LEFT_CARD_R, LEFT_CARD_R, 1f,
                withAlpha(c.surface(), 154), withAlpha(c.surfaceHover(), 166), withAlpha(0xFF0D1118, 176), withAlpha(0xFF0D1118, 176));
        Renderer2D.COLOR.roundedRectStrokeCorners(cardX, cardY, cardW, cardH, LEFT_CARD_R, LEFT_CARD_R, LEFT_CARD_R, LEFT_CARD_R, 0.55f, HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), 0.34f));

        String activeName = accountConfig.getActiveAccountName();
        if (activeName == null || activeName.isBlank()) {
            drawCentered(bodyRenderer, tr("screen.lovisual.alt_manager.no_account_selected"), cardX + cardW * 0.5f, cardY + 10.2f * SCALE, EMPTY_FONT, c.mutedLabel());
            return;
        }
        float faceX = cardX + 5f * SCALE;
        float faceY = cardY + 2.5f * SCALE;
        Identifier skin = accountConfig.getActiveAccountSkin();
        if (skin == null) skin = SkinManager.getSkin(activeName);

        PlayerHeadRenderer.drawRounded(context, faceX, faceY, ACTIVE_HEAD, 3f * SCALE, skin,
                new RenderColor(c.title()), true, new RenderColor(c.stroke()), 0.7f, false);

        float textX = faceX + ACTIVE_HEAD + 6f * SCALE;
        float textW = Math.max(1f, cardX + cardW - textX - 6f * SCALE);
        draw(bodyRenderer, ellipsize(bodyRenderer, activeName, ACTIVE_NAME_FONT, textW), textX, faceY + 4f * SCALE, ACTIVE_NAME_FONT, c.title());
        draw(bodyRenderer, ellipsize(bodyRenderer, accountConfig.getActiveAccountDate(), ACTIVE_DATE_FONT, textW), textX, faceY + 14f * SCALE, ACTIVE_DATE_FONT, c.muted());
    }

    void renderCard(AltCardsRenderer cards, GuiGraphicsExtractor context, AccountEntry entry, float mouseX, float mouseY,
                    float x, float y, float w, float listY, float listH, PanelColors c) {
        String key = entry.getName();
        boolean inList = mouseY >= listY && mouseY <= listY + listH;
        float hoverAnim = AnimationUtility.easeOutCubic(cards.cardHoverAnims.getOrDefault(key, 0f));
        float activeAnim = AnimationUtility.easeOutCubic(cards.cardActiveAnims.getOrDefault(key, 0f));
        float pressAnim = AnimationUtility.easeOutCubic(cards.cardPressAnims.getOrDefault(key, 0f));
        Themes.Theme theme = Theme.theme();
        int activeAccent = withAlpha(theme.accent(), Math.round(165f * activeAnim));

        int fillBase = HudRenderUtil.mixColor(c.surface(), c.surfaceHover(), 0.38f * hoverAnim + 0.10f * pressAnim);
        int fillTop = HudRenderUtil.mixColor(fillBase, c.surfaceHover(), 0.03f * pressAnim);
        int fillBottom = HudRenderUtil.mixColor(fillBase, 0xFF05070A, 0.18f);
        int stroke = HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), 0.45f * hoverAnim);
        stroke = HudRenderUtil.mixColor(stroke, activeAccent, 0.38f * activeAnim + 0.10f * pressAnim);
        Renderer2D.COLOR.roundedRectSoftShadow(x, y, w, CARD_H, 4f * SCALE, 8f * SCALE, 0.018f + hoverAnim * 0.018f + activeAnim * 0.010f, c.shadow());
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, CARD_H, 4f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        renderActiveCardMarker(x, y, w, CARD_H, activeAnim, theme);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, CARD_H, 4f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f + hoverAnim * 0.03f), stroke, 90f);

        PlayerHeadRenderer.drawRounded(context, x + 7f * SCALE, y + 7f * SCALE, CARD_HEAD, 3f * SCALE, SkinManager.getSkin(entry.getName()),
                new RenderColor(c.title()), true, null, 0f, false);

        draw(bodyRenderer, ellipsize(bodyRenderer, entry.getName(), CARD_NAME_FONT, w - CARD_HEAD - 45f * SCALE), x + 37f * SCALE, y + 9f * SCALE, CARD_NAME_FONT, c.title());
        draw(bodyRenderer, entry.getDate(), x + 37f * SCALE, y + 20f * SCALE, CARD_DATE_FONT, c.muted());
        renderAuthBadge(entry, x + 37f * SCALE, y + 30f * SCALE, c);

        float btnY = y + CARD_H - CARD_BTN - 5f * SCALE;
        float pinX = x + w - CARD_BTN * 2f - 8f * SCALE;
        float delX = x + w - CARD_BTN - 5f * SCALE;
        boolean pinHover = inList && inside(mouseX, mouseY, pinX, btnY, CARD_BTN, CARD_BTN);
        boolean delHover = inList && inside(mouseX, mouseY, delX, btnY, CARD_BTN, CARD_BTN);

        float pinHoverAnim = AnimationUtility.easeOutCubic(cards.pinHoverAnims.getOrDefault(key, pinHover ? 1f : 0f));
        float pinPressAnim = AnimationUtility.easeOutBack(cards.pinPressAnims.getOrDefault(key, 0f), 0.95f);
        int pinFill = entry.isPinned()
                ? HudRenderUtil.mixColor(withAlpha(c.accentSoft(), 190), withAlpha(c.accent(), 72), 0.18f + pinHoverAnim * 0.12f)
                : HudRenderUtil.mixColor(withAlpha(c.surface(), 170), withAlpha(c.surfaceHover(), 196), pinHoverAnim * 0.65f);
        int pinStroke = entry.isPinned()
                ? HudRenderUtil.mixColor(c.accent(), 0xFFFFFFFF, pinHoverAnim * 0.12f)
                : HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), pinHoverAnim * 0.70f);
        Renderer2D.COLOR.roundedRectGradientQuad(pinX, btnY, CARD_BTN, CARD_BTN, 3f * SCALE, 1f, pinFill, pinFill, pinFill, pinFill);
        Renderer2D.COLOR.roundedRectStrokeGradient(pinX, btnY, CARD_BTN, CARD_BTN, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(pinStroke, 0xFFFFFFFF, 0.04f), pinStroke, 90f);
        float pinContentLift = (pinHoverAnim * 0.35f + pinPressAnim * 0.6f) * SCALE;
        drawCentered(menuIconRenderer, "c", pinX + CARD_BTN * 0.5f, btnY + 1.5f * SCALE - pinContentLift, PIN_ICON_FONT, entry.isPinned() ? c.accent() : withAlpha(0xC0C8D4, 255));

        float delHoverAnim = AnimationUtility.easeOutCubic(cards.deleteHoverAnims.getOrDefault(key, delHover ? 1f : 0f));
        float delPressAnim = AnimationUtility.easeOutBack(cards.deletePressAnims.getOrDefault(key, 0f), 0.95f);
        int delFillTop = HudRenderUtil.mixColor(withAlpha(c.surface(), 150), withAlpha(0x5A2424, 210), 0.30f + delHoverAnim * 0.45f);
        int delFillBottom = HudRenderUtil.mixColor(withAlpha(c.surface(), 140), withAlpha(0x2E1212, 220), 0.20f + delHoverAnim * 0.55f);
        int delStroke = HudRenderUtil.mixColor(c.stroke(), withAlpha(0xA24E4E, 230), 0.25f + delHoverAnim * 0.65f);
        Renderer2D.COLOR.roundedRectGradientQuad(delX, btnY, CARD_BTN, CARD_BTN, 3f * SCALE, 1f, delFillTop, delFillTop, delFillBottom, delFillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(delX, btnY, CARD_BTN, CARD_BTN, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(delStroke, 0xFFFFFFFF, 0.04f), delStroke, 90f);
        float delContentLift = (delHoverAnim * 0.35f + delPressAnim * 0.6f) * SCALE;
        drawCentered(guiIconRenderer, "O", delX + CARD_BTN * 0.5f, btnY + 0.5f * SCALE - delContentLift, DELETE_ICON_FONT,
                HudRenderUtil.mixColor(withAlpha(0xC0C8D4, 255), withAlpha(0xFFB0B0, 255), 0.45f + delHoverAnim * 0.45f));
    }

    private void renderActiveCardMarker(float x, float y, float w, float h, float active, Themes.Theme theme) {
        float reveal = AnimationUtility.clamp01(active);
        if (reveal <= 0.001f) return;

        float markerW = CARD_MARKER_W * reveal;
        if (markerW <= 0.01f) return;

        int accent = withAlpha(theme.accent(), Math.round(255f * reveal));
        boolean clipped = ScissorFunction.pushRaw(x, y, markerW, h);
        if (!clipped) return;
        try {
            Renderer2D.COLOR.roundedRectCornersQuad(x, y, w, h, LEFT_CARD_R, LEFT_CARD_R, LEFT_CARD_R, LEFT_CARD_R, 1f, accent, accent, accent, accent);
        } finally {
            ScissorFunction.pop();
        }
    }

    private void renderAuthBadge(AccountEntry entry, float x, float y, PanelColors c) {
        boolean microsoft = entry != null && entry.isMicrosoft();
        String label = microsoft ? tr("screen.lovisual.alt_manager.badge.microsoft") : tr("screen.lovisual.alt_manager.badge.offline");
        float padX = 3.2f * SCALE;
        float badgeW = width(bodyRenderer, label, BADGE_FONT) + padX * 2f;
        float badgeH = 7f * SCALE;
        int fillTop = microsoft ? withAlpha(0x20394A, 172) : withAlpha(c.surface(), 150);
        int fillBottom = microsoft ? withAlpha(0x132732, 184) : withAlpha(0x11151B, 160);
        int stroke = microsoft ? withAlpha(0x527A8A, 190) : c.stroke();
        int text = microsoft ? withAlpha(0xDDEEFF, 235) : c.muted();
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, badgeW, badgeH, 2.2f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, badgeW, badgeH, 2.2f * SCALE, 1f, 0.45f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.05f), stroke, 90f);
        draw(bodyRenderer, label, x + padX, y + 1.9f * SCALE, BADGE_FONT, text);
    }
}
