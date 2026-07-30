/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.impl;

import dev.loki.lovisual.config.ConfigPaths;
import dev.loki.lovisual.config.ConfigSerializer;
import dev.loki.lovisual.features.command.ClientCommand;
import dev.loki.lovisual.features.command.CommandContext;
import dev.loki.lovisual.features.command.CommandInfo;
import dev.loki.lovisual.features.command.CommandOutput;
import dev.loki.lovisual.features.module.ModuleManager;

import java.util.List;
import java.util.Locale;

@CommandInfo(
        id = "config",
        aliases = {"cfg", "settings"},
        usage = "@config [save|load|path]",
        descriptionKey = "command.config.description"
)
public final class ConfigCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        String action = ctx.arg(0) == null ? "save" : ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "save" -> {
                ModuleManager.saveAllModuleConfigs();
                ConfigSerializer.saveAll();
                CommandOutput.success("Client config saved.");
            }
            case "load", "reload" -> {
                ConfigSerializer.loadAll();
                ModuleManager.loadAllModuleConfigs();
                CommandOutput.success("Client config reloaded.");
            }
            case "path", "folder" -> CommandOutput.send("Config directory: " + ConfigPaths.root().toAbsolutePath());
            default -> CommandOutput.warning("Usage: " + metadata().usage());
        }
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex != 1) return List.of();
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return List.of("save", "load", "path").stream().filter(value -> value.startsWith(lower)).toList();
    }
}
