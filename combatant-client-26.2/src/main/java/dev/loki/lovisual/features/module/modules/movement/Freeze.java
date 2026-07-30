/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.movement;

import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;

//todo Description
@ModuleInfo(
        id = "freeze",
        displayName = "Freeze",
        category = ModuleCategory.MOVEMENT
)
public final class Freeze extends Module {

    public Freeze() {
        setDefaultBind("COMMA");
    }
}
