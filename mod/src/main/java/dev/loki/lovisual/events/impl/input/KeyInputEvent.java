package dev.loki.lovisual.events.impl.input;

import lombok.Getter;
import net.minecraft.client.input.KeyEvent;
import dev.loki.lovisual.events.Event;

@Getter
public final class KeyInputEvent extends Event {
    private final int action;
    private final KeyEvent input;

    public KeyInputEvent(int action, KeyEvent input) {
        this.action = action;
        this.input = input;
    }

}
