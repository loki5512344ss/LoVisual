/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Input;
import dev.loki.lovisual.events.Event;

@Setter
@Getter
public final class MovementInputEvent extends Event {
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sneak;
    private boolean sprint;

    public MovementInputEvent(Input input) {
        this.forward = input.forward();
        this.backward = input.backward();
        this.left = input.left();
        this.right = input.right();
        this.jump = input.jump();
        this.sneak = input.shift();
        this.sprint = input.sprint();
    }

    public Input toPlayerInput() {
        return new Input(forward, backward, left, right, jump, sneak, sprint);
    }
}
