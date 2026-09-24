package dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.render.parts;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.ARC_CENTER_Y;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.ARC_RADIUS;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.ARC_RIGHT_INSET;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.ARC_TAIL_FADE_DEGREES;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.ARC_THICKNESS;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.CONTENT_X_OFFSET;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.CONTENT_Y_OFFSET;


import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud.*;
import dev.loki.lovisual.features.gui.hud.draggable.impl.targethud.TargetHud;

public final class TargetHudArcRenderer {

    private TargetHudArcRenderer() {
    }

    public static void drawHealthArc(TargetHud element,
                              Renderer2D renderer,
                              TextRenderer textRenderer,
                              float x,
                              float y,
                              float panelWidth,
                              float scaleFactor,
                              float alphaFactor,
                              String hpText,
                              int hpTextColor,
                              long now) {
        float centerX = x + panelWidth - ARC_RIGHT_INSET * scaleFactor + CONTENT_X_OFFSET * scaleFactor;
        float centerY = y + ARC_CENTER_Y * scaleFactor + CONTENT_Y_OFFSET * scaleFactor;
        float radius = ARC_RADIUS * scaleFactor;
        float thickness = ARC_THICKNESS * scaleFactor;
        float softness = Math.max(0.35f, 0.45f * scaleFactor);

        int ringBackground = HudRenderUtil.scaleAlpha(
                HudRenderUtil.mixColor(element.uiBarBackground, 0xFF000000, 0.34f),
                alphaFactor * 0.55f
        );
        renderer.arcStroke(centerX, centerY, radius, thickness, 0.0f, 360.0f, softness, ringBackground);

        float healthPercent = AnimationUtility.clamp01(element.healthAnimation);

        float healthSweep = 360.0f * healthPercent;
        if (healthSweep > 0.001f) {
            float healthFade = tailFade(healthSweep, ARC_TAIL_FADE_DEGREES);
            drawAnimatedArcGradient(
                    renderer,
                    centerX,
                    centerY,
                    radius,
                    thickness,
                    0.0f,
                    healthSweep,
                    softness,
                    alphaFactor * healthFade,
                    element.uiHealthStart,
                    element.uiHealthEnd,
                    element.uiHealthGradientAngle,
                    1700f,
                    now - element.waveStartMs,
                    0.0f
            );
        }

        float absorptionPercent = AnimationUtility.clamp01(element.absorptionAnimation);
        float absorptionSweep = 360.0f * absorptionPercent;
        if (absorptionSweep > 0.001f) {
            float absorptionThickness = Math.max(1.6f * scaleFactor, thickness * 0.9f);
            float absorptionFade = tailFade(absorptionSweep, ARC_TAIL_FADE_DEGREES);
            drawAnimatedArcGradient(
                    renderer,
                    centerX,
                    centerY,
                    radius,
                    absorptionThickness,
                    0.0f,
                    absorptionSweep,
                    softness,
                    alphaFactor * absorptionFade,
                    element.uiAbsorptionStart,
                    element.uiAbsorptionEnd,
                    element.uiAbsorptionGradientAngle,
                    1300f,
                    now - element.waveStartMs,
                    1.35f
            );
        }

        TextRenderer hpRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        float hpScale = ARC_HP_TEXT_SCALE * scaleFactor;
        hpRenderer.begin(hpScale, false, false);
        try {
            float textWidth = (float) hpRenderer.getWidth(hpText, false);
            float textHeight = (float) hpRenderer.getHeight(false);
            hpRenderer.render(
                    hpText,
                    centerX - textWidth * 0.5f,
                    centerY - textHeight * 0.5f,
                    new RenderColor(hpTextColor),
                    false
            );
        } finally {
            hpRenderer.end();
        }
    }

    public static void drawAnimatedArcGradient(Renderer2D renderer,
                                        float centerX,
                                        float centerY,
                                        float radius,
                                        float thickness,
                                        float startAngle,
                                        float sweep,
                                        float softness,
                                        float alphaFactor,
                                        int startColor,
                                        int endColor,
                                        float baseAngle,
                                        float speedMs,
                                        long elapsedMs,
                                        float phaseOffset) {
        float safeSpeed = Math.max(1.0f, speedMs);
        float phase = (elapsedMs / safeSpeed) * (float) Math.PI * 2.0f + phaseOffset;
        float animatedAngle = baseAngle
                + (elapsedMs / safeSpeed) * 92.0f
                + (float) Math.sin(phase * 0.63f) * 26.0f;
        float pulseA = ((float) Math.sin(phase) + 1.0f) * 0.5f;
        float pulseB = ((float) Math.sin(phase + 1.95f) + 1.0f) * 0.5f;

        int c0 = HudRenderUtil.mixColor(startColor, endColor, pulseA * 0.24f);
        int c1 = HudRenderUtil.mixColor(endColor, startColor, pulseB * 0.18f);
        int drawStart = HudRenderUtil.scaleAlpha(c0, alphaFactor);
        int drawEnd = HudRenderUtil.scaleAlpha(c1, alphaFactor);
        float offsetPx = (float) Math.sin(phase * 0.71f) * radius * 0.58f;

        renderer.arcStrokeGradient(
                centerX,
                centerY,
                radius,
                thickness,
                startAngle,
                startAngle + sweep,
                softness,
                drawStart,
                drawEnd,
                animatedAngle,
                offsetPx
        );

    }

    public static int[] buildWaveColors(long elapsedMs,
                                 float waveSpeedMs,
                                 float alphaFactor,
                                 int startColor,
                                 int endColor) {
        float phase = (elapsedMs % (long) waveSpeedMs) / waveSpeedMs * (float) Math.PI * 2.0f;
        int[] colors = new int[4];
        for (int i = 0; i < 2; i++) {
            float wave = ((float) Math.sin(phase - i * 1.5f) + 1.0f) * 0.5f;
            int mixed = HudRenderUtil.mixColor(startColor, endColor, wave);
            colors[i * 2] = HudRenderUtil.scaleAlpha(mixed, alphaFactor);
            colors[i * 2 + 1] = colors[i * 2];
        }
        return colors;
    }

    public static float tailFade(float visibleLength, float fadeLength) {
        if (fadeLength <= 0.0f) return visibleLength > 0.0f ? 1.0f : 0.0f;
        return AnimationUtility.smoothstep(AnimationUtility.clamp01(visibleLength / fadeLength));
    }
}
