/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.world.level;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks.WeatherMode;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void picker$rainGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
        WorldTweaks wt = Modules.get(WorldTweaks.class);
        if (wt == null || !wt.isWeatherControlEnabled()) return;

        WeatherMode mode = wt.getWeatherMode();
        cir.setReturnValue(
                (mode == WeatherMode.RAIN || mode == WeatherMode.THUNDER) ? 1.0F : 0.0F
        );
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void picker$thunderGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
        WorldTweaks wt = Modules.get(WorldTweaks.class);
        if (wt == null || !wt.isWeatherControlEnabled()) return;

        cir.setReturnValue(
                wt.getWeatherMode() == WeatherMode.THUNDER ? 1.0F : 0.0F
        );
    }

}
