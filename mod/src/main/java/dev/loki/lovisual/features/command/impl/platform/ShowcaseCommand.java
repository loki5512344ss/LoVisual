package dev.loki.lovisual.features.command.impl.platform;

import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.showcase.ShowcaseClient;
import dev.loki.lovisual.features.platform.showcase.ShowcaseFormat;

import java.util.List;
import java.util.Locale;

/**
 * Browses the public config showcase from the game: {@code %showcase} lists a
 * page, {@code %showcase load <n>} applies a listing without saving it,
 * {@code %showcase copy <n> <1-4>} keeps it in an empty cloud slot.
 */
@CommandInfo(
        id = "showcase",
        aliases = {"shop"},
        usage = "%showcase [new|popular] [page] | load <n|id> | copy <n|id> <1-4>",
        descriptionKey = "command.showcase.description"
)
public final class ShowcaseCommand implements ClientCommand {

    @Override
    public boolean execute(CommandContext ctx) {
        String first = ctx.arg(0) == null ? "" : ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (first) {
            case "load", "try" -> ShowcaseClient.apply(ctx.arg(1));
            case "copy" -> ShowcaseClient.copy(ctx.arg(1), ctx.arg(2) != null && ctx.arg(2).matches("[1-4]")
                    ? Integer.parseInt(ctx.arg(2)) : -1);
            default -> browse(ctx);
        }
        return true;
    }

    private void browse(CommandContext ctx) {
        String sort = ShowcaseFormat.parseSort(ctx.arg(0));
        if (sort == null) {
            CommandOutput.warning("Usage: " + metadata().usage());
            return;
        }
        int page = ctx.arg(1) != null && ctx.arg(1).matches("\\d{1,4}") ? Integer.parseInt(ctx.arg(1)) : 0;
        ShowcaseClient.browse(sort, page);
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex != 1) return List.of();
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return List.of("new", "popular", "load", "copy").stream().filter(v -> v.startsWith(lower)).toList();
    }
}
