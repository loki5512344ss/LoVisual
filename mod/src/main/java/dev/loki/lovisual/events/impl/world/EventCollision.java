package dev.loki.lovisual.events.impl.world;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import dev.loki.lovisual.events.Event;

@Getter
public class EventCollision extends Event {
    private final BlockPos pos;
    @Setter
    private BlockState state;

    public EventCollision(BlockState state, BlockPos pos) {
        this.state = state;
        this.pos = pos;
    }

}
