/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import dev.loki.lovisual.features.command.CommandManager;
import dev.loki.lovisual.features.command.CommandPrefix;
import dev.loki.lovisual.mixins.accessors.ChatScreenAccessor;
import dev.loki.lovisual.mixins.accessors.SuggestionWindowAccessor;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

/**
 * Autocomplete suggestion window state: hotkey, snapshot capture, window dragging and hit-test.
 * Rendering lives in {@link BetterChatSuggestionRenderer}.
 */
final class BetterChatSuggestionState {
    private BetterChatSuggestionState() {
    }

    static boolean isInsideSuggestRows(double mx, double my) {
        if (!BetterChatRenderer.suggestActive) return false;
        return mx >= BetterChatRenderer.suggestRowsX && mx <= BetterChatRenderer.suggestRowsX + BetterChatRenderer.suggestRowsW
                && my >= BetterChatRenderer.suggestRowsY && my <= BetterChatRenderer.suggestRowsY + BetterChatRenderer.suggestRowsH;
    }

    static boolean isInsideSuggestRows(double mx, double my, double rawX, double rawY) {
        return isInsideSuggestRows(mx, my) || isInsideSuggestRows(rawX, rawY);
    }

    static boolean isInsideSuggestWindow(double mx, double my) {
        if (!BetterChatRenderer.suggestActive) return false;
        return mx >= BetterChatRenderer.suggestX && mx <= BetterChatRenderer.suggestX + BetterChatRenderer.suggestW
                && my >= BetterChatRenderer.suggestY && my <= BetterChatRenderer.suggestY + BetterChatRenderer.suggestH;
    }

    static boolean isInsideSuggestWindow(double mx, double my, double rawX, double rawY) {
        return isInsideSuggestWindow(mx, my) || isInsideSuggestWindow(rawX, rawY);
    }

    static boolean isInsideSuggestScrollbar(double mx, double my) {
        if (!BetterChatRenderer.suggestActive || !BetterChatRenderer.suggestHasScrollbar) return false;
        float hitPad = 4f;
        return mx >= BetterChatRenderer.suggestTrackX - hitPad &&
                mx <= BetterChatRenderer.suggestTrackX + BetterChatRenderer.suggestTrackW + hitPad &&
                my >= BetterChatRenderer.suggestTrackY - hitPad &&
                my <= BetterChatRenderer.suggestTrackY + BetterChatRenderer.suggestTrackH + hitPad;
    }

    static void resetSuggestionTracking() {
        BetterChatRenderer.lastSuggestInput = "";
        BetterChatRenderer.lastSuggestCursor = -1;
        BetterChatRenderer.lastSuggestSelStart = -1;
        BetterChatRenderer.lastSuggestSelEnd = -1;
        BetterChatRenderer.lastSnapshot = null;
        BetterChatRenderer.lastSuggestionHash = 0;
        BetterChatRenderer.suggestWindow = null;
        BetterChatRenderer.suggestEntries = Collections.emptyList();
        BetterChatRenderer.suggestRowsX = BetterChatRenderer.suggestRowsY = BetterChatRenderer.suggestRowsW = BetterChatRenderer.suggestRowsH = 0f;
        BetterChatRenderer.suggestCurrentStart = -1;
        BetterChatRenderer.suggestCurrentSelection = -1;
    }

