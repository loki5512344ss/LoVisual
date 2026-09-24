package dev.loki.lovisual.mixininterface.entity;

import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.helpers.player.TrailPoint;

import java.util.List;

public interface IEntity {
    Vec3 get$InstantRenderPos();

    List<TrailPoint> lovisual$getTrails();
}
