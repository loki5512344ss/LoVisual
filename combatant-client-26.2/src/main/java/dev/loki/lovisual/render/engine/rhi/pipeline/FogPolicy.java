/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.pipeline;

public enum FogPolicy {
    NONE,
    VANILLA,
    LOVISUAL,
    SHADERPACK;

    public boolean requiresWorldFogUniform() {
        return this == VANILLA || this == LOVISUAL || this == SHADERPACK;
    }
}
