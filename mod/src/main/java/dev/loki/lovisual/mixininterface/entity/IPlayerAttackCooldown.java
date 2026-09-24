package dev.loki.lovisual.mixininterface.entity;

public interface IPlayerAttackCooldown {
    float lovisual$getAttackCooldownProgress(float tickDelta);

    boolean lovisual$isAttackCharged(float tickDelta);
}
