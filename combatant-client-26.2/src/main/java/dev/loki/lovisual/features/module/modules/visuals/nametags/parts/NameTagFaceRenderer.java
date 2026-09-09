/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.nametags.parts;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.helpers.MatteHudStyle;
import dev.loki.lovisual.render.helpers.PlayerHeadRenderer;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagModel;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

public final class NameTagFaceRenderer {

    private NameTagFaceRenderer() {
    }

    public static boolean shouldRenderPlayerHead(NameTags self, Player player) {
        return self.playerHead.get() && player instanceof AbstractClientPlayer;
    }

    static void drawRoundedHead(GuiGraphicsExtractor ctx, NameTagModel.NameplateLayout layout,
                                AbstractClientPlayer clientPlayer, int alpha) {
        PlayerHeadRenderer.drawRounded(
                ctx,
                layout.headX(),
                layout.headY(),
                layout.headSize(),
                Math.min(6.0f, layout.headSize() * 0.48f),
                clientPlayer,
                new RenderColor(MatteHudStyle.withAlpha(0xFFFFFFFF, alpha)),
                true,
                null,
                0.0f,
                false
        );
    }
}
