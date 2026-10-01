package dev.loki.lovisual.mixins.gui.chat;

import com.mojang.brigadier.suggestion.Suggestions;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.command.core.CommandManager;
import dev.loki.lovisual.features.command.util.CommandPrefix;
import dev.loki.lovisual.features.command.suggest.SuggestionPlan;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Baritone-style autocomplete for client commands in the vanilla chat: while the input starts
 * with the command prefix, the popup lists our commands and arguments as you type.
 * BetterChat draws its own popup, so it keeps its existing path.
 */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private EditBox input;
    @Shadow @Final private List<FormattedCharSequence> commandUsage;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private CommandSuggestions.SuggestionsList suggestions;
    @Shadow private boolean keepSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void lovisual$clientCommandSuggestions(CallbackInfo ci) {
        if (!Lifecycle.isActive() || BetterChat.isActive()) return;
        String text = input.getValue();
        if (!CommandPrefix.isCommandLike(text)) return;

        ci.cancel();
        commandUsage.clear();
        if (!keepSuggestions) {
            input.setSuggestion(null);
            suggestions = null;
        }
        SuggestionPlan plan = SuggestionPlan.of(CommandManager.suggest(text, input.getCursorPosition()));
        if (plan.suggestions() != null) {
            pendingSuggestions = CompletableFuture.completedFuture(plan.suggestions());
            showSuggestions(false);
        } else if (plan.ghostHint() != null && !keepSuggestions) {
            input.setSuggestion(" " + plan.ghostHint());
        }
    }
}
