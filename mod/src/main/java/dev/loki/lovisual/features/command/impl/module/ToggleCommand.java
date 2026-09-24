package dev.loki.lovisual.features.command.impl.module;

import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.core.Module;

import java.util.List;
import java.util.Locale;
import dev.loki.lovisual.features.command.impl.CommandUtils;

@CommandInfo(
        id = "toggle",
        aliases = "t",
        usage = "%toggle <module> [on|off|toggle]",
        descriptionKey = "command.toggle.description"
)
public final class ToggleCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        Module module = CommandUtils.findModule(ctx.arg(0));
        if (module == null) {
            CommandOutput.error(ctx.arg(0) == null ? "Usage: " + metadata().usage() : "Module not found: " + ctx.arg(0));
            return true;
        }
        String stateArg = ctx.arg(1);
        if (!CommandUtils.isState(stateArg)) {
            CommandOutput.warning("Usage: " + metadata().usage());
            return true;
        }
        boolean next = CommandUtils.parseState(stateArg, module.isEnabled());
        if (next && !module.isAvailable()) {
            CommandOutput.error(module.getDisplayName() + " is unavailable: " + module.getAvailabilityReason());
            return true;
        }
        module.setEnabled(next);
        if (module.isEnabled() == next) {
            CommandOutput.success(module.getDisplayName() + ": " + (next ? "ON" : "OFF"));
        } else {
            CommandOutput.error("Could not change " + module.getDisplayName() + '.');
        }
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
