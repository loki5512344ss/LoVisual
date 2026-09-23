/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ArmorData {

    private ArmorData() {}

    static boolean hasAnyStacks(ItemStack[] stacks) {
        if (stacks == null) return false;
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static ItemStack[] getArmorStacks() {
        Minecraft mc = Minecraft.getInstance();
        return new ItemStack[]{
                mc.player.getItemBySlot(EquipmentSlot.HEAD),
                mc.player.getItemBySlot(EquipmentSlot.CHEST),
                mc.player.getItemBySlot(EquipmentSlot.LEGS),
                mc.player.getItemBySlot(EquipmentSlot.FEET)
        };
    }

    static ItemStack[] previewStacks() {
        ItemStack head = new ItemStack(Items.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
        ItemStack feet = new ItemStack(Items.DIAMOND_BOOTS);
        head.setDamageValue(Math.max(1, head.getMaxDamage() / 3));
        chest.setDamageValue(Math.max(1, chest.getMaxDamage() / 2));
        legs.setDamageValue(Math.max(1, legs.getMaxDamage() / 4));
        feet.setDamageValue(Math.max(1, feet.getMaxDamage() / 5));
        return new ItemStack[]{head, chest, legs, feet};
    }

    static boolean shouldShowWhenEmpty(Armor host) {
        Minecraft mc = Minecraft.getInstance();
        HudGlobalConfig hud = host.hud;
        if (hud == null) return false;
        HudGlobalConfig.HeaderMode mode = hud.getHeaderMode();
        if (mode == HudGlobalConfig.HeaderMode.ALWAYS) return true;
        if (mode == HudGlobalConfig.HeaderMode.CHAT) {
            return mc != null && ClientScreen.current() instanceof ChatScreen;
        }
        return false;
    }
}
