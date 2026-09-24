package dev.loki.lovisual.mixins.accessors.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface ClickableWidgetAccessor {
    @Accessor("tooltip")
    WidgetTooltipHolder lovisual$getTooltipState();
}

