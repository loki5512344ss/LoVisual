package dev.loki.lovisual.features.command.impl.info;

import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.util.player.interaction.NetworkStatsUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.stats.Ping;

@CommandInfo(
        id = "ping",
        aliases = "latency",
        usage = "%ping",
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
