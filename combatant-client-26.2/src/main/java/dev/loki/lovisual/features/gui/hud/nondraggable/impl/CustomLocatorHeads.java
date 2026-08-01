/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.PlayerHeadRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

final class CustomLocatorHeads {

    private CustomLocatorHeads() {
    }

    static void render(GuiGraphicsExtractor ctx, CustomBar bm, List<CustomBarModel.LocatorHead> heads, float scale) {
        boolean startedHeadsBatch = CustomLocatorRenderer.beginOwnedColorBatch();
        try {
            for (int i = heads.size() - 1; i >= 0; i--) {
                CustomBarModel.LocatorHead head = heads.get(i);
                float headX = head.x() * scale;
                float headYScaled = head.y() * scale;
                float headSize = head.size() * scale;
                float headRadius = head.radius() * scale;
                float headOutline = head.outlineThickness() * scale;
                if (head.target()) {
                    int glow = CustomXpBarRenderer.applyBarAlpha(
                            HudRenderUtil.scaleAlpha(0xFF000000 | bm.getLocatorSearchColorRgb(), 0.28f), bm);
                    Renderer2D.COLOR.roundedRect(
                            headX - 1.9f * scale,
                            headYScaled - 1.9f * scale,
                            headSize + 3.8f * scale,
                            headSize + 3.8f * scale,
                            headRadius + 2.4f * scale,
                            scale,
                            glow
                    );
                }
                if (head.player() != null) {
                    PlayerHeadRenderer.drawRounded(
                            ctx,
                            headX, headYScaled, headSize,
                            headRadius,
                            head.player(),
                            head.color(),
                            true,
                            head.outlineColor(),
                            headOutline,
                            false
                    );
                } else if (head.skin() != null) {
                    PlayerHeadRenderer.drawRounded(
                            ctx,
                            headX, headYScaled, headSize,
                            headRadius,
                            head.skin(),
                            head.color(),
                            true,
                            head.outlineColor(),
                            headOutline,
                            false
                    );
                }
            }
        } finally {
            CustomLocatorRenderer.renderOwnedColorBatch(startedHeadsBatch);
        }
    }
}
