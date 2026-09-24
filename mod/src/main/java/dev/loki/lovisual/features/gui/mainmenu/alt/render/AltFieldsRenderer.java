package dev.loki.lovisual.features.gui.mainmenu.alt.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltManagerController;
import dev.loki.lovisual.features.gui.mainmenu.alt.auth.AltMicrosoftAuth;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltPanelAnims;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PanelColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.*;

/**
 * Renderer for the alt manager input widgets: microsoft status field, nickname field and add button.
 */
final class AltFieldsRenderer {
    private final TextRenderer bodyRenderer;

    AltFieldsRenderer(TextRenderer bodyRenderer) {
        this.bodyRenderer = bodyRenderer;
    }

    void renderMicrosoftField(AltPanelAnims anims, AltMicrosoftAuth auth, float x, float y, float w, float h, PanelColors c) {
        float hoverAnim = AnimationUtility.easeOutCubic(anims.fieldHover());
        int fillTop = HudRenderUtil.mixColor(withAlpha(c.surface(), 188), withAlpha(c.surfaceHover(), 208), hoverAnim * 0.45f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(c.surface(), 176), withAlpha(0xFF0D1118, 196), 0.18f);
        int stroke = HudRenderUtil.mixColor(c.stroke(), auth.isAuthPending() ? c.accentSoft() : c.strokeSoft(), auth.isAuthPending() ? 0.45f : hoverAnim * 0.60f);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        if (auth.isAuthPending()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 180.0);
            int glow = withAlpha(c.accentSoft(), Math.round(22f + 32f * pulse));
            Renderer2D.COLOR.roundedRectGradientQuad(x + 1.2f * SCALE, y + h - 1.6f * SCALE, w - 2.4f * SCALE, 1.1f * SCALE, 0.6f * SCALE, 1f,
                    glow, withAlpha(c.accent(), Math.round(20f + 24f * pulse)), withAlpha(c.accent(), Math.round(20f + 24f * pulse)), glow);
        }

        String display;
        int color;
        if (!auth.getUserCode().isBlank()) {
            display = tr("screen.lovisual.alt_manager.code", auth.getUserCode());
            color = c.title();
        } else if (!auth.getStatus().isBlank()) {
            display = auth.getStatus();
            color = auth.isAuthPending() ? c.title() : c.muted();
        } else {
            display = tr("screen.lovisual.alt_manager.ready_to_sign_in");
            color = c.mutedLabel();
        }
        draw(bodyRenderer, ellipsize(bodyRenderer, display, FIELD_FONT, w - 8f * SCALE), x + 4f * SCALE, y + 4.3f * SCALE, FIELD_FONT, color);
    }

    void renderField(AltManagerController controller, float x, float y, float w, float h, PanelColors c) {
        AltPanelAnims anims = controller.getAnims();
        float hoverAnim = AnimationUtility.easeOutCubic(anims.fieldHover());
        float focusAnim = AnimationUtility.easeOutCubic(anims.fieldFocus());
        int fillTop = HudRenderUtil.mixColor(withAlpha(c.surface(), 188), withAlpha(c.surfaceHover(), 208), hoverAnim * 0.45f + focusAnim * 0.16f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(c.surface(), 176), withAlpha(0xFF0D1118, 196), 0.18f);
        int stroke = controller.isNicknameFocused()
                ? HudRenderUtil.mixColor(c.strokeSoft(), c.accentSoft(), 0.55f)
                : HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), hoverAnim * 0.72f);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, w, h, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, w, h, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);
        if (focusAnim > 0.01f) {
            int glowLeft = withAlpha(c.accentSoft(), Math.round(48f * focusAnim));
            int glowRight = withAlpha(c.accent(), Math.round(28f * focusAnim));
            Renderer2D.COLOR.roundedRectGradientQuad(x + 1.2f * SCALE, y + h - 1.6f * SCALE, w - 2.4f * SCALE, 1.1f * SCALE, 0.6f * SCALE, 1f,
                    glowLeft, glowRight, glowRight, glowLeft);
        }

        String nicknameText = controller.getNickname();
        boolean caretOn = controller.isNicknameFocused() && AnimationUtility.blink(500L);
        String display = nicknameText.isEmpty() && !controller.isNicknameFocused() ? tr("screen.lovisual.alt_manager.enter_nick") : nicknameText + (caretOn ? "|" : "");
        draw(bodyRenderer, display, x + 4f * SCALE, y + 4.3f * SCALE, FIELD_FONT, nicknameText.isEmpty() && !controller.isNicknameFocused() ? c.mutedLabel() : c.title());
    }

    void renderAddButton(AltPanelAnims anims, float x, float y, PanelColors c) {
        float hoverAnim = AnimationUtility.easeOutCubic(anims.addHover());
        float pressAnim = AnimationUtility.easeOutCubic(anims.addPress());
        int fillTop = HudRenderUtil.mixColor(withAlpha(c.surface(), 172), withAlpha(c.surfaceHover(), 202), hoverAnim * 0.72f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(c.surface(), 162), withAlpha(0xFF0D1118, 186), 0.20f);
        int stroke = HudRenderUtil.mixColor(c.stroke(), c.strokeSoft(), hoverAnim * 0.65f + pressAnim * 0.20f);
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, ADD_SIZE, ADD_SIZE, 3f * SCALE, 1f, fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x, y, ADD_SIZE, ADD_SIZE, 3f * SCALE, 1f, 0.5f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.04f), stroke, 90f);

        float cx = x + ADD_SIZE * 0.5f;
        float cy = y + ADD_SIZE * 0.5f;
        float plusLift = (hoverAnim * 0.18f + pressAnim * 0.32f) * SCALE;
        float plusStretch = 1f + hoverAnim * 0.04f + pressAnim * 0.08f;
        Renderer2D.COLOR.roundedRect(cx - 2.5f * SCALE * plusStretch, cy - 0.6f * SCALE - plusLift, 5f * SCALE * plusStretch, 1.2f * SCALE, 0.5f * SCALE, 1f, c.title());
        Renderer2D.COLOR.roundedRect(cx - 0.6f * SCALE, cy - 2.5f * SCALE * plusStretch - plusLift, 1.2f * SCALE, 5f * SCALE * plusStretch, 0.5f * SCALE, 1f, c.title());
    }
}
