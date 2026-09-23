/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.light;

import net.minecraft.util.Mth;

public enum LightmapState {
    ;
    private static volatile float baseAmbient = 0.0f;
    private static volatile float ambient = 0.0f;

    public static void setAmbient(float baseValue, float value) {
        baseAmbient = Mth.clamp(baseValue, 0.0f, 1.0f);
        ambient = Mth.clamp(value, 0.0f, 1.0f);
    }

    public static float getAmbient() {
        return ambient;
    }

    public static float getBaseAmbient() {
        return baseAmbient;
    }

    /**
     * Returns override ambient if raised above base, otherwise -1.
     */
    public static float getOverrideAmbient() {
        return ambient > baseAmbient + 1.0e-4f ? ambient : -1.0f;
    }
}
