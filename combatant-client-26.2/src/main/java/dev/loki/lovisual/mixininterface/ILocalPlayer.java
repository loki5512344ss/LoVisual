/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

public interface ILocalPlayer {
    float lovisual$getLastYaw();

    float lovisual$getLastPitch();

    void lovisual$setLastYaw(float yaw);

    void lovisual$setLastPitch(float pitch);
}
