package dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.PANEL_RADIUS;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud;

public final class TargetHudPanelRenderer {

    private TargetHudPanelRenderer() {
    }

    public static void drawBackground(TargetHud element,
                               Renderer2D renderer,
                               float x,
                               float y,
                               float width,
                               float height,
                               float radius,
                               float alphaFactor,
                               boolean glassEnabled) {
        if (glassEnabled) {
            renderer.roundedRect(x, y, width, height, radius, 1.0f, HudRenderUtil.glassPanelBackground(alphaFactor));
            return;
        }

        int primary = HudRenderUtil.scaleAlpha(element.uiBgPrimary, alphaFactor);
        int secondary = HudRenderUtil.scaleAlpha(element.uiBgSecondary, alphaFactor);

        if (element.isThemeMode()) {
            HudRenderUtil.drawHudBackground(renderer, x, y, width, height, radius, 1.0f, primary, true);
        } else {
            renderer.roundedRectGradientQuad(x, y, width, height, radius, 1.0f, primary, secondary, primary, secondary);
        }

        renderer.roundedRectStroke(
                x, y, width, height, radius, 1.0f,
                0.35f * radius / PANEL_RADIUS,
                HudRenderUtil.scaleAlpha(element.uiStroke, alphaFactor)
        );
    }

    public static void drawHitPulse(TargetHud element,
                             Renderer2D renderer,
                             float x,
                             float y,
                             float width,
                             float height,
                             float radius,
                             float alphaFactor,
                             float pulse) {
        if (!element.hitAnim.get() || pulse <= 0.001f) return;

        float scaledPulse = pulse * element.hitPulse.get().floatValue();
        float spread = 6.0f * (0.65f + scaledPulse);
        int outerColor = HudRenderUtil.scaleAlpha(element.uiHitOuter, 0.58f * scaledPulse * alphaFactor);
        int innerColor = HudRenderUtil.scaleAlpha(element.uiHitInner, 0.92f * scaledPulse * alphaFactor);

        renderer.roundedRectStroke(
                x - spread * 0.4f,
                y - spread * 0.4f,
                width + spread * 0.8f,
                height + spread * 0.8f,
                radius + spread * 0.15f,
                1.0f,
                0.85f + scaledPulse,
                outerColor
        );
        renderer.roundedRectStroke(x, y, width, height, radius, 1.0f, 0.9f + scaledPulse, innerColor);
    }

    public static void drawBlur(TargetHud element,
                         float x,
                         float y,
                         float width,
                         float height,
                         float radius,
                         int tintRgb,
                         float alphaFactor) {
        if (!element.hasEffect() || width <= 0.0f || height <= 0.0f) return;
        float quality = element.hud.getBlurRadius();
        float brightness = 1.0f;
        float alpha = (element.blurAlpha.get() / 255.0f) * alphaFactor;
        Renderer2D.COLOR.blurRect(x, y, width, height, radius, quality, brightness, alpha, 0xFFFFFF);
    }

    public static void drawGlass(TargetHud element,
                          float x,
                          float y,
                          float width,
                          float height,
                          float radius,
                          float alphaFactor) {
        float blurStrength = (element.blurAlpha.get() / 255.0f) * alphaFactor;
        float glassAlpha = alphaFactor;
        float glassScale = PANEL_RADIUS <= 0.0f ? 1.0f : radius / PANEL_RADIUS;
        HudRenderUtil.drawLiquidGlass(x, y, width, height, radius, glassScale, true, blurStrength, glassAlpha);
    }

    public static void drawGlassCard(TargetHud element,
                              float x,
                              float y,
                              float width,
                              float height,
                              float radius,
                              float scaleFactor,
                              float alphaFactor) {
        float blurStrength = (element.blurAlpha.get() / 255.0f) * alphaFactor;
        HudRenderUtil.drawLiquidGlass(x, y, width, height, radius, scaleFactor, true, blurStrength, alphaFactor);
    }
}
