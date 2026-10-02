package dev.loki.lovisual.features.command.impl.platform;

import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.gui.mainmenu.cloud.CloudScreen;
import dev.loki.lovisual.util.screen.ClientScreen;

/** Opens the cloud configs screen: account link, the four slots and the showcase. */
@CommandInfo(
        id = "cloud",
        usage = "%cloud",
        descriptionKey = "command.cloud.description"
)
public final class CloudCommand implements ClientCommand {

    @Override
    public boolean execute(CommandContext ctx) {
        // The chat screen closes right after a command runs and would replace ours: open on the next tick.
        ctx.mc().execute(() -> ClientScreen.show(ctx.mc(), new CloudScreen(null)));
        return true;
    }
}
