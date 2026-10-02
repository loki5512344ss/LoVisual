package dev.loki.lovisual.mixins.render.weather;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.Blizzard;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Штормовая пелена Blizzard: после ванильного тумана сжимает дальность и красит туман в цвет снега. */
@Mixin(value = FogRenderer.class, priority = 1500)
public class BlizzardFogMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void lovisual$blizzardWhiteout(Camera camera, int viewDistance, DeltaTracker tracker, float partial,
                                           ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        Blizzard blizzard = Modules.get(Blizzard.class);
        if (blizzard != null) blizzard.applyFog(camera, cir.getReturnValue());
    }
}
