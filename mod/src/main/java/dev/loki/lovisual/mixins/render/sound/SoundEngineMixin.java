package dev.loki.lovisual.mixins.render.sound;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.sound.NoSound;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * SoundReducer support: scales the resolved volume of listed sounds in
 * NoSound's reduce mode. Mute mode keeps using the existing packet-level
 * filter in ClientPacketListenerMixin.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Redirect(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"
            )
    )
    private float lovisual$scaleSoundVolume(SoundInstance instance) {
        float original = instance.getVolume();

        Sound resolved = instance.getSound();
        if (resolved == null) return original;

        NoSound module = Modules.get(NoSound.class);
        if (module == null) return original;

        return original * module.volumeScale(resolved.getLocation().toString());
    }
}
