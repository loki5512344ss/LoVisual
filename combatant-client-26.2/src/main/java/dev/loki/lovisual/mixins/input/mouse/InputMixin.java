/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.input.mouse;

import net.minecraft.client.player.ClientInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientInput.class)
public abstract class InputMixin {

    @Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
    private void allowAllDirections(CallbackInfoReturnable<Boolean> cir) {
        // Sprint module removed; keep vanilla forward impulse check.
    }
}
