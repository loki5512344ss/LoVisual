package dev.loki.lovisual.features.command.impl.world;

import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.Balancer;
import dev.loki.lovisual.util.mctiers.MCTiersCache;
import dev.loki.lovisual.util.mctiers.MCTiersTier;

import java.util.Locale;

/**
 * {@code %tier <ник> [режим]} — тир игрока с mctiers.com. Первый запрос уходит
 * в фон (см. {@link MCTiersCache}) и почти всегда возвращает null сразу же —
 * повторный вызов команды через пару секунд покажет результат из кэша.
 */
@CommandInfo(
        id = "tier",
        aliases = {"mctier"},
        usage = "%tier <player> [vanilla|uhc|pot|nethop|smp|sword|axe|mace]",
        descriptionKey = "command.tier.description"
)
public final class TierCommand implements ClientCommand {

    @Override
    public boolean execute(CommandContext ctx) {
        String player = ctx.arg(0);
        if (player == null || player.isBlank()) {
            CommandOutput.warning("Usage: " + metadata().usage());
            return true;
        }

        Balancer balancer = Modules.get(Balancer.class);
        String fixType = balancer != null ? balancer.fixType() : "Vanilla";
        String override = ctx.arg(1);
        if (override != null && !override.isBlank()) {
            fixType = normalizeFixType(override);
        }

        String apiMode = MCTiersTier.modeApiKey(fixType);
        MCTiersTier tier = MCTiersCache.getTier(player, apiMode);
        if (tier == null) {
            CommandOutput.send("Fetching mctiers.com data for " + player + "... run %tier again in a moment.");
            return true;
        }

        CommandOutput.success(String.format(Locale.ROOT, "%s — %s: %s%s",
                player, fixType, tier.label(), tier.retired() ? " (retired)" : ""));
        return true;
    }

    private static String normalizeFixType(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "uhc" -> "UHC";
            case "pot" -> "Pot";
            case "nethop", "neth_pot", "netherite" -> "NethOP";
            case "smp" -> "SMP";
            case "sword" -> "Sword";
            case "axe" -> "Axe";
            case "mace" -> "Mace";
            default -> "Vanilla";
        };
    }
}
