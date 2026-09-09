/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.fog;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.Level;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.weather.AtmoDawnFog;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;

@Mixin(value = FogRenderer.class, priority = 1200)
public class FogRendererMixin {

    @ModifyVariable(
            method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2)
    private int lovisual$disableDistanceFog(int value) {
        WorldTweaks module = Modules.get(WorldTweaks.class);
        if (module == null || !module.isFogControlEnabled()) return value;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc != null ? mc.level : null;
        if (world == null) return value;

        int out = value;

        if (module.disableDistanceFog()) {
            int blocks = Math.max(16, module.getDistanceBlocks());
            int chunks = (blocks + 15) / 16;
            out = Math.max(out, chunks);
        }

        if (module.disableWeatherFog()
                && world.dimension() == Level.OVERWORLD
                && (world.getRainLevel(1.0f) > 0.0f
                || world.getThunderLevel(1.0f) > 0.0f)) {
            int blocks = Math.max(16, module.getWeatherDistanceBlocks());
            int chunks = (blocks + 15) / 16;
            out = Math.max(out, chunks);
        }

        return out;
    }

    @Inject(
            method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
            at = @At("RETURN"),
            cancellable = true
    )
    private void lovisual$modifyFogColor(Camera camera,
                                          float tickDelta,
                                          ClientLevel world,
                                          int viewDistance,
                                          float skyDarkness,
                                          Vector4f color, CallbackInfo ci) {
        if (AtmoDawnFog.tintColor(color, world, camera.position().y)) {
            return;
        }

        WorldTweaks module = Modules.get(WorldTweaks.class);
        if (module == null || !module.isFogModifyEnabled()) return;

        int argb = module.getFogColorArgb();
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = 1.0f;
        if (color != null) {
            a = color.w;
            color.set(r, g, b, a);
        }
    }
}
