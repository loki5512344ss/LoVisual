/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors.chat;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public interface ChatInputSuggestorAccessor {
    @Accessor("suggestions")
    CommandSuggestions.SuggestionsList getLoVisual$window();

    @Accessor("pendingSuggestions")
    CompletableFuture<Suggestions> getLoVisual$pendingSuggestions();
}

