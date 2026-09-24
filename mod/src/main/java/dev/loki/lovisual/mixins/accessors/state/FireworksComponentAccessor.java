package dev.loki.lovisual.mixins.accessors.state;

import net.minecraft.world.item.component.Fireworks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Fireworks.class)
public interface FireworksComponentAccessor {
    @Accessor("flightDuration")
    int lovisual$getFlightDuration();
}

