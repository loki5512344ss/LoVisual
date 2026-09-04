/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.draw;

public record UiEffectSpec(UiEffectKind kind,
                           UiShape shape,
                           float intensity,
                           float thickness,
                           float extra0,
                           float extra1,
                           int argb) {
    public static UiEffectSpec blur(UiBoxShape box, double radius, int argb) {
        return blur(UiShape.box(box), radius, argb);
    }

    public static UiEffectSpec blur(UiShape shape, double radius, int argb) {
        return new UiEffectSpec(UiEffectKind.BLUR, shape, (float) Math.max(0.0, radius), 0f, 0f, 0f, argb);
    }

    public static UiEffectSpec liquidGlass(UiBoxShape box, double radius, double thickness, double distortion, int argb) {
        return liquidGlass(UiShape.box(box), radius, thickness, distortion, argb);
    }

    public static UiEffectSpec liquidGlass(UiShape shape, double radius, double thickness, double distortion, int argb) {
        return new UiEffectSpec(UiEffectKind.LIQUID_GLASS, shape,
                (float) Math.max(0.0, radius), (float) Math.max(0.0, thickness), (float) distortion, 0f, argb);
    }
}
