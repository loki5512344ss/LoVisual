/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.profiler.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.RenderProfiler2D;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.pvp.opponents.TotemPopSnapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud.*;
import static dev.loki.lovisual.features.theme.Theme.theme;

final class TargetHudRenderer {

    private TargetHudRenderer() {
    }

    static void render(TargetHud element, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        try (ProfilerPhase.Scope ignored = ProfilerPhase.scope("2d:target_hud:main");
             RenderProfiler2D.Section ignoredSection = RenderProfiler2D.section("target_hud:main")) {
            boolean preview = DraggableHudElementRegistry.isForceVisible();
            if (element.mc == null || (element.mc.player == null && !preview)) return;
            long now = System.currentTimeMillis();
            LivingEntity target = TargetHudTargetResolver.resolveTarget(element, preview);
            TargetHudTargetResolver.updateDisplayTarget(element, target, now);
            boolean shouldShow = TargetHudTargetResolver.shouldShow(element, target, now);
            float dt = AnimationUtility.deltaTime();
            element.visibilityAnimation = HudRenderUtil.animateVisibility(element.visibilityAnimation, shouldShow);
            if (!shouldShow && element.visibilityAnimation <= 0.001f) {
                element.displayTarget = null;
                element.setHudSize(0f, 0f);
                return;
            }
            if (element.displayTarget == null) return;
            updatePalette(element);
            float hudScale = HudScale.scale(screenW, screenH) * element.scale.get().floatValue();
            float alphaFactor = AnimationUtility.easeOutCubic(element.visibilityAnimation);
            float hitPulseFactor = computeHitPulse(element, now);
            EffectLayout boundsEffects = TargetHudEffectsRenderer.buildEffectLayout(element, element.displayTarget, preview, textRenderer, hudScale);
            boolean hasEffects = !boundsEffects.chips().isEmpty();
            float effectsTarget = hasEffects ? 1.0f : 0.0f;
            element.effectsAnimation = AnimationUtility.approach(element.effectsAnimation, effectsTarget, dt, EQUIPMENT_SPEED);
            element.effectsAnimation = AnimationUtility.snap(element.effectsAnimation, effectsTarget, 0.001f);
            EquipmentLayout boundsEquipment = TargetHudEquipmentRenderer.buildEquipmentLayout(element, element.displayTarget, preview, textRenderer, hudScale);
            boolean hasEquipment = !boundsEquipment.entries().isEmpty();
            float equipmentTarget = hasEquipment ? 1.0f : 0.0f;
            element.equipmentAnimation = AnimationUtility.approach(element.equipmentAnimation, equipmentTarget, dt, EQUIPMENT_SPEED);
            element.equipmentAnimation = AnimationUtility.snap(element.equipmentAnimation, equipmentTarget, 0.001f);
            float scalePulse = element.hitAnim.get()
                    ? 1.0f + hitPulseFactor * 0.018f * element.hitPulse.get().floatValue()
                    : 1.0f;
            float widgetScale = HudRenderUtil.visibilityScale(element.visibilityAnimation);
            float panelWidthLogical = BASE_WIDTH;
            float panelHeightLogical = BASE_HEIGHT;
            float targetWidth = panelWidthLogical * hudScale;
            float targetHeight = panelHeightLogical * hudScale;
            element.setHudSize(HudRenderUtil.animateDimension(element.hudWidth(), targetWidth), HudRenderUtil.animateDimension(element.hudHeight(), targetHeight));
            float widthFactor = targetWidth <= 0.0f ? 1.0f : element.hudWidth() / targetWidth;
            float heightFactor = targetHeight <= 0.0f ? 1.0f : element.hudHeight() / targetHeight;
            float dimensionFactor = Math.max(0.0f, Math.min(widthFactor, heightFactor));
            float scaleFactor = hudScale * dimensionFactor * widgetScale * scalePulse;
            float drawW = panelWidthLogical * scaleFactor;
            float drawH = panelHeightLogical * scaleFactor;
            float panelDrawW = panelWidthLogical * scaleFactor;
            float panelDrawH = panelHeightLogical * scaleFactor;
            float drawX = element.hudX() + (element.hudWidth() - drawW) * 0.5f;
            float drawY = element.hudY() + (element.hudHeight() - drawH) * 0.5f;
            float panelBaseX = drawX + (drawW - panelDrawW) * 0.5f;
            float panelBaseY = drawY;
            float panelX = panelBaseX + PANEL_INSET * scaleFactor;
            float panelY = panelBaseY + PANEL_INSET * scaleFactor;
            float panelW = panelDrawW - PANEL_INSET * 2f * scaleFactor;
            float panelH = panelDrawH - PANEL_INSET * 2f * scaleFactor;
            float panelRadius = PANEL_RADIUS * scaleFactor;

            boolean glassEnabled = element.isGlassEffect();
            boolean blurEnabled = element.isBlurEffect();
            TotemPopSnapshot totemPopSnapshot = TargetHudTargetResolver.resolveTotemPopSnapshot(element, element.displayTarget);
            TargetHudFaceRenderer.updateTotemPopAnimation(element, element.displayTarget, totemPopSnapshot, dt);

            if (glassEnabled) {
                TargetHudPanelRenderer.drawGlass(element, panelX, panelY, panelW, panelH, panelRadius, alphaFactor);
            } else if (blurEnabled) {
                TargetHudPanelRenderer.drawBlur(element, panelX, panelY, panelW, panelH, panelRadius, element.uiBgPrimary, alphaFactor);
            }

            TargetHudPanelRenderer.drawBackground(element, renderer, panelX, panelY, panelW, panelH, panelRadius, alphaFactor, glassEnabled);
            TargetHudPanelRenderer.drawHitPulse(element, renderer, panelX, panelY, panelW, panelH, panelRadius, alphaFactor, hitPulseFactor);
            TargetHudFaceRenderer.drawFace(element, renderer, textRenderer, ctx, element.displayTarget, panelX, panelY, scaleFactor, alphaFactor, totemPopSnapshot);
            TargetHudContentRenderer.drawContent(element, renderer, textRenderer, element.displayTarget, panelX, panelY, panelDrawW, scaleFactor, alphaFactor, now, dt);
            if (element.effectsAnimation > 0.01f) {
                EffectLayout drawEffects = TargetHudEffectsRenderer.buildEffectLayout(element, element.displayTarget, preview, textRenderer, scaleFactor);
                float effectsY = panelY - drawEffects.height();
                TargetHudEffectsRenderer.drawEffects(element, renderer, textRenderer, drawEffects, panelX, effectsY, panelW, scaleFactor, alphaFactor * element.effectsAnimation, glassEnabled);
            }
            if (element.equipmentAnimation > 0.01f) {
                EquipmentLayout drawEquipment = TargetHudEquipmentRenderer.buildEquipmentLayout(element, element.displayTarget, preview, textRenderer, scaleFactor);
                TargetHudEquipmentRenderer.drawEquipment(element, renderer, textRenderer, ctx, drawEquipment, panelX, panelY + panelH, panelW, drawEquipment.height(), scaleFactor, alphaFactor * element.equipmentAnimation, glassEnabled);
            }
        }
    }

