/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.movement.InventoryMove;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void combatant$inventoryMove$deferClose(CallbackInfo ci) {
        InventoryMove module = Modules.get(InventoryMove.class);
        if (module == null || !module.isEnabled()) return;
        if (module.shouldCancelHandledScreenClose((AbstractContainerScreen<?>) (Object) this)) {
            ci.cancel();
        }
    }
}
