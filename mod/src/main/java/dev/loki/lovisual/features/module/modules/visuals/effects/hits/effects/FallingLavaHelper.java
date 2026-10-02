package dev.loki.lovisual.features.module.modules.visuals.effects.hits.effects;

import dev.loki.lovisual.mixins.accessors.world.ClientLevelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

/** Лавовые капли по объёму тела (режим FallingLava у KillEffect). */
public final class FallingLavaHelper {

    private FallingLavaHelper() {
    }

    public static void spawn(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int height = (int) (entity.getBbHeight() * 10);
        int width = (int) (entity.getBbWidth() * 10);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < width; k++) {
                    ((ClientLevelAccessor) mc.level).invokeAddParticle(ParticleTypes.FALLING_LAVA, false, true,
                            entity.getX() + j * 0.1, entity.getY() + i * 0.1, entity.getZ() + k * 0.1, 0, 0, 0);
                }
            }
        }
    }
}
