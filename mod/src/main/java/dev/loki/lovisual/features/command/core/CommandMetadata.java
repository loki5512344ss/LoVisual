/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.core;

import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandPrefix;

public final class CommandMetadata {
    private static final ClassValue<Optional<CommandMetadata>> ANNOTATED = new ClassValue<>() {
        @Override
        protected Optional<CommandMetadata> computeValue(Class<?> type) {
            CommandInfo info = type.getAnnotation(CommandInfo.class);
            return info == null ? Optional.empty() : Optional.of(from(info));
        }
    };

    private final String id;
    private final List<String> aliases;
    private final String usage;
    private final String description;
    private final boolean localizedDescription;

    private CommandMetadata(String id,
                            List<String> aliases,
                            String usage,
                            String description,
                            boolean localizedDescription) {
        this.id = normalizeId(id);
        this.aliases = normalizeAliases(aliases, this.id);
        this.usage = normalizeUsage(usage, this.id);
        if (description == null || description.isBlank()) {
            this.description = this.usage;
            this.localizedDescription = false;
        } else {
            this.description = description.trim();
            this.localizedDescription = localizedDescription;
        }
    }

    public String id() {
        return id;
    }

    public List<String> aliases() {
        return aliases;
    }

    public String usage() {
        return usage;
    }

    public String description() {
        return localizedDescription ? I18n.get(description) : description;
    }

    public String descriptionKey() {
        return localizedDescription ? description : "";
    }

    public static CommandMetadata from(ClientCommand command) {
        if (command == null) throw new IllegalArgumentException("command cannot be null");

        CommandMetadata annotated = annotated(command.getClass());
        if (annotated != null) return annotated;
        return fromLegacy(command);
    }

    public static CommandMetadata fromAnnotation(Class<?> type) {

        CommandMetadata annotated = annotated(type);
        if (annotated == null) {
            throw new IllegalStateException(type.getName() + " must be annotated with @CommandInfo");
        }
        return annotated;
    }

    public static CommandMetadata annotated(Class<?> type) {

        return ANNOTATED.get(type).orElse(null);
    }

    private static CommandMetadata from(CommandInfo info) {
        return new CommandMetadata(info.id(), List.of(info.aliases()), info.usage(), info.descriptionKey(), true);
    }

    @SuppressWarnings("removal")
    private static CommandMetadata fromLegacy(ClientCommand command) {
        String id = command.name();
        List<String> aliases = command.aliases();
        // Legacy default usage()/description() resolve back to the id with no
        // prefix; they are normalized here the same way as annotated strings.
        String usage = command.usage();
        String description = command.description();
        String normalizedUsage = usage.equals(id) || usage.isBlank() ? null : usage;
        String normalizedDescription =
                description.equals(id) || description.equals(usage) || description.isBlank() ? null : description;
        return new CommandMetadata(id, aliases, normalizedUsage, normalizedDescription, false);
    }

    private static String normalizeUsage(String usage, String id) {
        if (usage == null || usage.isBlank()) return CommandPrefix.STRING + id;
        String trimmed = usage.trim();
        // Usage strings were written with the legacy '@' prefix; re-prefix them
        // with the active prefix so help output stays consistent.
        if (!trimmed.isEmpty() && (trimmed.charAt(0) == CommandPrefix.LEGACY
                || trimmed.charAt(0) == CommandPrefix.CHAR)) {
            trimmed = trimmed.substring(1);
        }
        return CommandPrefix.STRING + trimmed;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Command id cannot be blank");
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.charAt(0) == '@' || normalized.charAt(0) == CommandPrefix.CHAR) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Invalid command id: " + id);
        }
        return normalized;
    }

    private static List<String> normalizeAliases(List<String> aliases, String id) {
        if (aliases == null || aliases.isEmpty()) return List.of();
        List<String> normalized = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) continue;
            String value = alias.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty() && (value.charAt(0) == '@' || value.charAt(0) == CommandPrefix.CHAR)) {
                value = value.substring(1);
            }
            if (value.isBlank() || value.chars().anyMatch(Character::isWhitespace) || value.equals(id)) continue;
            if (!normalized.contains(value)) normalized.add(value);
        }
        return List.copyOf(normalized);
    }
}
