/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player.interact;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.mixins.accessors.inventory.PlayerInventoryAccessor;

//todo Description
@ModuleInfo(
        id = "clickpearl",
        displayName = "ClickPearl",
        category = ModuleCategory.PLAYER
)
public class ClickPearl extends Module {
    private static final String ACTION_CLICKPEARL = "clickpearl";

    public ClickPearl() {
        // Default binds:
        // - Toggle module with LEFT_CTRL+Z
        // - Action key is Z (handled by module's own logic)
        setDefaultBind("LEFT_CTRL+Z");
        action(ACTION_CLICKPEARL, "Z");
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return;

        if (isActionPressedOnce(ACTION_CLICKPEARL)) {
            tryThrowPearl();
        }
    }

    // ===========================
    //      ТВОЯ ЛОГИКА — НЕ ТРОНУТА
    // ===========================
    private void tryThrowPearl() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode manager = client.gameMode;
        if (player == null || manager == null) return;
        if (player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) return;

        PlayerInventoryAccessor inv = (PlayerInventoryAccessor) player.getInventory();
        int originalSlot = inv.lovisual$getSelectedSlot();

        ItemStack off = player.getOffhandItem();
        if (isPearl(off)) {
            manager.useItem(player, InteractionHand.OFF_HAND);
            player.swing(InteractionHand.OFF_HAND);
            return;
        }

        int hotbarPearl = findPearlInHotbar(player);
        if (hotbarPearl != -1) {
            inv.lovisual$setSelectedSlot(hotbarPearl);
            manager.useItem(player, InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND);
            inv.lovisual$setSelectedSlot(originalSlot);
            return;
        }

        // Inventory swap removed (hotbar-only mode) — pearl must be in hotbar/offhand
        return;
    }

    private boolean isPearl(ItemStack stack) {
        return stack != null && stack.is(Items.ENDER_PEARL);
    }

    private int findPearlInHotbar(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (isPearl(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

}
