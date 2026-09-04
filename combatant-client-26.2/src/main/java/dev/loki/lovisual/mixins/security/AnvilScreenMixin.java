/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.security;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.loki.lovisual.features.security.BackdoorProtection;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {

    @WrapOperation(method = "onNameChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component lovisual$backdoor$filterRenameName(ItemStack stack, Operation<Component> original) {
        return BackdoorProtection.INSTANCE.filterText(original.call(stack));
    }

    @WrapOperation(method = "slotChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component lovisual$backdoor$filterSlotUpdateName(ItemStack stack, Operation<Component> original) {
        return BackdoorProtection.INSTANCE.filterText(original.call(stack));
    }
}
