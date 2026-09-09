/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.input;

import dev.loki.lovisual.events.Event;
import lombok.Getter;

/**
 * Rotation lifecycle event fired each tick in two phases.
 */
@Getter
public final class RotationUpdateEvent extends Event {
    private final Type type;

    public RotationUpdateEvent(Type type) {
        this.type = type == null ? Type.PRE : type;
    }

    public enum Type {
        PRE,
        POST
    }
}
