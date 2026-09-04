/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.Event;

@Setter
@Getter
public final class FireworkEvent extends Event {
    private Vec3 vector;

    public FireworkEvent(Vec3 vector) {
        this.vector = vector;
    }

}
