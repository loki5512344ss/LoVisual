/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module;

public enum WorldPhase {
    NONE,

    BEFORE_ENTITIES,     // WorldRenderEvents.BEFORE_ENTITIES
    AFTER_ENTITIES,      // WorldRenderEvents.AFTER_ENTITIES
    BEFORE_TRANSLUCENT,  // WorldRenderEvents.BEFORE_TRANSLUCENT
    END_MAIN,            // WorldRenderEvents.END_MAIN (аналог старого LAST)
    AFTER_POST_PROCESS   // After PRE_HAND postprocess, before hand render
}
