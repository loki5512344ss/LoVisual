package dev.loki.lovisual.events.impl.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import dev.loki.lovisual.events.Event;

@Getter
public final class CrosshairTargetUpdateEvent extends Event {
    private final float tickDelta;
    @Setter
    private HitResult hitResult;
    @Setter
    private Entity targetedEntity;

    public CrosshairTargetUpdateEvent(float tickDelta, HitResult hitResult, Entity targetedEntity) {
        this.tickDelta = tickDelta;
        this.hitResult = hitResult;
        this.targetedEntity = targetedEntity;
    }

}
