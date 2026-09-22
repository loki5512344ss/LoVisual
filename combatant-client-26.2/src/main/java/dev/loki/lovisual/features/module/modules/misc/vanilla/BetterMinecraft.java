/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc.vanilla;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

//todo Description
@ModuleInfo(
        id = "betterminecraft",
        displayName = "BetterMinecraft",
        category = ModuleCategory.MISC
)
public final class BetterMinecraft extends Module {

    private final BooleanValue hideRecipeBook = bool("hide_recipe_book", true);
    private final BooleanValue tablistRelations = bool("tablist_relations", true);

    public boolean isHideRecipeBookEnabled() {
        return isEnabled() && hideRecipeBook.get();
    }

    public boolean isTablistRelationsEnabled() {
        return isEnabled() && tablistRelations.get();
    }
}
