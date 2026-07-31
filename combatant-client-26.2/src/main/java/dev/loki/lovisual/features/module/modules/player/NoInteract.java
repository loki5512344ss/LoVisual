/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
//todo Description
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