    static char commandPrefix(String text) {
        if (text == null) return 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) continue;
            if (c == '/' || c == CommandPrefix.CHAR || c == CommandPrefix.LEGACY) return c;
            return 0;
        }
        return 0;
    }

    static boolean refreshSuggestionsIfNeeded(String text, int cursor, int selStart, int selEnd) {
        if (text.equals(BetterChatRenderer.lastSuggestInput)
                && cursor == BetterChatRenderer.lastSuggestCursor
                && selStart == BetterChatRenderer.lastSuggestSelStart
                && selEnd == BetterChatRenderer.lastSuggestSelEnd) {
            return false;
        }
        BetterChatRenderer.lastSuggestInput = text;
        BetterChatRenderer.lastSuggestCursor = cursor;
        BetterChatRenderer.lastSuggestSelStart = selStart;
        BetterChatRenderer.lastSuggestSelEnd = selEnd;
        return true;
    }

    static void applySuggestionWindowState(int start, int selection) {
        if (BetterChatRenderer.suggestWindow == null) return;
        SuggestionWindowAccessor acc = (SuggestionWindowAccessor) BetterChatRenderer.suggestWindow;
        acc.setLoVisual$inWindowIndex(Math.max(0, start));
        if (selection >= 0) {
            acc.setLoVisual$selection(selection);
        }
    }

    static void applySuggestion(int idx) {
        Minecraft mc = Minecraft.getInstance();
        if (!(ClientScreen.current() instanceof ChatScreen chatScreen)) return;
        EditBox field = ((ChatScreenAccessor) chatScreen).getChatField();
        if (field == null) return;

        if (BetterChatRenderer.suggestWindow != null) {
            SuggestionWindowAccessor acc = (SuggestionWindowAccessor) BetterChatRenderer.suggestWindow;
            acc.setLoVisual$selection(idx);
        }

        if (idx < 0 || idx >= BetterChatRenderer.suggestEntries.size()) return;
        com.mojang.brigadier.suggestion.Suggestion suggestion = BetterChatRenderer.suggestEntries.get(idx);
        String current = field.getValue();
        try {
            String applied = suggestion.apply(current);
            field.setValue(applied);
            BetterChatEditBoxUtil.setCursorSafe(field, applied.length());
            BetterChatRenderer.suggestCurrentSelection = idx;
            applySuggestionWindowState(BetterChatRenderer.suggestCurrentStart, BetterChatRenderer.suggestCurrentSelection);
        } catch (Exception ignored) {
        }
    }

    static void scrollSuggestToPosition(double my) {
        if (!BetterChatRenderer.suggestHasScrollbar || BetterChatRenderer.suggestVisible <= 0 || BetterChatRenderer.suggestTotal <= BetterChatRenderer.suggestVisible)
            return;
        float trackSpan = BetterChatRenderer.suggestTrackH - BetterChatRenderer.suggestThumbH;
        if (trackSpan <= 0f) return;
        float rel = (float) (my - BetterChatRenderer.suggestTrackY - BetterChatRenderer.suggestThumbH * 0.5f);
        float t = Mth.clamp(rel / trackSpan, 0f, 1f);
        int scrollable = Math.max(0, BetterChatRenderer.suggestTotal - BetterChatRenderer.suggestVisible);
        int targetStart = Math.round(t * scrollable);
        BetterChatRenderer.suggestCurrentStart = Mth.clamp(targetStart, 0, scrollable);
        applySuggestionWindowState(BetterChatRenderer.suggestCurrentStart, BetterChatRenderer.suggestCurrentSelection);
    }

    static CommandSuggestorBridge.SuggestionSnapshot buildCustomSnapshot(String text, int cursor) {
        List<com.mojang.brigadier.suggestion.Suggestion> suggestions = CommandManager.suggest(text, cursor);
        if (suggestions == null || suggestions.isEmpty()) return null;
        List<String> texts = new ArrayList<>(suggestions.size());
        for (com.mojang.brigadier.suggestion.Suggestion s : suggestions) {
            texts.add(s.getText());
        }
        int selection = 0;
        int start = 0;
        int visible = Math.min(5, Math.max(1, suggestions.size()));
        return new CommandSuggestorBridge.SuggestionSnapshot(0, 0, 0, 0, texts, suggestions, selection, start, visible);
    }

    static void updateSearchHotkey(Minecraft mc, boolean chatOpen) {
        if (!chatOpen || mc == null || mc.getWindow() == null) {
            BetterChatRenderer.searchHotkeyDown = false;
            return;
        }
        long handle = mc.getWindow().handle();
        boolean ctrl =
                glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS ||
                        glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS;
        boolean f = glfwGetKey(handle, GLFW.GLFW_KEY_F) == GLFW_PRESS;
        boolean down = ctrl && f;
        if (down && !BetterChatRenderer.searchHotkeyDown) {
            toggleSearchMode();
        }
        BetterChatRenderer.searchHotkeyDown = down;
    }

    static void toggleSearchMode() {
        if (BetterChatSearch.isActive() || BetterChatSearch.hasQuery()) {
            BetterChatSearch.deactivate();
        } else {
            BetterChatSearch.setActive(true);
        }
    }
}