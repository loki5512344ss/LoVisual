package dev.loki.lovisual.util.player.simulation;

import net.minecraft.world.entity.player.Player;
import dev.loki.lovisual.util.player.simulation.PlayerMovementSimulation;
import dev.loki.lovisual.util.player.simulation.SimulatedInput;

public final class LocalPlayerSimulation extends PlayerMovementSimulation {

    LocalPlayerSimulation(Player player, SimulatedInput input) {
        super(player, input);
    }
}
