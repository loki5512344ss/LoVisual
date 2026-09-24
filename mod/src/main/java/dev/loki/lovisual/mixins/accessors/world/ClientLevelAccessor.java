package dev.loki.lovisual.mixins.accessors.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
    @Invoker("doAddParticle")
    void invokeAddParticle(ParticleOptions effect,
                           boolean force,
                           boolean canSpawnOnMinimal,
                           double x, double y, double z,
                           double vx, double vy, double vz);

    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler lovisual$getPendingUpdateManager();
}

