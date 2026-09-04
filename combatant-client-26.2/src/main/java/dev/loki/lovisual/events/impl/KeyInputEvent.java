/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

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
