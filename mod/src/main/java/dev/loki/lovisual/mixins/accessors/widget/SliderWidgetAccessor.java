package dev.loki.lovisual.mixins.accessors.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSliderButton.class)
public interface SliderWidgetAccessor {
    @Accessor("value")
    double lovisual$getValue();

    @Accessor("dragging")
    boolean lovisual$isDragging();
}

