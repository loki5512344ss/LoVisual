package dev.loki.lovisual.mixins.accessors.world;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface WorldAccessor {

    @Accessor("isClientSide")
    boolean lovisual$isClient();

    @Accessor("rainLevel")
    float lovisual$getRainGradientRaw();

    @Accessor("oRainLevel")
    float lovisual$getLastRainGradientRaw();

    @Accessor("thunderLevel")
    float lovisual$getThunderGradientRaw();

    @Accessor("oThunderLevel")
    float lovisual$getLastThunderGradientRaw();
}

