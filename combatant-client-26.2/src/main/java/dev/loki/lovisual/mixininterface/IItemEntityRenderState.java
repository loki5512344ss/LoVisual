/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

public interface IItemEntityRenderState {
    boolean lovisual$isOnGround();

    void lovisual$setOnGround(boolean onGround);
}
