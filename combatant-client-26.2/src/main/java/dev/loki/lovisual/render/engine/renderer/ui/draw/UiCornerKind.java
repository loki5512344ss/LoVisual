/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.draw;

/**
 * Per-corner geometry mode for flexible UI boxes.
 */
public enum UiCornerKind {
    SQUARE(0.0f),
    ROUNDED(1.0f),
    CHAMFERED(2.0f),
    CONCAVE_ROUNDED(3.0f),
    NOTCHED(4.0f),
    CUSTOM(8.0f);

    private final float shaderCode;

    UiCornerKind(float shaderCode) {
        this.shaderCode = shaderCode;
    }

    public float shaderCode() {
        return shaderCode;
    }
}
