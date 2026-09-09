/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.network.game;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.player.LockSlot;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels THROW container clicks (Q / Ctrl+Q over a hovered slot and
 * throw clicks) targeting locked hotbar slots while LockSlot is enabled.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeThrowMixin {

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void lovisual$lockSlot$blockThrowClick(int containerId, int slotId, int button,
                                                   ContainerInput input, Player player, CallbackInfo ci) {
        if (input != ContainerInput.THROW) return;
        LockSlot lockSlot = Modules.get(LockSlot.class);
        if (lockSlot != null && lockSlot.shouldBlockContainerThrow(player, containerId, slotId)) {
            ci.cancel();
        }
    }
}
