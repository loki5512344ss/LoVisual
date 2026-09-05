/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.player.LockSlot;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels the Q / Ctrl+Q drop of the selected hotbar slot
 * while LockSlot protects it.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerDropMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void lovisual$lockSlot$blockSelectedDrop(boolean wholeStack, CallbackInfoReturnable<Boolean> cir) {
        LockSlot lockSlot = Modules.get(LockSlot.class);
        if (lockSlot != null && lockSlot.shouldBlockSelectedDrop()) {
            cir.setReturnValue(false);
        }
    }
}
