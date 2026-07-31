/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

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
