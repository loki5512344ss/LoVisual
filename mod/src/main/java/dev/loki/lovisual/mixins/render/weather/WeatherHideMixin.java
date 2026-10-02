package dev.loki.lovisual.mixins.render.weather;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.blizzard.Blizzard;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blizzard убирает ванильные полосы дождя и снега, пока идёт шторм. */
@Mixin(WeatherEffectRenderer.class)
public class WeatherHideMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V",
            at = @At("RETURN"))
    private void lovisual$dropVanillaColumns(ClientLevel level, float partial, Vec3 cameraPos,
                                             WeatherRenderState state, CallbackInfo ci) {
        Blizzard blizzard = Modules.get(Blizzard.class);
        if (blizzard == null || !blizzard.hidesVanillaWeather()) return;
        state.rainColumns.clear();
        state.snowColumns.clear();
    }
}
