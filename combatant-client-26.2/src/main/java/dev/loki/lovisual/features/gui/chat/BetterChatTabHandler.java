/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.loki.lovisual.features.command.CommandManager;
import dev.loki.lovisual.features.command.CommandPrefix;
import dev.loki.lovisual.mixins.accessors.ChatScreenAccessor;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;

import java.util.List;

/**
 * Tab / arrow-key cycling through the custom {@code @}-command suggestion popup.
 * Vanilla CommandSuggestions only drives suggestions for {@code /} commands, so our
 * {@code @} popup needs its own keyboard handling: Up/Down move the highlight, Tab applies.
 */
public final class BetterChatTabHandler {
    private BetterChatTabHandler() {
    }

    private static String lastInput = "";
    private static int lastSelection = -1;
    private static int lastTotal = 0;

    public static boolean handleKey(KeyEvent input) {
        if (input == null) return false;
        boolean down = input.isDown();
        boolean up = input.isUp();
        if (!down && !up && !input.isCycleFocus()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (!(ClientScreen.current() instanceof ChatScreen chatScreen)) return false;
        EditBox field = ((ChatScreenAccessor) chatScreen).getChatField();
        if (field == null) return false;
        String text = field.getValue();
        if (text == null || text.isEmpty()) return false;
        if (BetterChatSuggestionState.commandPrefix(text) != CommandPrefix.CHAR && BetterChatSuggestionState.commandPrefix(text) != CommandPrefix.LEGACY) return false;

        int cursor = field.getCursorPosition();
        List<Suggestion> suggestions = CommandManager.suggest(text, cursor);
        if (suggestions == null || suggestions.isEmpty()) return false;
        int total = suggestions.size();
        if (!text.equals(lastInput) || lastTotal != total) {
            lastInput = text;
            lastTotal = total;
            lastSelection = -1;
        }

        if (down || up) {
            int delta = down ? 1 : -1;
            int selection = lastSelection < 0 ? (delta > 0 ? 0 : total - 1) : (lastSelection + delta) % total;
            if (selection < 0) selection += total;
            lastSelection = selection;
            trackSelection(suggestions, selection);
            return true;
        }

        int selection = lastSelection < 0 ? 0 : (lastSelection + (input.hasShiftDown() ? -1 : 1)) % total;
        if (selection < 0) selection += total;
        lastSelection = selection;

        Suggestion suggestion = suggestions.get(selection);
        try {
            field.setValue(suggestion.apply(text));
            BetterChatEditBoxUtil.setCursorSafe(field, field.getValue().length());
            trackSelection(suggestions, selection);
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    private static void trackSelection(List<Suggestion> suggestions, int selection) {
        BetterChatRenderer.suggestEntries = suggestions;
        BetterChatRenderer.suggestCurrentSelection = selection;
        BetterChatRenderer.suggestCurrentStart = 0;
        BetterChatRenderer.lastSnapshot = null;
    }
}
