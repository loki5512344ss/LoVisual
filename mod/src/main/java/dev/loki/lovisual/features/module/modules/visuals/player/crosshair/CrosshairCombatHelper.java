package dev.loki.lovisual.features.module.modules.visuals.player;

import dev.loki.lovisual.features.gui.hud.draggable.impl.combat.Itemizer;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.mixininterface.entity.IPlayerAttackCooldown;
import dev.loki.lovisual.util.item.food.FoodUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

final class CrosshairCombatHelper {

    private final Crosshair host;
    private boolean chestInit;
    private boolean lastSwapCandidate;
    private boolean lastChestWasElytra;
    private ItemStack lastChestStack = ItemStack.EMPTY;

    CrosshairCombatHelper(Crosshair host) {
        this.host = host;
    }

    static boolean isRiptideBlocked(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        if (!stack.is(Items.TRIDENT)) return false;
        if (!hasRiptide(stack)) return false;
        return !isTouchingWaterOrServerRain(player);
    }

    private static boolean hasRiptide(ItemStack stack) {
        ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return ench.keySet().stream().anyMatch(entry -> {
            var keyOpt = entry.unwrapKey();
            return keyOpt.isPresent() && keyOpt.get().identifier().equals(Enchantments.RIPTIDE.identifier());
        });
    }

    private static boolean isTouchingWaterOrServerRain(Player player) {
        if (player == null) return false;
        if (player.isInWater()) return true;
        Level world = player.level();
        BlockPos pos = player.blockPosition();
        if (WorldTweaks.hasServerRainAt(world, pos)) return true;
        BlockPos top = BlockPos.containing(pos.getX(), player.getBoundingBox().maxY, pos.getZ());
        return WorldTweaks.hasServerRainAt(world, top);
    }

    float getAttackCooldownProgress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof IPlayerAttackCooldown cd) {
            return cd.lovisual$getAttackCooldownProgress(0.0f);
        }
        return mc.player.getAttackStrengthScale(0.0f);
    }

    float getChargeProgress(Player player) {
        if (player == null || !player.isUsingItem()) return -1.0f;
        ItemStack stack = player.getUseItem();
        if (stack == null || stack.isEmpty()) return -1.0f;
        int useTicks = player.getTicksUsingItem();
        Item item = stack.getItem();
        if (item instanceof BowItem) return Mth.clamp(BowItem.getPowerForTime(useTicks), 0.0f, 1.0f);
        if (item instanceof CrossbowItem) {
            int pullTime = CrossbowItem.getChargeDuration(stack, player);
            if (pullTime <= 0) return 1.0f;
            return Mth.clamp(useTicks / (float) pullTime, 0.0f, 1.0f);
        }
        if (stack.is(Items.TRIDENT)) return Mth.clamp(useTicks / 10.0f, 0.0f, 1.0f);
        return -1.0f;
    }

    boolean shouldShowRiptideBlocked(Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (isRiptideBlocked(player, main)) return true;
        if (!isHandLikeItem(main)) return false;
        return isRiptideBlocked(player, player.getOffhandItem());
    }

    boolean shouldShowCooldownCross(Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (isCooldownCrossItem(main) && player.getCooldowns().isOnCooldown(main)) return true;
        if (shouldSuppressOffhandCooldown(main)) return false;
        ItemStack off = player.getOffhandItem();
        return isCooldownCrossItem(off) && player.getCooldowns().isOnCooldown(off);
    }

    private boolean isCooldownCrossItem(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private boolean shouldSuppressOffhandCooldown(ItemStack main) {
        return !isHandLikeItem(main);
    }

    private boolean isHandLikeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        Item item = stack.getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem) return false;
        if (stack.is(Items.TRIDENT)) return false;
        if (FoodUtil.isFood(stack)) return false;
        final boolean[] hasAttackMod = {false};
        stack.forEachModifier(net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND, (a, m, d) -> {
            if (a.equals(Attributes.ATTACK_DAMAGE) || a.equals(Attributes.ATTACK_SPEED)) hasAttackMod[0] = true;
        });
        return !hasAttackMod[0];
    }

    void updateSwapDetector(Player player) {
        ItemStack current = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isElytra = isElytra(current);
        boolean isChestplate = isChestplate(current);
        boolean candidate = isElytra || isChestplate;
        if (!chestInit) {
            chestInit = true;
            lastSwapCandidate = candidate;
            lastChestWasElytra = isElytra;
            lastChestStack = current.copy();
            return;
        }
        if (candidate && lastSwapCandidate && isElytra != lastChestWasElytra
                && !ItemStack.isSameItem(current, lastChestStack)) {
            Itemizer.showElytraSwap(current.copy());
        }
        lastSwapCandidate = candidate;
        lastChestWasElytra = isElytra;
        lastChestStack = current.copy();
    }

    private boolean isElytra(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.ELYTRA);
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.ELYTRA)) return false;
        Equippable eq = stack.get(DataComponents.EQUIPPABLE);
        return eq != null && eq.slot() == EquipmentSlot.CHEST;
    }

    void resetSwapDetector() {
        chestInit = false;
        lastSwapCandidate = false;
        lastChestWasElytra = false;
        lastChestStack = ItemStack.EMPTY;
    }
}
