/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.projectile;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
