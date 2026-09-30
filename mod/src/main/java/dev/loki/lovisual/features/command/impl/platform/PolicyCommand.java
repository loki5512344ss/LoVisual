package dev.loki.lovisual.features.command.impl.platform;

import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.serverpolicy.ServerPolicy;
import dev.loki.lovisual.features.platform.serverpolicy.ServerPolicyState;

/**
 * {@code %policy} — shows what the proxy currently demands of this client:
 * nothing (not on a LoVisual proxy), or the active mode, denied modules,
 * backend server and reason received on the {@code lovisual:policy} channel.
 */
@CommandInfo(
        id = "policy",
        usage = "%policy",
        descriptionKey = "command.policy.description"
)
public final class PolicyCommand implements ClientCommand {

    @Override
    public boolean execute(CommandContext ctx) {
        ServerPolicy policy = ServerPolicyState.current();
        if (policy == null) {
            CommandOutput.send("No server policy: not connected to a LoVisual proxy, or none configured.");
            return true;
        }
        StringBuilder line = new StringBuilder("Server policy: ").append(policy.mode());
        line.append(policy.deny().isEmpty()
                ? ", no denied modules"
                : ", denies: " + String.join(", ", policy.deny()));
        line.append(", server=").append(policy.server().isEmpty() ? "-" : policy.server());
        line.append(", reason=").append(policy.reason().isEmpty() ? "-" : policy.reason());
        CommandOutput.send(line.toString());
        return true;
    }
}
