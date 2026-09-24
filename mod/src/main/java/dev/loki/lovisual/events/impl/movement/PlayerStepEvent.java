package dev.loki.lovisual.events.impl.movement;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class PlayerStepEvent extends Event {
    private float height;

    public PlayerStepEvent(float height) {
        this.height = height;
    }

}
