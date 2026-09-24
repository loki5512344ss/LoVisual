package dev.loki.lovisual.features.command.core;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import dev.loki.lovisual.util.logging.DebugLog;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.core.CommandMetadata;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.command.util.CommandPrefix;
import dev.loki.lovisual.features.command.util.UsageHints;

public enum CommandManager {
    ;
    private static final List<ClientCommand> COMMANDS = new ArrayList<>();
    private static boolean initialized;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        discover("dev.loki.lovisual.features.command.impl");
    }

    public static List<ClientCommand> getCommands() {
        if (!initialized) init();
        return Collections.unmodifiableList(COMMANDS);
    }

    public static void register(ClientCommand cmd) {
        if (cmd == null) return;
        CommandMetadata metadata = cmd.metadata();
        if (findRegistered(metadata.id()) != null) {
            throw new IllegalArgumentException("Duplicate client command: @" + metadata.id());
        }
        for (String alias : metadata.aliases()) {
            if (findRegistered(alias) != null) {
                throw new IllegalArgumentException("Duplicate client command alias: @" + alias);
            }
        }
        COMMANDS.add(cmd);
    }

    public static boolean handle(String raw) {
        if (raw == null) return false;
        String trimmed = raw.trim();
        if (!CommandPrefix.isCommandLike(trimmed)) return false;
        if (!initialized) init();

        String body = CommandPrefix.strip(trimmed);
        if (body.isEmpty()) return false;
        String[] parts = body.split("\\s+");
        if (parts.length == 0) return false;
        String name = parts[0].toLowerCase();

        ClientCommand cmd = find(name);
        if (cmd == null) {
            CommandOutput.error("Unknown client command: " + CommandPrefix.STRING + name
                    + ". Use " + CommandPrefix.STRING + "help.");
            return true;
        }
        if (!cmd.isAvailable()) {
            return true; // silently ignore when command is disabled
        }

        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(parts).subList(1, parts.length));
        CommandContext ctx = new CommandContext(Minecraft.getInstance(), raw, name, args);
        return cmd.execute(ctx);
    }

    public static List<Suggestion> suggest(String input, int cursor) {
        if (input == null) return List.of();
        int start = firstNonWhitespace(input);
        if (start < 0) return List.of();
        char prefix = input.charAt(start);
        if (prefix != CommandPrefix.CHAR && prefix != CommandPrefix.LEGACY) return List.of();
        if (!initialized) init();

        int cursorClamped = Math.max(start + 1, Math.min(cursor, input.length()));
        String before = input.substring(start + 1, cursorClamped);
        String[] parts = before.split("\\s+", -1);
        if (parts.length == 0) return List.of();

        int argIndex = parts.length - 1;
        String token = parts[argIndex];
        String cmdName = parts[0].toLowerCase();

        int tokenStartInBefore = before.lastIndexOf(' ');
        tokenStartInBefore = tokenStartInBefore >= 0 ? tokenStartInBefore + 1 : 0;
        int tokenStartInInput = start + 1 + tokenStartInBefore;
        int tokenEndInInput = tokenStartInInput + token.length();
        StringRange range = StringRange.between(tokenStartInInput, tokenEndInInput);

        List<String> suggestions;
        if (argIndex == 0) {
            suggestions = suggestCommandNames(token);
        } else {
            ClientCommand cmd = find(cmdName);
            if (cmd == null || !cmd.isAvailable()) return List.of();
            List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList(parts).subList(1, parts.length));
            CommandContext ctx = new CommandContext(Minecraft.getInstance(), input, cmdName, args);
            suggestions = cmd.suggest(ctx, argIndex, token);
            if (suggestions == null || suggestions.isEmpty()) {
                // Fall back to the usage hint so there is always guidance on
                // what can be typed at the current position.
                String hint = UsageHints.forCommand(cmd, argIndex);
                suggestions = hint == null ? List.of() : List.of(hint);
            }
        }

        if (suggestions == null || suggestions.isEmpty()) return List.of();
        List<Suggestion> out = new ArrayList<>(suggestions.size());
        for (String s : suggestions) {
            if (s == null || s.isBlank()) continue;
            out.add(new Suggestion(range, s));
        }
        return out;
    }

    public static ClientCommand find(String name) {
        if (!initialized) init();
        return findRegistered(name);
    }

    private static ClientCommand findRegistered(String name) {
        if (name == null || name.isBlank()) return null;
        for (ClientCommand cmd : COMMANDS) {
            CommandMetadata metadata = cmd.metadata();
            if (metadata.id().equalsIgnoreCase(name)) return cmd;
            for (String alias : metadata.aliases()) {
                if (alias.equalsIgnoreCase(name)) return cmd;
            }
        }
        return null;
    }

    private static List<String> suggestCommandNames(String token) {
        String lower = token == null ? "" : token.toLowerCase();
        List<String> out = new ArrayList<>();
        for (ClientCommand cmd : COMMANDS) {
            if (!cmd.isAvailable()) continue;
            CommandMetadata metadata = cmd.metadata();
            if (lower.isEmpty() || metadata.id().startsWith(lower)) {
                out.add(metadata.id());
            }
            for (String alias : metadata.aliases()) {
                if (alias == null || alias.isBlank()) continue;
                if (lower.isEmpty() || alias.startsWith(lower)) {
                    out.add(alias);
                }
            }
        }
        return out;
    }

    private static int firstNonWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) return i;
        }
        return -1;
    }

    private static void discover(String basePackage) {
        try (ScanResult scan = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(basePackage)
                .scan()) {
            scan.getClassesWithAnnotation(CommandInfo.class.getName()).stream()
                    .map(ClassInfo::getName)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(CommandManager::loadCandidate);
        }
    }

    private static void loadCandidate(String className) {
        try {
            Class<?> type = Class.forName(className, false, CommandManager.class.getClassLoader());
            if (!ClientCommand.class.isAssignableFrom(type)) {
                DebugLog.config("Skipping annotated non-command: %s", type.getName());
                return;
            }
            int modifiers = type.getModifiers();
            if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers) || type.isAnnotation()) {
                DebugLog.config("Skipping non-concrete command class: %s", type.getName());
                return;
            }
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            register((ClientCommand) constructor.newInstance());
            DebugLog.config("Loaded command: %s", type.getName());
        } catch (NoSuchMethodException exception) {
            DebugLog.error("Failed to load command %s: no no-args constructor", exception, className);
        } catch (Throwable throwable) {
            DebugLog.error("Failed to load command: %s", throwable, className);
        }
    }
}
