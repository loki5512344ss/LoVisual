/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.projectile.targeting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.entity.simulation.MountedEntityPrediction;
import dev.loki.lovisual.util.player.simulation.PlayerSimulationCache;

public interface ProjectileTarget {

    static ProjectileTarget forEntity(Entity entity) {
        return new ProjectileTarget() {
            @Override
            public Vec3 getPositionInTicks(double ticks) {
                if (entity.isPassenger()) {
                    return MountedEntityPrediction.predictMountedPosition(entity, (int) Math.round(ticks));
                }

                if (entity instanceof Player player) {
                    return PlayerSimulationCache.getSimulationForOtherPlayers(player)
                            .getSnapshotAt((int) Math.round(ticks))
                            .pos();
                }

                return entity.position().add(entity.getDeltaMovement().scale(ticks));
            }

            @Override
            public AABB getBoxInTicks(double ticks) {
                Vec3 predicted = getPositionInTicks(ticks);
                return entity.getBoundingBox().move(predicted.subtract(entity.position()));
            }
        };
    }

    static ProjectileTarget constant(Vec3 position, AABB box) {
        return new ProjectileTarget() {
            @Override
            public Vec3 getPositionInTicks(double ticks) {
                return position;
            }

            @Override
            public AABB getBoxInTicks(double ticks) {
                return box;
            }
        };
    }

    Vec3 getPositionInTicks(double ticks);

    AABB getBoxInTicks(double ticks);
}
