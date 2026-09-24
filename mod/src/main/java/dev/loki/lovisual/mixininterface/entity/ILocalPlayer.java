package dev.loki.lovisual.mixininterface.entity;

public interface ILocalPlayer {
    float lovisual$getLastYaw();

    float lovisual$getLastPitch();

    void lovisual$setLastYaw(float yaw);

    void lovisual$setLastPitch(float pitch);
}
