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

