/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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

