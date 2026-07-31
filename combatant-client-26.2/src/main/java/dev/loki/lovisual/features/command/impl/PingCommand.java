/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.command.impl;

import dev.loki.lovisual.features.command.ClientCommand;
import dev.loki.lovisual.features.command.CommandContext;
import dev.loki.lovisual.features.command.CommandInfo;
import dev.loki.lovisual.features.command.CommandOutput;
import dev.loki.lovisual.util.player.NetworkStatsUtil;

@CommandInfo(
        id = "ping",
        aliases = "latency",
        usage = "@ping",
        descriptionKey = "command.ping.description"
)
public final class PingCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        int ping = NetworkStatsUtil.getPing(ctx.mc());
        if (ping < 0) CommandOutput.warning("Ping is unavailable.");
        else CommandOutput.send("Ping: " + ping + " ms");
        return true;
    }
}
