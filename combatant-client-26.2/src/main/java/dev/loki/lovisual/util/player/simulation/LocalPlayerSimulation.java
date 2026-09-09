/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.simulation;

import net.minecraft.world.entity.player.Player;

public final class LocalPlayerSimulation extends PlayerMovementSimulation {

    LocalPlayerSimulation(Player player, SimulatedInput input) {
        super(player, input);
    }
}
