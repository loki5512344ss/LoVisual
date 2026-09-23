/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat.input;

import dev.loki.lovisual.mixins.accessors.widget.TextFieldWidgetAccessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;

/**
 * EditBox selection introspection + word-boundary helper, independent of the chat renderer.
 */
public final class BetterChatEditBoxUtil {
    private BetterChatEditBoxUtil() {
    }

    public static int selectionStart(EditBox field, int fallback) {
        if (field == null) return fallback;
        return Mth.clamp(field.getCursorPosition(), 0, field.getValue().length());
    }

    public static int selectionEnd(EditBox field, int fallback) {
        if (field == null) return fallback;
        try {
            return Mth.clamp(((TextFieldWidgetAccessor) field).lovisual$getSelectionEnd(), 0, field.getValue().length());
        } catch (Throwable ignored) {
            String selected = field.getHighlighted();
            int cursor = selectionStart(field, fallback);
            if (selected == null || selected.isEmpty()) return cursor;
            return Mth.clamp(cursor + selected.length(), 0, field.getValue().length());
        }
    }

    public static void setCursorSafe(EditBox field, int pos) {
        if (field == null) return;
        field.moveCursorTo(Mth.clamp(pos, 0, field.getValue().length()), false);
    }

    public static String wordAt(String text, int charIndex) {
        if (text == null || text.isEmpty()) return "";
        int idx = Mth.clamp(charIndex, 0, Math.max(0, text.length() - 1));
        int start = idx;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        int end = idx;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
        if (end <= start) return "";
        return text.substring(start, end).replaceAll("[<>:\\[\\]]", "");
    }
}
