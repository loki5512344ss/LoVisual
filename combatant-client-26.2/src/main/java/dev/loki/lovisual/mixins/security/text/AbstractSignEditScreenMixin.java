/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.security.text;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.security.BackdoorProtection;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {

    @Shadow
    private SignText text;

    @Shadow
    private String[] messages;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN")
    )
    private void lovisual$backdoor$filterSignMessages(SignBlockEntity blockEntity, boolean front, boolean filtered, Component title, CallbackInfo ci) {
        for (int line = 0; line < this.messages.length; line++) {
            Component safeText = BackdoorProtection.INSTANCE.filterText(this.text.getMessage(line, filtered));
            this.messages[line] = safeText.getString();
        }
    }
}
