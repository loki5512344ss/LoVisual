package dev.loki.lovisual.mixins.entity.player;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.player.inventory.LockSlot;
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
