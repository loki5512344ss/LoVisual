/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("portalProcess")
    PortalProcessor lovisual$getPortalManager();

    @Accessor("stuckSpeedMultiplier")
    void lovisual$setMovementMultiplier(Vec3 multiplier);

    @Invoker("unsetRemoved")
    void lovisual$unsetRemoved();
}

