/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat.suggestions;

import dev.loki.lovisual.features.command.util.CommandOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import dev.loki.lovisual.features.gui.chat.model.ChatLine;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;

/**
 * Merges command-output lines into the visible chat tail, sharing the identity-keyed
 * command-line cache held on {@link BetterChatRenderer}.
 */
public final class BetterChatCommandMerge {
    private BetterChatCommandMerge() {
    }

    public static List<ChatLine> mergeCommandOutput(List<ChatLine> storeLines,
                                             List<CommandOutput.MessageLine> commandLines,
                                             int needed) {
        if (commandLines == null || commandLines.isEmpty()) {
            return storeLines;
        }
        List<ChatLine> out = new ArrayList<>(storeLines.size() + commandLines.size());
        out.addAll(storeLines);
        for (CommandOutput.MessageLine line : commandLines) {
            if (line == null || line.text() == null) continue;
            out.add(BetterChatRenderer.COMMAND_LINE_CACHE.computeIfAbsent(line, key -> new ChatLine(key.text(), key.timestampMs())));
        }
        out.sort(Comparator.comparingLong(ChatLine::timestampMs));
        trimCommandLineCache(commandLines);
        if (needed > 0 && out.size() > needed) {
            return new ArrayList<>(out.subList(out.size() - needed, out.size()));
        }
        return out;
    }

    static void trimCommandLineCache(List<CommandOutput.MessageLine> liveLines) {
        if (BetterChatRenderer.COMMAND_LINE_CACHE.size() <= 160) return;
        IdentityHashMap<CommandOutput.MessageLine, Boolean> live = new IdentityHashMap<>();
        for (CommandOutput.MessageLine line : liveLines) {
            live.put(line, Boolean.TRUE);
        }
        BetterChatRenderer.COMMAND_LINE_CACHE.keySet().removeIf(line -> !live.containsKey(line));
    }
}
