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
import dev.loki.lovisual.util.player.NetworkStatsUtil;

import java.util.Locale;

@CommandInfo(
        id = "tps",
        aliases = "servertps",
        usage = "@tps",
        descriptionKey = "command.tps.description"
)
public final class TpsCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        float tps = NetworkStatsUtil.getTps(ctx.mc());
        CommandOutput.send(String.format(Locale.ROOT, "TPS: %.2f / 20.00", tps));
        return true;
    }
}
