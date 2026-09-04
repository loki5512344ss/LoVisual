/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.Event;

@Getter
public class PlayerMoveEvent extends Event {
    private final MoverType type;
    @Setter
    private Vec3 movement;

    public PlayerMoveEvent(MoverType type, Vec3 movement) {
        this.type = type;
        this.movement = movement;
    }

}
