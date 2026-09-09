/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.esp.overlay;

import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;

/**
 * Делает невидимых игроков видимыми: модель рендерится полупрозрачной,
 * сила прозрачности регулируется слайдером.
 */
@ModuleInfo(
        id = "invisesp",
        displayName = "InvisESP",
        aliases = {"invis"},
        category = ModuleCategory.VISUALS,
        description = "module.invisesp.description"
)
public final class InvisEsp extends Module {

    private final NumberValue<Float> opacity =
            num("invisesp_opacity", "opacity", 0.5f, 0.1f, 1.0f);

    {
        setDefaultBind("L");
    }

    public float getOpacity() {
        return opacity.get();
    }

    /** Прозрачный рендер имеет смысл только ниже полной непрозрачности. */
    public boolean isTranslucentReveal() {
        return opacity.get() < 0.99f;
    }
}
