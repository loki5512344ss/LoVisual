/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.projectile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SimulatedArrow {

    private final ClientLevel world;
    private final boolean collideEntities;
    private Vec3 pos;
    private Vec3 velocity;
    private boolean inGround;

    public SimulatedArrow(ClientLevel world, Vec3 pos, Vec3 velocity, boolean collideEntities) {
        this.world = world;
        this.pos = pos;
        this.velocity = velocity;
        this.collideEntities = collideEntities;
    }

    public Vec3 getPos() {
        return pos;
    }

    public HitResult tick() {
        if (inGround) {
            return null;
        }

        Vec3 previousPos = pos;
        Vec3 newPos = pos.add(velocity);
        double drag = isTouchingWater() ? 0.6 : 0.99;

        velocity = velocity.scale(drag);
        velocity = velocity.add(0.0, -0.05000000074505806, 0.0);

        HitResult collision = updateCollision(previousPos, newPos);
        if (collision != null) {
            pos = collision.getLocation();
            inGround = true;
            return collision;
        }

        pos = newPos;
        return null;
    }

    private HitResult updateCollision(Vec3 start, Vec3 end) {
        AbstractArrow arrowEntity = new AbstractArrow(
                net.minecraft.world.entity.EntityTypes.ARROW,
                start.x,
                start.y,
                start.z,
                this.world,
                ItemStack.EMPTY,
                ItemStack.EMPTY
        ) {
            @Override
            protected ItemStack getDefaultPickupItem() {
                return new ItemStack(Items.ARROW);
            }
        };

        HitResult blockHit = world.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                arrowEntity
        ));

        if (collideEntities) {
            double size = 0.45;
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    arrowEntity,
                    start,
                    end,
                    new AABB(-size, -size, -size, size, size, size)
                            .move(start)
                            .expandTowards(end.subtract(start))
                            .inflate(1.0),
                    entity -> !entity.isSpectator() && entity.isAlive() && entity.isAttackable(),
                    end.distanceToSqr(start)
            );
            if (entityHit != null && entityHit.getType() != HitResult.Type.MISS) {
                return entityHit;
            }
        }

        return blockHit != null && blockHit.getType() != HitResult.Type.MISS ? blockHit : null;
    }

    private boolean isTouchingWater() {
        return false;
    }
}
