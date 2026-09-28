package dev.loki.lovisual.features.command.impl.client;

import dev.loki.lovisual.config.io.ConfigPaths;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.platform.config.ConfigCloudUploader;
import dev.loki.lovisual.features.platform.config.ConfigShareLoader;

import java.util.List;
import java.util.Locale;

@CommandInfo(
        id = "config",
        aliases = {"cfg", "settings"},
        usage = "%config [save [1-4 name]|load [code]|path]",
        descriptionKey = "command.config.description"
)
public final class ConfigCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        String action = ctx.arg(0) == null ? "save" : ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "save" -> {
                String slotArg = ctx.arg(1);
                if (slotArg != null && slotArg.matches("[1-4]")) {
                    // %config save <1-4> [name]: snapshot current modules and
                    // upload to the linked account's cloud slot (PUT /configs/{slot}).
                    ConfigCloudUploader.upload(Integer.parseInt(slotArg), ctx.arg(2));
                    break;
                }
                ModuleManager.saveAllModuleConfigs();
                ConfigSerializer.saveAll();
                CommandOutput.success("Client config saved.");
            }
            case "load", "reload" -> {
                if (ctx.arg(1) != null) {
                    // %config load <code>: fetch a shared config from the backend
                    // (GET /configs/shared/{code}) instead of reloading local files.
                    ConfigShareLoader.load(ctx.arg(1));
                    break;
                }
                ConfigSerializer.loadAll();
                ModuleManager.loadAllModuleConfigs();
                CommandOutput.success("Client config reloaded.");
            }
            case "path", "folder" -> CommandOutput.send("Config directory: " + ConfigPaths.root().toAbsolutePath());
            default -> CommandOutput.warning("Usage: " + metadata().usage());
        }
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex != 1) return List.of();
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return List.of("save", "load", "path").stream().filter(value -> value.startsWith(lower)).toList();
    }
}
