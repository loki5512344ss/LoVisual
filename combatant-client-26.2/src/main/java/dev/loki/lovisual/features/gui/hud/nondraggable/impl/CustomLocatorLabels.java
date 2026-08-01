/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.ColorMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBar.*;
import static dev.loki.lovisual.features.theme.Theme.theme;

final class CustomLocatorLabels {

    private CustomLocatorLabels() {
    }

    static void render(GuiGraphicsExtractor ctx, CustomBar bm, float screenW, float barY, float scale,
                       List<CustomBarModel.LocatorLabel> labels) {
        TextRenderer tr = Fonts.renderer("Iosevka");
        Renderer2D r2d = Renderer2D.COLOR;
        float labelScale = LOCATOR_LABEL_SCALE * scale;
        List<CustomBarModel.LocatorLabel> layoutLabels = new ArrayList<>();
        for (CustomBarModel.LocatorLabel label : labels) {
            if (label.active()) {
                layoutLabels.add(label);
            }
        }
        if (layoutLabels.isEmpty()) {
            layoutLabels.addAll(labels);
        }
        layoutLabels.sort(Comparator
                .comparing((CustomBarModel.LocatorLabel label) -> !label.target())
                .thenComparing(label -> label.sort() == null ? "" : label.sort())
                .thenComparing(CustomBarModel.LocatorLabel::name));
        List<CustomBarModel.LocatorLabel> draw = layoutLabels.size() > LOCATOR_LABEL_MAX_ROWS
                ? new ArrayList<>(layoutLabels.subList(0, LOCATOR_LABEL_MAX_ROWS))
                : layoutLabels;

        tr.begin(labelScale, false, false);
        float textH = (float) tr.getHeight(true);
        float markerReserve = draw.stream().anyMatch(CustomBarModel.LocatorLabel::target) ? 4.7f * scale : 0.0f;
        float chipPadX = LOCATOR_LABEL_CHIP_PAD_X * scale;
        float chipPadY = LOCATOR_LABEL_CHIP_PAD_Y * scale;
        float distanceGap = 5.6f * scale;
        float contentW = 0.0f;
        for (CustomBarModel.LocatorLabel label : draw) {
            float nameW = (float) tr.getWidth(label.name(), true);
            float distanceW = label.distance().isEmpty() ? 0.0f : (float) tr.getWidth(label.distance(), true);
            float rowTextW = nameW + (distanceW > 0.0f ? distanceGap + distanceW : 0.0f);
            contentW = Math.max(contentW, markerReserve + rowTextW);
        }
        tr.end();

        float padX = LOCATOR_LABEL_PADDING_X * scale;
        float padY = LOCATOR_LABEL_PADDING_Y * scale;
        float rowGap = LOCATOR_LABEL_ROW_GAP * scale;
        float chipH = textH + chipPadY * 2.0f;
        float rowStep = chipH + rowGap;
        float rowW = Math.max(58.0f * scale, contentW + chipPadX * 2.0f);
        float panelW = padX * 2.0f + rowW;
        float panelH = padY * 2.0f + draw.size() * chipH + Math.max(0, draw.size() - 1) * rowGap;
        float panelX = (screenW * scale - panelW) * 0.5f;
        panelX = Mth.clamp(panelX, 4.0f * scale, screenW * scale - panelW - 4.0f * scale);
        float panelY = barY * scale - LOCATOR_LABEL_OFFSET * scale - panelH;
        panelY = Math.max(4.0f * scale, panelY);

        float panelAlpha = 0.0f;
        boolean hasTarget = false;
        for (CustomBarModel.LocatorLabel label : draw) {
            panelAlpha = Math.max(panelAlpha, Mth.clamp(label.alpha(), 0f, 1f));
            hasTarget |= label.target();
        }

        boolean startedLabelBatch = CustomLocatorRenderer.beginOwnedColorBatch();
        try {
            float ease = locatorLabelEase(panelAlpha);
            float radius = LOCATOR_LABEL_RADIUS * scale;
            float fadedPanelY = panelY + (1.0f - ease) * 2.2f * scale;
            CustomLocatorLabelPanel.drawPanel(r2d, bm, panelX, fadedPanelY, panelW, panelH, radius, scale, ease, hasTarget);

            float rowX = panelX + padX;
            float rowY = fadedPanelY + padY;
            float chipR = Math.min(chipH * 0.48f, LOCATOR_LABEL_CHIP_RADIUS * scale);
            for (int row = 0; row < draw.size(); row++) {
                CustomBarModel.LocatorLabel label = draw.get(row);
                float anim = Mth.clamp(label.alpha(), 0f, 1f);
                CustomLocatorLabelPanel.drawRow(r2d, bm, label, rowX, rowY + row * rowStep, rowW, chipH, chipR, scale, anim);
            }

            tr.begin(labelScale, false, false);
            for (int row = 0; row < draw.size(); row++) {
                CustomBarModel.LocatorLabel label = draw.get(row);
                float anim = Mth.clamp(label.alpha(), 0f, 1f);
                float textBaseX = rowX + chipPadX + markerReserve;
                float textY = rowY + row * rowStep + (chipH - textH) * 0.5f;
                int nameArgb = CustomXpBarRenderer.applyBarAlpha(
                        ColorMath.scaleAlpha(theme().textPrimary(), anim * (label.target() ? 1.0f : 0.92f)), bm);
                tr.render(label.name(), textBaseX, textY, new RenderColor(nameArgb), true);
                if (!label.distance().isEmpty()) {
                    float distanceW = (float) tr.getWidth(label.distance(), true);
                    float distanceX = rowX + rowW - chipPadX - distanceW;
                    int distanceArgb = CustomXpBarRenderer.applyBarAlpha(
                            ColorMath.scaleAlpha(theme().textPrimary(), anim * 0.90f), bm);
                    tr.render(label.distance(), distanceX, textY, new RenderColor(distanceArgb), true);
                }
            }
            tr.end();
        } finally {
            CustomLocatorRenderer.renderOwnedColorBatch(startedLabelBatch);
        }
    }

    private static float locatorLabelEase(float alpha) {
        return AnimationUtility.easeOutCubic(alpha);
    }
}
