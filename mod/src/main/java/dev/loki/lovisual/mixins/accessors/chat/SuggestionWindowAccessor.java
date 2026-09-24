package dev.loki.lovisual.mixins.accessors.chat;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface SuggestionWindowAccessor {
    @Accessor("rect")
    Rect2i getLoVisual$area();

    @Accessor("suggestionList")
    List<?> getLoVisual$suggestions();

    @Accessor("current")
    int getLoVisual$selection();

    @Accessor("current")
    void setLoVisual$selection(int selection);

    @Accessor("offset")
    int getLoVisual$inWindowIndex();

    @Accessor("offset")
    void setLoVisual$inWindowIndex(int index);
}

