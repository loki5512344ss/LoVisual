/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.impl;

import dev.loki.lovisual.features.command.ClientCommand;
import dev.loki.lovisual.features.command.CommandContext;
import dev.loki.lovisual.features.command.CommandInfo;
import dev.loki.lovisual.features.command.CommandOutput;

import java.util.Locale;

@CommandInfo(
        id = "coordinates",
        aliases = {"coords", "position", "pos"},
        usage = "@coordinates",
        descriptionKey = "command.coordinates.description"
)
public final class CoordinatesCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        if (ctx.mc() == null || ctx.mc().player == null) {
            CommandOutput.error("Player is unavailable.");
            return true;
        }
        CommandOutput.send(String.format(Locale.ROOT, "Position: %.2f, %.2f, %.2f | Block: %d, %d, %d",
                ctx.mc().player.getX(), ctx.mc().player.getY(), ctx.mc().player.getZ(),
                ctx.mc().player.getBlockX(), ctx.mc().player.getBlockY(), ctx.mc().player.getBlockZ()));
        return true;
    }
}
