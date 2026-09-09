/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.input;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

public class SprintControlEvent extends Event {
    @Getter
    private final float forward;
    @Getter
    private final float sideways;
    @Getter
    private final Source source;
    @Setter
    private boolean sprint;
    public SprintControlEvent(float forward, float sideways, boolean sprint, Source source) {
        this.forward = forward;
        this.sideways = sideways;
        this.sprint = sprint;
        this.source = source;
    }

    public boolean isMoving() {
        return forward != 0.0f || sideways != 0.0f;
    }

    public boolean shouldSprint() {
        return sprint;
    }

    public enum Source {
        INPUT,
        MOVEMENT_TICK,
        NETWORK
    }
}
