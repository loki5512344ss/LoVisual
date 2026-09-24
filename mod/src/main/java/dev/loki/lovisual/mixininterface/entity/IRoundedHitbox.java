package dev.loki.lovisual.mixininterface.entity;

public interface IRoundedHitbox {
    void lovisual$setRoundedHitbox(float radius);

    void lovisual$clearRoundedHitbox();

    float lovisual$getRoundedHitbox();

    boolean lovisual$useRoundedHitbox();
}
