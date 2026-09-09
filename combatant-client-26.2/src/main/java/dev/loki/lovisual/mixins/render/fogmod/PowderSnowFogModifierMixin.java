/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.fogmod;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;

@Mixin(value = PowderedSnowFogEnvironment.class, priority = 1200)
public class PowderSnowFogModifierMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void lovisual$noPowderFog(FogData data,
                                       Camera camera,
                                       ClientLevel world,
                                       float viewDistance,
                                       DeltaTracker tickCounter,
                                       CallbackInfo ci) {
        WorldTweaks module = Modules.get(WorldTweaks.class);
        if (module == null || !module.isFogControlEnabled()) return;
        if (module.isFogModifyEnabled()) {
            module.applyFogOverride(data);
            ci.cancel();
            return;
        }
        if (!module.disablePowderSnowFog()) return;

        float far = Math.max(viewDistance, 16.0f);
        data.environmentalStart = far;
        data.environmentalEnd = far;
        data.renderDistanceStart = far;
        data.renderDistanceEnd = far;
        data.skyEnd = far;
        data.cloudEnd = far;
        ci.cancel();
    }
}

