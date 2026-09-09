/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.security.net;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.security.BackdoorProtection;

import java.io.File;

@Mixin(Options.class)
public abstract class GameOptionsMixin {

    @Shadow
    @Final
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At("TAIL"))
    private void lovisual$backdoor$registerVanillaKeybinds(Minecraft client, File optionsFile, CallbackInfo ci) {
        BackdoorProtection.INSTANCE.resetVanillaKeybinds();
        for (KeyMapping keyBinding : keyMappings) {
            BackdoorProtection.INSTANCE.registerVanillaKeybind(keyBinding.getName());
        }
    }
}
