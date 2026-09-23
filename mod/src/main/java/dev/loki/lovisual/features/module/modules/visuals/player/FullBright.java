/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.player;

import net.minecraft.util.Mth;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.render.LightmapModifyEvent;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

//todo Description
@ModuleInfo(
        id = "fullbright",
        displayName = "FullBright",
        category = ModuleCategory.VISUALS
)
public class FullBright extends Module {

    private static final String SETTING_MIN_LIGHT = "min_light";
    private final NumberValue<Integer> minLight =
            num("fullBrightMinLight", SETTING_MIN_LIGHT, 15, 1, 15);

    public int getMinLight() {
        return isEnabled() ? minLight.get() : 0;
    }

    public float getMinLightStrength() {
        return isEnabled() ? Mth.clamp(minLight.get(), 1, 15) / 15.0f : 0.0f;
    }

    @EventHandler
    private void onLightmapState(LightmapModifyEvent event) {
        if (!isEnabled()) return;
        event.raiseMinimumLight(getMinLightStrength());
    }
}