    static void updatePalette(TargetHud element) {
        if (element.isThemeMode()) {
            int panelAlpha = element.bgAlpha.get();
            element.uiBgPrimary = HudRenderUtil.setAlpha(theme().windowBg(), panelAlpha);
            element.uiBgSecondary = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowHeader(), 0.35f),
                    panelAlpha
            );
            element.uiStroke = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowStroke(), theme().strokeSoft(), 0.4f),
                    Math.min(panelAlpha, 190)
            );
            element.uiTextPrimary = theme().textPrimary();
            element.uiTextSecondary = theme().textMuted();
            element.uiBarBackground = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), 0xFF000000, 0.18f),
                    200
            );
            Themes.ThemeEntry entry = Theme.currentEntry();
            Themes.GradientSpec healthGradient = entry != null && entry.cardGradient() != null && entry.cardGradient().enabled()
                    ? entry.cardGradient()
                    : null;
            if (healthGradient != null) {
                element.uiHealthStart = healthGradient.start();
                element.uiHealthEnd = healthGradient.end();
                element.uiHealthGradientAngle = healthGradient.angleDeg();
            } else {
                element.uiHealthStart = theme().accent();
                element.uiHealthEnd = HudRenderUtil.mixColor(theme().accentSoft(), theme().textPrimary(), 0.18f);
                element.uiHealthGradientAngle = 45.0f;
            }
            element.uiTrail = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().accentSoft(), 0.22f),
                    170
            );
            element.uiAbsorptionStart = 0xFFFFB400;
            element.uiAbsorptionEnd = 0xFFFFE17A;
            element.uiAbsorptionGradientAngle = element.uiHealthGradientAngle + 90.0f;
            element.uiHitOuter = HudRenderUtil.mixColor(theme().accentSoft(), 0xFFFF6767, 0.45f);
            element.uiHitInner = HudRenderUtil.mixColor(theme().accent(), 0xFFFFC2C2, 0.22f);
            return;
        }

        element.uiBgPrimary = element.bg.getArgb();
        element.uiBgSecondary = element.bg2.getArgb();
        element.uiStroke = element.stroke.getArgb();
        element.uiTextPrimary = element.text.getArgb();
        element.uiTextSecondary = element.textSecondary.getArgb();
        element.uiBarBackground = element.barBg.getArgb();
        element.uiHealthStart = element.healthStart.getArgb();
        element.uiHealthEnd = element.healthEnd.getArgb();
        element.uiHealthGradientAngle = 45.0f;
        element.uiTrail = element.trail.getArgb();
        element.uiAbsorptionStart = element.absorptionStart.getArgb();
        element.uiAbsorptionEnd = element.absorptionEnd.getArgb();
        element.uiAbsorptionGradientAngle = 135.0f;
        element.uiHitOuter = element.hitOuter.getArgb();
        element.uiHitInner = element.hitInner.getArgb();
    }

    static float computeHitPulse(TargetHud element, long now) {
        if (element.displayTarget == null) return 0.0f;
        Long hitAt = HIT_FLASH_MS_BY_ID.get(element.displayTarget.getId());
        if (hitAt == null) return 0.0f;

        float elapsed = now - hitAt;
        if (elapsed >= HIT_FLASH_MS) {
            HIT_FLASH_MS_BY_ID.remove(element.displayTarget.getId());
            return 0.0f;
        }
        float t = 1.0f - elapsed / HIT_FLASH_MS;
        return AnimationUtility.easeOutCubic(AnimationUtility.clamp01(t));
    }
}
