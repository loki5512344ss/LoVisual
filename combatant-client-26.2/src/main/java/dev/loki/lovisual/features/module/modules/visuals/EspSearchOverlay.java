/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.MatteHudStyle;
import dev.loki.lovisual.render.helpers.ScreenSpaceOverlay2D;
import dev.loki.lovisual.util.player.PlayerHealthResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class EspSearchOverlay {

    private EspSearchOverlay() {
    }

    static void render(ESP esp, Renderer2D renderer, TextRenderer fallbackTextRenderer, float tickDelta, Vec3 camPos) {
        List<EspModel.RenderEntry> entries = EspEntityCollector.collectSearchEntityEntries(esp, camPos);
        if (entries.isEmpty()) return;

        entries.sort(Comparator.comparingDouble(EspModel.RenderEntry::distSq).reversed());

        TextRenderer labelRenderer = ScreenSpaceOverlay2D.labelRenderer(fallbackTextRenderer);
        List<EspModel.SearchLabelEntry> labels = new ArrayList<>();
        boolean measureStarted = false;
        if (!labelRenderer.isBuilding()) {
            labelRenderer.begin(ScreenSpaceOverlay2D.TEXT_SCALE, true, false);
            measureStarted = true;
        }

        for (EspModel.RenderEntry entry : entries) {
            Entity entity = entry.entity();
            AABB box = Esp2DRenderer.resolveWorldBox(entity, tickDelta);
            ScreenSpaceOverlay2D.ScreenRect rect = ScreenSpaceOverlay2D.projectBox(box, tickDelta);
            if (rect == null) continue;

            ScreenSpaceOverlay2D.LabelEntry label = createSearchEntityLabel(labelRenderer, entity, entry.color(), rect);
            labels.add(new EspModel.SearchLabelEntry(entry.distSq(), rect, entry.color(), label));
        }

        if (measureStarted) {
            labelRenderer.end();
        }
        if (labels.isEmpty()) return;

        labels.sort(Comparator.comparingDouble(EspModel.SearchLabelEntry::distSq).reversed());
        for (EspModel.SearchLabelEntry entry : labels) {
            renderSearchEntityLabelPass(renderer, labelRenderer, entry);
        }
    }

    private static void renderSearchEntityLabelPass(Renderer2D renderer,
                                                    TextRenderer labelRenderer,
                                                    EspModel.SearchLabelEntry entry) {
        ScreenSpaceOverlay2D.ScreenRect rect = entry.rect();
        ScreenSpaceOverlay2D.LabelEntry label = entry.label();
        MatteHudStyle.drawFrame(renderer, rect.minX(), rect.minY(), rect.width(), rect.height(), entry.color(), 1.0f);
        MatteHudStyle.drawLabelPlate(renderer, label.x(), label.y(), label.totalWidth(), label.height(), 1.0f);

        boolean renderStarted = false;
        if (!labelRenderer.isBuilding()) {
            labelRenderer.begin(ScreenSpaceOverlay2D.TEXT_SCALE);
            renderStarted = true;
        }
        try {
            ScreenSpaceOverlay2D.renderLabels(labelRenderer, List.of(label), false);
        } finally {
            if (renderStarted) {
                labelRenderer.end();
            }
        }
        Renderer2D.flushBatch(Renderer2D.FlushReason.EXPLICIT);
    }

    private static ScreenSpaceOverlay2D.LabelEntry createSearchEntityLabel(TextRenderer labelRenderer, Entity entity,
                                                                           int baseColor,
                                                                           ScreenSpaceOverlay2D.ScreenRect rect) {
        String name = entity.getDisplayName() != null ? entity.getDisplayName().getString() : entity.getName().getString();
        if (entity instanceof LivingEntity living) {
            PlayerHealthResolver.HealthSnapshot health = PlayerHealthResolver.resolve(living);
            float max = health.maxHealth();
            if (max > 0.0f) {
                float ratio = EspColorUtil.clamp01(health.totalHealth() / max);
                String hpText = EspColorUtil.formatHealth(health.totalHealth());
                return ScreenSpaceOverlay2D.createCenteredLabel(
                        labelRenderer,
                        name,
                        hpText,
                        baseColor,
                        EspColorUtil.withAlpha(EspColorUtil.healthRgb(ratio), 235),
                        rect
                );
            }
        }
        return ScreenSpaceOverlay2D.createCenteredLabel(labelRenderer, name, baseColor, rect);
    }
}
