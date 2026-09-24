package dev.loki.lovisual.mixins.world.level;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.features.module.lifecycle.Modules;
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
