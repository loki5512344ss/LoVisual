/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.entity.simulation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum PositionExtrapolation {
    ;

    public static Extrapolation getBestForEntity(Entity entity) {
        return new Extrapolation(entity);
    }

    public static final class Extrapolation {
        private final Entity entity;

        private Extrapolation(Entity entity) {
            this.entity = entity;
        }

        public Vec3 getPositionInTicks(double ticks) {
            return PositionPredictor.predictEntityPos(entity, ticks);
        }

        public Vec3 getEyePositionInTicks(double ticks) {
            if (entity instanceof LivingEntity living) {
                return PositionPredictor.predictEyePos(living, ticks);
            }
            return getPositionInTicks(ticks);
        }

        public AABB getBoxInTicks(double ticks) {
            return PositionPredictor.predictBox(entity, ticks);
        }
    }
}
