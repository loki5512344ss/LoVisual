/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.util.time.GameClock;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.events.EventHandler;
import dev.loki.lovisual.events.impl.GameTickEvent;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.mixins.accessors.MultiPlayerGameModeAccessor;
import dev.loki.lovisual.mixins.accessors.PlayerInventoryAccessor;
import dev.loki.lovisual.util.player.inventory.InventorySwap;

//todo Description
@ModuleInfo(
        id = "autotool",
        displayName = "AutoTool",
        category = ModuleCategory.PLAYER
)
public class AutoTool extends Module {

    private static final String SETTING_RESTORE_DELAY = "restore_delay_ms";
    private final NumberValue<Integer> restoreDelayMs =
            num("autoToolRestoreDelayMs", SETTING_RESTORE_DELAY, 300, 0, 1000);

    private int originalSlot = -1;
    private long lastBreakMs = 0L;

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && originalSlot >= 0) {
            InventorySwap.INSTANCE.selectHotbar(originalSlot);
        }
        originalSlot = -1;
        lastBreakMs = 0L;
    }

    @EventHandler
    private void onGameTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null || mc.options == null || mc.gameMode == null) {
            resetState();
            return;
        }

        boolean breakingNow = mc.options.keyAttack.isDown() && mc.gameMode.isDestroying();
        if (breakingNow && mc.gameMode instanceof MultiPlayerGameModeAccessor accessor) {
            BlockPos pos = accessor.lovisual$getCurrentBreakingPos();
            if (pos != null) {
                int bestSlot = findBestHotbarTool(pos);
                int selected = ((PlayerInventoryAccessor) mc.player.getInventory()).lovisual$getSelectedSlot();
                if (bestSlot >= 0 && bestSlot != selected) {
                    if (originalSlot < 0) {
                        originalSlot = selected;
                    }
                    InventorySwap.INSTANCE.selectHotbar(bestSlot);
                }
                lastBreakMs = GameClock.millis();
                return;
            }
        }

        if (originalSlot >= 0) {
            long now = GameClock.millis();
            if (now - lastBreakMs >= restoreDelayMs.get()) {
                InventorySwap.INSTANCE.selectHotbar(originalSlot);
                originalSlot = -1;
            }
        }
    }

    private int findBestHotbarTool(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return -1;
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return -1;

        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private void resetState() {
        originalSlot = -1;
        lastBreakMs = 0L;
    }
}
