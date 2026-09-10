/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.scene;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

/**
 * Физика предметов на земле: предметы лежат плашмя, крутятся в воздухе,
 * без ваниллового покачивания; опционально уменьшенный размер.
 */
@ModuleInfo(
        id = "itemphysic",
        displayName = "Item Physic",
        aliases = {"itemphysic", "physics"},
        category = ModuleCategory.VISUALS,
        description = "module.itemphysic.description"
)
public final class ItemPhysic extends Module {

    private final BooleanValue reduceSize =
            bool("itemphysicReduceSize", "reduce_size", false);
    private final BooleanValue layFlat =
            bool("itemphysicLayFlat", "lay_flat", true);
    private final BooleanValue spinInAir =
            bool("itemphysicSpinInAir", "spin_in_air", true);
    private final BooleanValue noBob =
            bool("itemphysicNoBob", "no_bob", true);

    public boolean shouldReduceSize() {
        return isEnabled() && reduceSize.get();
    }

    public boolean shouldLayFlat() {
        return isEnabled() && layFlat.get();
    }

    public boolean shouldSpinInAir() {
        return isEnabled() && spinInAir.get();
    }

    public boolean shouldCancelBob() {
        return isEnabled() && noBob.get();
    }
}
