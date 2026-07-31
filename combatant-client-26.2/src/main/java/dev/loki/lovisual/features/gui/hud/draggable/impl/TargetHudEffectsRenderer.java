/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.FontInfo;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.player.effect.StatusEffectView;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud.*;
import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHudFormatter.*;

final class TargetHudEffectsRenderer {

    private TargetHudEffectsRenderer() {
    }

    static EffectLayout buildEffectLayout(TargetHud element,
                                          LivingEntity target,
                                          boolean preview,
                                          TextRenderer textRenderer,
                                          float scale) {
        if (!element.isEffectsEnabled() || target == null || textRenderer == null || scale <= 0.0f) {
            return new EffectLayout(List.of(), 0.0f, 0.0f);
        }

        List<MobEffectInstance> effects = collectTargetEffects(element, target, preview);
        if (effects.isEmpty()) {
            return new EffectLayout(List.of(), 0.0f, 0.0f);
        }

        List<EffectChip> chips = new ArrayList<>();
        TextRenderer chipRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        float textScale = EFFECTS_TEXT_SCALE * scale;
        int limit = Math.min(4, effects.size());
        float totalWidth = 0.0f;

        for (int i = 0; i < limit; i++) {
            MobEffectInstance inst = effects.get(i);
            Identifier iconId = resolveEffectIcon(inst);
            String text = compactEffectText(target.getId(), inst);
            float width = EFFECTS_CHIP_PAD_X * 2.0f * scale + EFFECTS_ICON_SIZE * scale;
            if (!text.isEmpty()) {
                width += EFFECTS_TEXT_GAP * scale + TargetHudTextRenderer.measureText(chipRenderer, text, textScale);
            }
            chips.add(new EffectChip(inst, iconId, text, width));
            totalWidth += width;
            if (i < limit - 1) {
                totalWidth += EFFECTS_CHIP_GAP * scale;
            }
        }

        float totalHeight = (EFFECTS_CHIP_HEIGHT + EFFECTS_PANEL_GAP) * scale;
        return new EffectLayout(chips, totalWidth, totalHeight);
    }

    static void drawEffects(TargetHud element,
                            Renderer2D renderer,
                            TextRenderer textRenderer,
                            EffectLayout layout,
                            float x,
                            float y,
                            float panelWidth,
                            float scaleFactor,
                            float alphaFactor,
                            boolean glassEnabled) {
        if (layout.chips().isEmpty() || alphaFactor <= 0.01f) {
            return;
        }

        float chipHeight = EFFECTS_CHIP_HEIGHT * scaleFactor;
        float rowX = x + (panelWidth - layout.width()) * 0.5f;
        float rowY = y + (layout.height() - chipHeight) * 0.5f;
        float radius = EFFECTS_CHIP_RADIUS * scaleFactor;
        int fill = glassEnabled
                ? HudRenderUtil.glassSmallBackground(alphaFactor)
                : HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(element.uiBgPrimary, element.uiBgSecondary, 0.4f), alphaFactor);
        int stroke = glassEnabled
                ? 0
                : HudRenderUtil.scaleAlpha(element.uiStroke, alphaFactor * 0.5f);
        int iconColor = HudRenderUtil.scaleAlpha(element.uiTextPrimary, alphaFactor);
        int textColor = HudRenderUtil.scaleAlpha(element.uiTextSecondary, alphaFactor);

        TextRenderer chipRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        float textScale = EFFECTS_TEXT_SCALE * scaleFactor;
        float textHeight = 0.0f;
        chipRenderer.begin(textScale, false, false);
        try {
            textHeight = (float) chipRenderer.getHeight(false);
            float cursorX = rowX;
            for (EffectChip chip : layout.chips()) {
                if (glassEnabled) {
                    TargetHudPanelRenderer.drawGlassCard(element, cursorX, rowY, chip.width(), chipHeight, radius, scaleFactor, alphaFactor);
                }
                renderer.roundedRect(cursorX, rowY, chip.width(), chipHeight, radius, 1.0f, fill);
                if (!glassEnabled) {
                    renderer.roundedRectStroke(cursorX, rowY, chip.width(), chipHeight, radius, 1.0f, 0.45f * scaleFactor, stroke);
                }

                float iconX = cursorX + EFFECTS_CHIP_PAD_X * scaleFactor;
                float iconY = rowY + (chipHeight - EFFECTS_ICON_SIZE * scaleFactor) * 0.5f;
                if (chip.iconId() != null) {
                    Renderer2D.TEXTURE.roundedTexRect(
                            iconX,
                            iconY,
                            EFFECTS_ICON_SIZE * scaleFactor,
                            EFFECTS_ICON_SIZE * scaleFactor,
                            2.0f * scaleFactor,
                            1.0f,
                            iconColor,
                            chip.iconId()
                    );
                }

                if (!chip.text().isEmpty()) {
                    float textX = iconX + EFFECTS_ICON_SIZE * scaleFactor + EFFECTS_TEXT_GAP * scaleFactor;
                    float textY = rowY + (chipHeight - textHeight) * 0.5f;
                    chipRenderer.render(chip.text(), textX, textY, new RenderColor(textColor), false);
                }

                cursorX += chip.width() + EFFECTS_CHIP_GAP * scaleFactor;
            }
        } finally {
            chipRenderer.end();
        }
    }

    private static List<MobEffectInstance> collectTargetEffects(TargetHud element, LivingEntity target, boolean preview) {
        if (target == null) {
            return List.of();
        }
        if (target == element.mc.player && !element.effectsSelf.get() && !preview) {
            return List.of();
        }
        if (target instanceof Player player) {
            StatusEffectView.sync(element.mc);
            List<MobEffectInstance> effects = StatusEffectView.collectHudEffects(player);
            if (!effects.isEmpty() || !preview) {
                return effects;
            }
        } else {
            List<MobEffectInstance> direct = new ArrayList<>(target.getActiveEffects());
            if (!direct.isEmpty()) {
                return direct;
            }
        }

        if (!preview) {
            return List.of();
        }

        List<MobEffectInstance> sample = new ArrayList<>();
        sample.add(new MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 2450, 1));
        sample.add(new MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1640, 0));
        sample.add(new MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 920, 0));
        return sample;
    }
}
