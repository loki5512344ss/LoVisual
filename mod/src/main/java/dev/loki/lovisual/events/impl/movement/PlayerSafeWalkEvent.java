package dev.loki.lovisual.events.impl.movement;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlayerSafeWalkEvent extends Event {
    private boolean safeWalk;

}
