/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.draw;

/**
 * Convenience paint factory for Renderer2D's normalized draw API.
 */
public enum UiPaints {
    ;

    public static UiPaint solid(int argb) {
        return UiPaint.solid(argb);
    }

    public static UiPaint corners(int tl, int tr, int br, int bl) {
        return UiPaint.corners(tl, tr, br, bl);
    }

    public static UiPaint linear(int startArgb, int endArgb, float angleDeg) {
        return UiPaint.linear(startArgb, endArgb, angleDeg, 0.0f);
    }

    public static UiPaint linear(int startArgb, int endArgb, float angleDeg, float offsetPx) {
        return UiPaint.linear(startArgb, endArgb, angleDeg, offsetPx);
    }
}
