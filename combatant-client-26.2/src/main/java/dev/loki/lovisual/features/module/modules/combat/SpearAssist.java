/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.config.values.BooleanMapValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;

//todo Description
@ModuleInfo(
        id = "spearassist",
        displayName = "SpearAssist",
        category = ModuleCategory.COMBAT
)
public class SpearAssist extends Module {

    private static final String SETTING_TOGGLES = "toggles";

    private final BooleanMapValue toggles = group(
            "spearassist_toggles",
            SETTING_TOGGLES,
            new java.util.LinkedHashMap<>() {{
                put("respect_cooldown", true);
            }}
    );

    public static boolean isSpear(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.get(DataComponents.KINETIC_WEAPON) != null;
    }

    public boolean respectCooldownEnabled() {
        return toggles.get("respect_cooldown");
    }
}
