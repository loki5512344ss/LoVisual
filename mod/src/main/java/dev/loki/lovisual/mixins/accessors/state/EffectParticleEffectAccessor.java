/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors.state;

import net.minecraft.core.particles.SpellParticleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpellParticleOption.class)
public interface EffectParticleEffectAccessor {
    @Accessor("color")
    int lovisual$getColor();
}

