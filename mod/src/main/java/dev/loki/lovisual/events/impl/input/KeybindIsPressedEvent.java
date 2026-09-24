package dev.loki.lovisual.events.impl.input;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.KeyMapping;
import dev.loki.lovisual.events.Event;

@Getter
public final class KeybindIsPressedEvent extends Event {

    private final KeyMapping keyBinding;
    @Setter
    private boolean pressed;

    public KeybindIsPressedEvent(KeyMapping keyBinding, boolean pressed) {
        this.keyBinding = keyBinding;
        this.pressed = pressed;
    }

}
