/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Event {
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }
}
