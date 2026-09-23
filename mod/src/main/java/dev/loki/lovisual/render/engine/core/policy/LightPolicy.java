/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.core.policy;

/**
 * Placeholder for LoVisual lighting policy. Kept explicit so fullbright/ambient/shaderpack hooks do not leak into renderers.
 */
public interface LightPolicy {
    LightPolicy VANILLA = new LightPolicy() {
    };
}
