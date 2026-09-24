package dev.loki.lovisual.mixins.accessors.state;

import net.minecraft.core.particles.SpellParticleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpellParticleOption.class)
public interface EffectParticleEffectAccessor {
    @Accessor("color")
    int lovisual$getColor();
}

