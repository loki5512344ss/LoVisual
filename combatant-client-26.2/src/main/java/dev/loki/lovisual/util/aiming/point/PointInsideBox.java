/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.point;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record PointInsideBox(Vec3 pos, AABB box) {

    public PointInsideBox {
        pos = clampToBox(pos, box);
    }

    private static Vec3 clampToBox(Vec3 pos, AABB box) {
        return new Vec3(
                Mth.clamp(pos.x, box.minX, box.maxX),
                Mth.clamp(pos.y, box.minY, box.maxY),
                Mth.clamp(pos.z, box.minZ, box.maxZ)
        );
    }
}
