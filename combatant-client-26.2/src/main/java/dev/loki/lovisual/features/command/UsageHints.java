/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds usage-hint suggestions shown in the autocomplete popup when a command
 * has no concrete suggestions for the argument position the user is typing at.
 * Pure string logic, unit-testable without Minecraft.
 */
public final class UsageHints {
    private UsageHints() {
    }

    /**
     * Extracts the placeholder at {@code argIndex} (1-based, after the command
     * name) from a usage string like "%toggle &lt;module&gt; [on|off|toggle]".
     *
     * @return hint without brackets, e.g. "module" or "on|off|toggle";
     *         null when the usage has nothing at that position
     */
    public static String placeholder(String usage, int argIndex) {
        if (usage == null || usage.isBlank() || argIndex < 1) return null;
        return placeholders(usage).stream()
                .skip(argIndex - 1L)
                .findFirst()
                .orElse(null);
    }

    /**
     * Usage hint with the command prefix: "&lt;module&gt;" becomes "%toggle &lt;module&gt;"
     * so the popup always shows what the full input looks like so far.
     */
    public static String forCommand(ClientCommand command, int argIndex) {
        if (command == null || argIndex < 1) return null;
        String usage = command.metadata().usage();
        String placeholder = placeholder(usage, argIndex);
        if (placeholder == null) return null;

        String head = commandHead(usage);
        String prefix = CommandPrefix.STRING + (head.isEmpty() ? command.metadata().id() : head);
        return prefix + " <" + placeholder + ">";
    }

    /** Splits "[list|add|remove] [player]" into ["list|add|remove", "player"]. */
    static List<String> placeholders(String usage) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inside = false;
        for (int i = 0; i < usage.length(); i++) {
            char c = usage.charAt(i);
            if (c == '[' || c == '<') {
                inside = true;
                current.setLength(0);
            } else if (c == ']' || c == '>') {
                inside = false;
                if (!current.isEmpty()) out.add(current.toString());
                current.setLength(0);
            } else if (inside) {
                current.append(c);
            }
        }
        if (inside && !current.isEmpty()) out.add(current.toString());
        return List.copyOf(out);
    }

    /** First word of the usage body, e.g. "toggle" from "%toggle <module>". */
    static String commandHead(String usage) {
        String trimmed = usage.trim();
        if (trimmed.startsWith(CommandPrefix.STRING)) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        String head = space < 0 ? trimmed : trimmed.substring(0, space);
        return head.toLowerCase(Locale.ROOT);
    }
}
