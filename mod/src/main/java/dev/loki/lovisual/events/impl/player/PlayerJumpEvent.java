package dev.loki.lovisual.events.impl.player;

import dev.loki.lovisual.events.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class PlayerJumpEvent extends Event {
    private float yaw;
    private float motion;

    public PlayerJumpEvent(float yaw, float motion) {
        this.yaw = yaw;
        this.motion = motion;
    }

}
