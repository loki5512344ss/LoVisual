/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.helpers.material;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import dev.loki.lovisual.render.helpers.util.GuiSpriteBatch;

public enum EffectIconRenderer {
    ;

    /**
     * Draws a status effect icon via DrawContext if the sprite is available.
     */
    public static void draw(GuiGraphicsExtractor ctx, MobEffectInstance effect, int x, int y, int size) {
        if (effect == null) return;
        Identifier texture = Hud.getMobEffectSprite(effect.getEffect());
        GuiSpriteBatch.draw(texture, x, y, size, size, 0xFFFFFFFF);
    }

    public static void beginBatch() {
        GuiSpriteBatch.begin();
    }

    public static void endBatch() {
        GuiSpriteBatch.end();
    }
}
