/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.impl;

import dev.loki.lovisual.features.command.ClientCommand;
import dev.loki.lovisual.features.command.CommandContext;
import dev.loki.lovisual.features.command.CommandInfo;
import dev.loki.lovisual.features.command.CommandOutput;
import dev.loki.lovisual.features.module.Module;

import java.util.List;
import java.util.Locale;

@CommandInfo(
        id = "hide",
        aliases = {"modulevisibility", "showmodule"},
        usage = "@hide <module> [on|off|toggle]",
        descriptionKey = "command.hide.description"
)
public final class HideCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        Module module = CommandUtils.findModule(ctx.arg(0));
        if (module == null) {
            CommandOutput.error(ctx.arg(0) == null ? "Usage: " + metadata().usage() : "Module not found: " + ctx.arg(0));
            return true;
        }
        String action = ctx.arg(1);
        if (!CommandUtils.isState(action)) {
            CommandOutput.warning("Usage: " + metadata().usage());
            return true;
        }
        boolean hidden = CommandUtils.parseState(action, !module.isShownInModuleList());
        module.setShownInModuleList(!hidden);
        CommandOutput.success(module.getDisplayName() + (hidden ? " hidden from ModuleList" : " shown in ModuleList"));
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex == 1) return CommandUtils.suggestModules(token);
        if (argIndex == 2) {
            String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
            return List.of("on", "off", "toggle").stream().filter(value -> value.startsWith(lower)).toList();
        }
        return List.of();
    }
}
