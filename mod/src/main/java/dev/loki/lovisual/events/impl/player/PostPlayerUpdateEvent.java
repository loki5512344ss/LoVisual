package dev.loki.lovisual.events.impl.player;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PostPlayerUpdateEvent extends Event {
    private int iterations;

}
