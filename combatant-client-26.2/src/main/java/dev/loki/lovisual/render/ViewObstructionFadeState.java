/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render;

public interface ViewObstructionFadeState {
    boolean lovisual$isViewObstructionFadeActive();

    float lovisual$getViewObstructionFadeAlpha();

    void lovisual$setViewObstructionFadeActive(boolean active);

    void lovisual$setViewObstructionFadeAlpha(float alpha);

    boolean lovisual$isSeeInvisibleFadeActive();

    void lovisual$setSeeInvisibleFadeActive(boolean active);
}
