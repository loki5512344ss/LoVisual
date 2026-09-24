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
