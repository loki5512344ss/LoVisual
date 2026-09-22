/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.fog;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.sky.AtmoDawnFog;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;

@Mixin(value = AtmosphericFogEnvironment.class, priority = 1200)
public class AtmosphericFogEnvironmentMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void lovisual$noDimensionFog(FogData data,
                                          Camera camera,
                                          ClientLevel world,
                                          float viewDistance,
                                          DeltaTracker tickCounter,
                                          CallbackInfo ci) {
        if (AtmoDawnFog.applyFog(data, camera, world, viewDistance,
                tickCounter.getGameTimeDeltaPartialTick(false))) {
            ci.cancel();
            return;
        }

        WorldTweaks module = Modules.get(WorldTweaks.class);
        if (module == null || !module.isFogControlEnabled()) return;
        if (module.isFogModifyEnabled()) {
            module.applyFogOverride(data);
            ci.cancel();
            return;
        }
        if (world == null) return;

        boolean disable =
                (world.dimension() == Level.OVERWORLD && module.disableOverworldFog())
                        || (world.dimension() == Level.NETHER && module.disableNetherFog())
                        || (world.dimension() == Level.END && module.disableEndFog());

        if (!disable) return;

        float far = Math.max(viewDistance, 16.0f);
        data.environmentalStart = far;
        data.environmentalEnd = far;
        data.skyEnd = far;
        data.cloudEnd = far;
        ci.cancel();
    }
}

