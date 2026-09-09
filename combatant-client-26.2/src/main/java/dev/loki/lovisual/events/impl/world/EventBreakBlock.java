/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.world;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import dev.loki.lovisual.events.Event;

@Getter
public class EventBreakBlock extends Event {
    private final BlockPos pos;

    public EventBreakBlock(BlockPos pos) {
        this.pos = pos;
    }

}
