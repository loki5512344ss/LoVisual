/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.entity;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import dev.loki.lovisual.events.Event;

@Getter
public final class AttackEntityEvent extends Event {
    private final Player player;
    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }

}
