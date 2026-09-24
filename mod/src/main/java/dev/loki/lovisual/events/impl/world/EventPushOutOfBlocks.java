package dev.loki.lovisual.events.impl.world;

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
