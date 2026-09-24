package dev.loki.lovisual.mixins.sodium.vertex;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public abstract class SodiumWorldRendererFreecamMixin {

    @ModifyVariable(
            method = "setupTerrain(Lnet/minecraft/client/Camera;Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;Lnet/caffeinemc/mods/sodium/client/util/FogParameters;ZZLorg/joml/Matrix4f;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private boolean lovisual$freecamUsesSpectatorOcclusionRules(boolean spectator) {
        Freecam freecam = Modules.get(Freecam.class);
        return spectator || (freecam != null && freecam.isEnabled() && freecam.camEntity != null);
    }
}
