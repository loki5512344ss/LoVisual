/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.impl.world;

import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.world.Gps;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Locale;

@CommandInfo(
        id = "gps",
        usage = "%gps <x> <y> <z> | %gps clear",
        descriptionKey = "command.gps.description"
)
public final class GpsCommand implements ClientCommand {

    @Override
    public boolean execute(CommandContext ctx) {
        Gps gps = Modules.get(Gps.class);
        if (gps == null) {
            CommandOutput.error("Gps module is unavailable.");
            return true;
        }

        String first = ctx.arg(0);
        if (first != null && "clear".equalsIgnoreCase(first.trim())) {
            gps.clearTarget();
            CommandOutput.success("Gps target cleared.");
            return true;
        }

        if (ctx.args().size() < 3) {
            CommandOutput.warning("Usage: " + metadata().usage());
            return true;
        }

        Integer x = parseInt(ctx.arg(0));
        Integer y = parseInt(ctx.arg(1));
        Integer z = parseInt(ctx.arg(2));
        if (x == null || y == null || z == null) {
            CommandOutput.error("Invalid coordinates. Use integers.");
            return true;
        }

        gps.setTarget(new BlockPos(x, y, z));
        CommandOutput.success(String.format(Locale.ROOT, "Gps target set: %d, %d, %d", x, y, z));
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex == 0) {
            String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
            return "clear".startsWith(lower) ? List.of("clear") : List.of();
        }
        return List.of();
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
