package dev.loki.lovisual.policy.plugin;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

/**
 * {@code /lvpolicy [reload|status]} — shows the active policy and re-reads
 * {@code config.yml} without a proxy restart.
 */
final class PolicyCommand implements SimpleCommand {

    private final LoVisualPolicyPlugin plugin;

    PolicyCommand(LoVisualPolicyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        String sub = args.length == 0 ? "status" : args[0];
        if ("reload".equalsIgnoreCase(sub)) {
            plugin.reload();
            source.sendPlainMessage("LoVisual policy: " + plugin.status());
            return;
        }
        if ("status".equalsIgnoreCase(sub)) {
            source.sendPlainMessage("LoVisual policy: " + plugin.status());
            return;
        }
        source.sendPlainMessage("Использование: /lvpolicy [reload|status]");
    }
}
