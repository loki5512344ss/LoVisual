/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.world.level;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void lovisual$overrideRenderClock(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        if (clock == null || !clock.is(WorldClocks.OVERWORLD)) return;

        WorldTweaks worldTweaks = Modules.get(WorldTweaks.class);
        if (worldTweaks == null || !worldTweaks.shouldOverrideTime()) return;

        cir.setReturnValue(worldTweaks.getRenderClockTicks());
    }
}
