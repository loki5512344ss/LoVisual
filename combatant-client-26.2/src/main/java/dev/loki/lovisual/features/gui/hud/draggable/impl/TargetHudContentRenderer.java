/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.FontInfo;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.player.PlayerHealthResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud.*;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHudFormatter.*;

final class TargetHudContentRenderer {

    private TargetHudContentRenderer() {
    }

    static void drawContent(TargetHud element,
                            Renderer2D renderer,
                            TextRenderer textRenderer,
                            LivingEntity target,
                            float x,
                            float y,
                            float drawWidth,
                            float scaleFactor,
                            float alphaFactor,
                            long now,
                            float dt) {
        Minecraft mc = Minecraft.getInstance();
        float contentX = x + (FACE_X + FACE_SIZE + CONTENT_GAP + CONTENT_X_OFFSET) * scaleFactor;
        float nameY = y + (NAME_Y + CONTENT_Y_OFFSET) * scaleFactor;

        PlayerHealthResolver.HealthSnapshot resolved = PlayerHealthResolver.resolve(target);
        float health = Math.max(0.0f, resolved.totalHealth() - resolved.absorption());
        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float absorption = Math.max(0.0f, resolved.absorption());

        float targetDisplayHealth = health + absorption;
        element.displayedHealth = AnimationUtility.approach(element.displayedHealth, targetDisplayHealth, dt, HEALTH_SPEED);
        element.displayedHealth = AnimationUtility.snap(element.displayedHealth, targetDisplayHealth, 0.02f);
        if (targetDisplayHealth <= HEALTH_ZERO_CLAMP && element.displayedHealth <= HEALTH_ZERO_SNAP_THRESHOLD) {
            element.displayedHealth = 0.0f;
        }

        float targetHealthFraction = AnimationUtility.clamp01(health / maxHealth);
        float targetAbsorptionFraction = AnimationUtility.clamp01(absorption / maxHealth);
        element.healthAnimation = AnimationUtility.approach(element.healthAnimation, targetHealthFraction, dt, BAR_SPEED);
        element.healthAnimation = AnimationUtility.snap(element.healthAnimation, targetHealthFraction, PROGRESS_SNAP_EPSILON);
        if (targetHealthFraction > element.trailAnimation) {
            element.trailAnimation = targetHealthFraction;
        }
        element.trailAnimation = AnimationUtility.approach(element.trailAnimation, targetHealthFraction, dt, BAR_SPEED - 0.5f);
        element.trailAnimation = AnimationUtility.snap(element.trailAnimation, targetHealthFraction, PROGRESS_SNAP_EPSILON);
        element.absorptionAnimation = AnimationUtility.approach(element.absorptionAnimation, targetAbsorptionFraction, dt, BAR_SPEED);
        element.absorptionAnimation = AnimationUtility.snap(element.absorptionAnimation, targetAbsorptionFraction, PROGRESS_SNAP_EPSILON);

        boolean selfTarget = target == mc.player;
        if (!selfTarget) {
            float targetDistance = mc.player != null ? mc.player.distanceTo(target) : 0.0f;
            element.displayedDistance = AnimationUtility.approach(element.displayedDistance, targetDistance, dt, DISTANCE_SPEED);
            element.displayedDistance = AnimationUtility.snap(element.displayedDistance, targetDistance, 0.01f);
        }

        String hpText = formatHealth(formatDisplayHealth(element.displayedHealth));
        String distanceText = selfTarget ? "" : formatDistance(element.displayedDistance) + "m";
        String moduleStatus = resolveModuleStatus();
        String metaText = moduleStatus != null ? moduleStatus : distanceText;
        float panelWidth = drawWidth - PANEL_INSET * 4f * scaleFactor;
        TextRenderer labelRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        TextRenderer metaRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        boolean arcMode = element.isArcHealthMode();
        float hpTextScale = (arcMode ? ARC_HP_TEXT_SCALE : HP_TEXT_SCALE) * scaleFactor;
        float hpWidth = TargetHudTextRenderer.measureText(labelRenderer, hpText, hpTextScale);
        float contentReserve = arcMode
                ? ARC_RESERVED_WIDTH * scaleFactor
                : hpWidth + 12.0f * scaleFactor;
        float maxNameWidth = Math.max(12.0f * scaleFactor, panelWidth - FACE_SIZE * scaleFactor - CONTENT_GAP * scaleFactor - contentReserve);
        String rawName = target.getName().getString();
        String name = TargetHudTextRenderer.trimText(labelRenderer, rawName, maxNameWidth, NAME_TEXT_SCALE * scaleFactor);
        String meta = TargetHudTextRenderer.trimText(metaRenderer, metaText, maxNameWidth, DISTANCE_TEXT_SCALE * scaleFactor);
        float nameScale = NAME_TEXT_SCALE * scaleFactor;
        float rawNameWidth = TargetHudTextRenderer.measureText(labelRenderer, rawName, nameScale);
        boolean useMarquee = element.marquee.get() && rawNameWidth > maxNameWidth * 1.01f;

        int nameColor = HudRenderUtil.scaleAlpha(element.uiTextPrimary, alphaFactor);
        int hpColor = HudRenderUtil.scaleAlpha(element.uiTextSecondary, alphaFactor);
        int distanceColor = HudRenderUtil.scaleAlpha(element.uiTextSecondary, alphaFactor);

        if (useMarquee) {
            TargetHudTextRenderer.drawScrollingText(
                    labelRenderer,
                    rawName,
                    contentX,
                    nameY,
                    rawNameWidth,
                    maxNameWidth,
                    nameScale,
                    scaleFactor,
                    alphaFactor,
                    nameColor
            );
        } else {
            labelRenderer.begin(nameScale, false, false);
            try {
                labelRenderer.render(name, contentX, nameY, new RenderColor(nameColor), false);
            } finally {
                labelRenderer.end();
            }
        }

        if (arcMode) {
            metaRenderer.begin(DISTANCE_TEXT_SCALE * scaleFactor, false, false);
            try {
                metaRenderer.render(
                        meta,
                        contentX,
                        nameY + DISTANCE_Y_OFFSET * scaleFactor,
                        new RenderColor(distanceColor),
                        false
                );
            } finally {
                metaRenderer.end();
            }

            TargetHudArcRenderer.drawHealthArc(element, renderer, textRenderer, x, y, panelWidth, scaleFactor, alphaFactor, hpText, hpColor, now);
            return;
        }

        labelRenderer.begin(HP_TEXT_SCALE * scaleFactor, false, false);
        try {
            float hpX = x + panelWidth - hpWidth + CONTENT_X_OFFSET * scaleFactor;
            labelRenderer.render(hpText, hpX, nameY, new RenderColor(hpColor), false);
        } finally {
            labelRenderer.end();
        }

        float barX = contentX;
        float barY = nameY + BAR_Y_OFFSET * scaleFactor;
        float barWidth = BAR_WIDTH * scaleFactor;
        float barHeight = BAR_HEIGHT * scaleFactor;
        float barRadius = BAR_RADIUS * scaleFactor;

        renderer.roundedRect(
                barX, barY, barWidth, barHeight, barRadius, 1.0f,
                HudRenderUtil.scaleAlpha(element.uiBarBackground, alphaFactor)
        );

        float trailPercent = AnimationUtility.clamp01(element.trailAnimation);
        float healthPercent = AnimationUtility.clamp01(element.healthAnimation);
        if (trailPercent > healthPercent + 0.001f) {
            float trailFade = TargetHudArcRenderer.tailFade(BAR_WIDTH * trailPercent, BAR_TAIL_FADE_LENGTH);
            int trailColor = HudRenderUtil.scaleAlpha(element.uiTrail, alphaFactor * trailFade);
            renderer.roundedProgressRectGradient(
                    barX, barY, barWidth, barHeight, barRadius, 1.0f,
                    trailPercent, false, trailColor, trailColor, 0.0f, 0.0f
            );
        }

        long elapsedMs = Math.max(0L, now - element.waveStartMs);

        if (healthPercent > 0.0001f) {
            float healthFade = TargetHudArcRenderer.tailFade(BAR_WIDTH * healthPercent, BAR_TAIL_FADE_LENGTH);
            int[] healthWave = TargetHudArcRenderer.buildWaveColors(elapsedMs, 1500f, alphaFactor * healthFade, element.uiHealthStart, element.uiHealthEnd);
            renderer.roundedProgressRectGradient(
                    barX, barY, barWidth, barHeight, barRadius, 1.0f,
                    healthPercent, false, healthWave[0], healthWave[2], 90.0f, 0.0f
            );
        }

        float absorptionPercent = AnimationUtility.clamp01(element.absorptionAnimation);
        if (absorptionPercent > 0.0001f) {
            float absorptionFade = TargetHudArcRenderer.tailFade(BAR_WIDTH * absorptionPercent, BAR_TAIL_FADE_LENGTH);
            int[] absorptionWave = TargetHudArcRenderer.buildWaveColors(elapsedMs, 1200f, alphaFactor * absorptionFade, element.uiAbsorptionStart, element.uiAbsorptionEnd);
            renderer.roundedProgressRectGradient(
                    barX, barY, barWidth, barHeight, barRadius, 1.0f,
                    absorptionPercent, false, absorptionWave[0], absorptionWave[2], 90.0f, 0.0f
            );
        }
    }

    private static String resolveModuleStatus() {
        return null;
    }
}
