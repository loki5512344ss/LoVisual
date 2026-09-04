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
import dev.loki.lovisual.render.iris.IrisCompatibilityFeature;
import dev.loki.lovisual.render.iris.IrisCompatibilityProfile;
import dev.loki.lovisual.render.iris.IrisRuntime;
import dev.loki.lovisual.render.iris.IrisRuntimeSnapshot;
import dev.loki.lovisual.render.iris.patch.ShaderPatchEngine;

import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(
        id = "iris",
        aliases = "shaderpack",
        usage = "@iris",
        descriptionKey = "command.iris.description"
)
public final class IrisCommand implements ClientCommand {
    private static String formatFeature(IrisCompatibilityFeature feature) {
        return feature.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean execute(CommandContext ctx) {
        IrisRuntimeSnapshot snapshot = IrisRuntime.snapshot();
        CommandOutput.send(snapshot.shortLine());

        if (!snapshot.modLoaded()) {
            return true;
        }
        CommandOutput.send("Iris API: " + (snapshot.apiAvailable() ? "available" : "unavailable")
                + ", status: " + snapshot.status()
                + ", shadow pass: " + snapshot.renderingShadowPass());

        IrisCompatibilityProfile profile = snapshot.profile();
        String features = profile.features().isEmpty()
                ? "none"
                : profile.features().stream()
                .map(IrisCommand::formatFeature)
                .collect(Collectors.joining(", "));
        CommandOutput.send("Iris profile: " + profile.getId()
                + " (" + profile.displayName() + "), features: " + features);
        List<String> patchDiagnostics = ShaderPatchEngine.diagnostics();
        CommandOutput.send("Iris patch compiler: " + (patchDiagnostics.isEmpty() ? "no session data" : "session data follows"));
        for (String diagnostic : patchDiagnostics) {
            CommandOutput.send("patch " + diagnostic);
        }
        return true;
    }
}
