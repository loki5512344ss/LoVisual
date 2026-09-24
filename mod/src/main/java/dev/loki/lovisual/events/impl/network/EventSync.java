package dev.loki.lovisual.events.impl.network;

import dev.loki.lovisual.events.Event;
import lombok.Getter;

public class EventSync extends Event {
    @Getter
    private final float originalYaw;
    @Getter
    private final float originalPitch;
    @Getter
    private float yaw;
    @Getter
    private float pitch;
    private boolean rotationOverride;
    @Getter
    private boolean silentRotation;
    @Getter
    private Runnable postAction;

    public EventSync(float yaw, float pitch) {
        this.originalYaw = yaw;
        this.originalPitch = pitch;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.rotationOverride = true;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.rotationOverride = true;
    }

    public void setRotation(float yaw, float pitch, boolean silent) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.rotationOverride = true;
        this.silentRotation = silent;
    }

    public boolean hasRotationOverride() {
        return rotationOverride;
    }

    public void addPostAction(Runnable r) {
        if (r == null) return;
        if (postAction == null) {
            postAction = r;
        } else {
            Runnable prev = postAction;
            postAction = () -> {
                prev.run();
                r.run();
            };
        }
    }

}
