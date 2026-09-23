/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors.hud;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteIconButton.class)
public interface TextIconButtonWidgetAccessor {
    @Accessor("spriteWidth")
    int lovisual$getTextureWidth();

    @Accessor("spriteHeight")
    int lovisual$getTextureHeight();

    @Accessor("sprite")
    WidgetSprites lovisual$getTexture();
}

