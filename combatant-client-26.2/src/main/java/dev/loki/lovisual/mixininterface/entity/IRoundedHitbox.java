/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface.entity;

public interface IRoundedHitbox {
    void lovisual$setRoundedHitbox(float radius);

    void lovisual$clearRoundedHitbox();

    float lovisual$getRoundedHitbox();

    boolean lovisual$useRoundedHitbox();
}
