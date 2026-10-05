package dev.loki.lovisual.mixins.render.godrays;

import dev.loki.lovisual.features.module.modules.visuals.godrays.GodRaysSun;
import dev.loki.lovisual.mixins.accessors.world.LevelRendererAccessor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the sun angle and sunrise glow of the frame for the God Rays pass. */
@Mixin(LevelRenderer.class)
public abstract class GodRaysSkyMixin {
    @Inject(method = "addSkyPass", at = @At("HEAD"))
    private void lovisual$captureSun(CallbackInfo ci) {
        SkyRenderState sky = ((LevelRendererAccessor) this).lovisual$getWorldRenderState().skyRenderState;
        if (sky == null) {
            GodRaysSun.invalidate();
        } else {
            GodRaysSun.capture(sky.sunAngle, sky.sunriseAndSunsetColor);
        }
    }
}
