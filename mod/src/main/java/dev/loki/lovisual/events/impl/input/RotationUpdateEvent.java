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
