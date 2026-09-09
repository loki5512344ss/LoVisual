/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.sound;

import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.Library;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.util.wav.CustomSoundEngine;

/**
 * Hooks into Minecraft SoundEngine lifecycle to reset our WAV caches
 * when audio is (re)initialized or closed.
 */
@Mixin(Library.class)
public class LibraryMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void lovisual$resetOnInit(@Nullable String deviceSpecifier, DeviceList deviceList, boolean directionalAudio, CallbackInfo ci) {
        CustomSoundEngine.get().reset();
    }

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void lovisual$resetOnClose(CallbackInfo ci) {
        CustomSoundEngine.get().reset();
    }
}

