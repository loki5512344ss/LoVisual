/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.visuals;

/**
 * Render graph ordering for the custom world visual stack.
 */
public enum LoVisualVisualPhase {
    PREPARE,
    SHADOWS,
    VOLUMETRICS,
    REFLECTIONS,
    COMPOSITE
}
