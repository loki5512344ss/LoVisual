/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

public interface IRoundedHitbox {
    void lovisual$setRoundedHitbox(float radius);

    void lovisual$clearRoundedHitbox();

    float lovisual$getRoundedHitbox();

    boolean lovisual$useRoundedHitbox();
}
