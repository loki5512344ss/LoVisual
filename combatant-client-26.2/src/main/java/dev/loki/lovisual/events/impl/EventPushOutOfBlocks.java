/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import dev.loki.lovisual.events.Event;
import lombok.Getter;

@Getter
public class EventPushOutOfBlocks extends Event {
    private final double x;
    private final double z;

    public EventPushOutOfBlocks(double x, double z) {
        this.x = x;
        this.z = z;
    }

}
