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
import dev.loki.lovisual.runtime.ClientRuntime;
import dev.loki.lovisual.runtime.ClientRuntimeState;

import java.nio.file.Path;
import java.util.List;

@CommandInfo(
        id = "runtime",
        aliases = {"panic", "jarreplace"},
        usage = "@runtime [status|panic|resume|jar|source]",
        descriptionKey = "command.runtime.description"
)
public final class RuntimeCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        String action = ctx.arg(0);
        if (action == null || action.equalsIgnoreCase("status")) {
            sendStatus();
            return true;
        }
        if (action.equalsIgnoreCase("panic")) {
            ClientRuntime.toggleSoftPanic("runtime command");
            CommandOutput.send("Runtime state: " + ClientRuntime.state());
            return true;
        }
        if (action.equalsIgnoreCase("resume")) {
            if (ClientRuntime.state() == ClientRuntimeState.SOFT_PANIC) {
                ClientRuntime.exitSoftPanic("runtime command");
            }
            CommandOutput.send("Runtime state: " + ClientRuntime.state());
            return true;
        }
        if (action.equalsIgnoreCase("jar") || action.equalsIgnoreCase("replace")) {
            ClientRuntime.prepareJarReplacement("runtime command");
            CommandOutput.send("Client is fully disabled for safe JAR replacement. Restart Minecraft to use the new JAR.");
            return true;
        }
        if (action.equalsIgnoreCase("source")) {
            Path jar = ClientRuntime.loaderBridge().currentModJarPath();
            CommandOutput.send("Loader: standardFabric=" + ClientRuntime.loaderBridge().isStandardFabric()
                    + ", safeJarReplacement=" + ClientRuntime.loaderBridge().supportsSafeJarReplacement());
            CommandOutput.send("Current mod source: " + (jar == null ? "<unknown>" : jar.toAbsolutePath()));
            CommandOutput.send("LoVisual class source: "
                    + ClientRuntime.loaderBridge().describeClassSource("dev.loki.lovisual.LoVisual"));
            return true;
        }
        CommandOutput.send("Usage: " + metadata().usage());
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex != 1) return List.of();
        String lower = token == null ? "" : token.toLowerCase();
        return List.of("status", "panic", "resume", "jar", "source").stream()
                .filter(value -> lower.isEmpty() || value.startsWith(lower))
                .toList();
    }

    private void sendStatus() {
        for (String line : ClientRuntime.diagnostics().lines()) {
            CommandOutput.send(line);
        }
    }
}
