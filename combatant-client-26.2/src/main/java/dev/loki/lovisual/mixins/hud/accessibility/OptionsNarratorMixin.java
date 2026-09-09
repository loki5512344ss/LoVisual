/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.hud.accessibility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.util.NarratorBlocker;

import java.io.File;

@Mixin(Options.class)
public abstract class OptionsNarratorMixin {

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At("RETURN"))
    private void lovisual$enforceNarratorDefaults(Minecraft client, File optionsFile, CallbackInfo ci) {
        NarratorBlocker.enforce((Options) (Object) this);
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void lovisual$enforceNarratorBeforeWrite(CallbackInfo ci) {
        NarratorBlocker.enforce((Options) (Object) this);
    }

    @Inject(method = "narrator", at = @At("HEAD"))
    private void lovisual$enforceNarratorGetter(CallbackInfoReturnable<OptionInstance<NarratorStatus>> cir) {
        NarratorBlocker.enforce((Options) (Object) this);
    }

    @Inject(method = "narratorHotkey", at = @At("HEAD"))
    private void lovisual$enforceNarratorHotkeyGetter(CallbackInfoReturnable<OptionInstance<Boolean>> cir) {
        NarratorBlocker.enforce((Options) (Object) this);
    }
}
