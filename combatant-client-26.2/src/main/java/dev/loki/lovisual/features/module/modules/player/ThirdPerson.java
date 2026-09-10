/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

/**
 * Free third-person look: while the bind is held the camera orbits freely and
 * the player's movement direction stays untouched. No camera entity is
 * spawned — only the vanilla third-person camera is extended.
 */
@ModuleInfo(
        id = "thirdperson",
        displayName = "Third Person",
        aliases = {"thirdperson", "3p"},
        category = ModuleCategory.PLAYER
)
public class ThirdPerson extends Module {
    private static final String SETTING_LOOK = "look";
    private static final String SETTING_DISTANCE = "distance";

    private final NumberValue<Double> distance = num("thirdPersonDistance", SETTING_DISTANCE, 4.0, 1.0, 16.0);

    public float orbitYaw;
    public float orbitPitch;

    {
        action(SETTING_LOOK, "L", BindMode.HOLD);
    }

    /**
     * Camera input is captured only while the action bind is held; otherwise
     * the vanilla turnPlayer path stays in charge.
     */
    public boolean isLooking() {
        return isEnabled() && isActionHeld(SETTING_LOOK);
    }

    public double distance() {
        return distance.get();
    }

    /** Pure pitch clamp helper shared with the mouse mixin. */
    public static float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }
}
