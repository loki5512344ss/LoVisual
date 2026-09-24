package dev.loki.lovisual.mixins.accessors.widget;

import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(CycleButton.class)
public interface CyclingButtonWidgetAccessor<T> {
    @Accessor("spriteSupplier")
    CycleButton.SpriteSupplier<T> lovisual$getIcon();

    @Accessor("displayState")
    CycleButton.DisplayState lovisual$getLabelType();

    @Accessor("valueStringifier")
    Function<T, net.minecraft.network.chat.Component> lovisual$getValueToText();
}

