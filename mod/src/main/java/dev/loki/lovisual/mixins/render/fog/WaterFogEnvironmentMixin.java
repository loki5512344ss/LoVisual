package dev.loki.lovisual.mixins.render.fog;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;

@Mixin(value = WaterFogEnvironment.class, priority = 1200)
public class WaterFogEnvironmentMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void lovisual$noWaterFog(FogData data,
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
        if (!module.disableWaterFog()) return;

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

