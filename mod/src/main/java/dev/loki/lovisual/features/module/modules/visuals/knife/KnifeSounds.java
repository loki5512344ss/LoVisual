package dev.loki.lovisual.features.module.modules.visuals.knife;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeSoundSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Plays the sound events of knife clips (registered in {@code assets/lovisual/sounds.json}) at the player. */
final class KnifeSounds implements KnifeSoundSink {
    static final KnifeSounds INSTANCE = new KnifeSounds();

    private KnifeSounds() {
    }

    @Override
    public Object play(String effect) {
        Knife knife = Modules.get(Knife.class);
        LocalPlayer player = Minecraft.getInstance().player;
        if (knife == null || player == null || !knife.soundsEnabled()) return null;
        SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("lovisual", effect));
        SimpleSoundInstance sound = new SimpleSoundInstance(event, SoundSource.PLAYERS, knife.soundVolume(), 1.0f,
                RandomSource.create(), player.getX(), player.getY(), player.getZ());
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    }

    @Override
    public boolean active(Object handle) {
        return handle instanceof SoundInstance sound && Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    @Override
    public void stop(Object handle) {
        if (handle instanceof SoundInstance sound) Minecraft.getInstance().getSoundManager().stop(sound);
    }
}
