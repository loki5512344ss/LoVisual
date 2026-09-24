package dev.loki.lovisual.util.projectile.targeting;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.projectile.targeting.PointFinding;

public enum ProjectileTargetPointFinder {
    ;

    public static Vec3 findHittablePosition(
            Vec3 playerHeadPosition,
            Vec3 directionOnImpact,
            Vec3 entityPositionOnImpact,
            AABB targetEntityBox
    ) {
        Vec3 virtualEyes = playerHeadPosition.add(
                0.0,
                directionOnImpact.y * -(playerHeadPosition.distanceTo(entityPositionOnImpact)),
                0.0
        );
        return PointFinding.findVisiblePointFromVirtualEye(virtualEyes, targetEntityBox, 5.0);
    }
}
