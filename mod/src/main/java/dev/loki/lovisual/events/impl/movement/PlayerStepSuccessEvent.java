package dev.loki.lovisual.events.impl.movement;

import lombok.Getter;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.Event;

@Getter
public final class PlayerStepSuccessEvent extends Event {
    private final Vec3 adjustedVec;

    public PlayerStepSuccessEvent(Vec3 adjustedVec) {
        this.adjustedVec = adjustedVec;
    }

}
