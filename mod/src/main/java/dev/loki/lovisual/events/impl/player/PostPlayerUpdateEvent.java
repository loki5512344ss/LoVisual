/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.player;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PostPlayerUpdateEvent extends Event {
    private int iterations;

}
