/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.fade;

public interface ViewObstructionFadeState {
    public boolean lovisual$isViewObstructionFadeActive();

    public float lovisual$getViewObstructionFadeAlpha();

    public void lovisual$setViewObstructionFadeActive(boolean active);

    public void lovisual$setViewObstructionFadeAlpha(float alpha);
}
