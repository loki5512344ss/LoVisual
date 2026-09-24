package dev.loki.lovisual.features.command.impl.info;

import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.util.player.interaction.NetworkStatsUtil;

import java.util.Locale;

@CommandInfo(
        id = "tps",
        aliases = "servertps",
        usage = "%tps",
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
