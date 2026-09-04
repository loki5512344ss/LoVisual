/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import net.minecraft.client.Minecraft;

/**
 * Smooths and adds a subtle hand-held sway to first-person item rendering, so weapons and tools
 * settle into place with an ease-out-back curve instead of vanilla's linear equip motion.
 */
@ModuleInfo(
        id = "holdmyitems",
        displayName = "Item Holder",
        aliases = {"itemholder"},
        category = ModuleCategory.VISUALS,
        description = "Плавная анимация удержания предмета в руке вместо линейного ваниллового equip"
)
public final class HoldMyItems extends Module {

    public final Minecraft mc = Minecraft.getInstance();

    public final NumberValue<Float> swayIntensity = num("sway_intensity", 1.0f, 0.0f, 2.0f);
    public final NumberValue<Float> easeStrength = num("ease_strength", 1.6f, 0.5f, 3.5f);
    public final BooleanValue offhandToo = bool("offhand", true);

    /**
     * Ease-out-back: overshoots slightly past 1.0 before settling, giving the equip motion weight.
     * Generalized with a configurable strength.
     */
    public float easeOutBack(float x) {
        float c1 = easeStrength.get();
        float c3 = c1 + 1.0f;
        float t = x - 1.0f;
        return 1.0f + c3 * t * t * t + c1 * t * t;
    }

    /**
     * Reshapes the raw [0,1] equip progress with the ease-out-back curve, then blends by intensity
     * so a intensity of 0 falls back to vanilla's linear motion.
     */
    public float shapeEquipProgress(float linear) {
        float eased = easeOutBack(linear);
        float amount = swayIntensity.get();
        return linear + (eased - linear) * amount;
    }
}
