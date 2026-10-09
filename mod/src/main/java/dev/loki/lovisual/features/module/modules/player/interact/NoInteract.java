package dev.loki.lovisual.features.module.modules.player.interact;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
@ModuleInfo(
        id = "nointeract",
        displayName = "NoInteract",
        category = ModuleCategory.PLAYER
)
public final class NoInteract extends Module {
    private final BooleanValue onlyKillAura = bool("only_kill_aura", true);

    public boolean shouldBlockBlockInteraction() {
        return shouldBlockBlockInteraction(
                isEnabled(),
                onlyKillAura.get(),
                false
        );
    }

    static boolean shouldBlockBlockInteraction(
            boolean noInteractEnabled,
            boolean onlyKillAura,
            boolean killAuraEnabled
    ) {
        return noInteractEnabled && (!onlyKillAura || killAuraEnabled);
    }
}
