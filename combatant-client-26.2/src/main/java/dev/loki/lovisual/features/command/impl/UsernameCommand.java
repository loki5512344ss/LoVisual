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

import java.util.List;

@CommandInfo(
        id = "username",
        aliases = {"name", "ign", "whoami"},
        usage = "%username",
        descriptionKey = "command.username.description"
)
public final class UsernameCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        if (ctx.mc() == null || ctx.mc().player == null) {
            CommandOutput.error("Player is unavailable.");
            return true;
        }
        var profile = ctx.mc().player.getGameProfile();
        CommandOutput.send("Username: " + profile.name() + " | UUID: " + profile.id());
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        return List.of();
    }
}
