/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors;

import net.minecraft.world.item.component.Fireworks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Fireworks.class)
public interface FireworksComponentAccessor {
    @Accessor("flightDuration")
    int lovisual$getFlightDuration();
}

