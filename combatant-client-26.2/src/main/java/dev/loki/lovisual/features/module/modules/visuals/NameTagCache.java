/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.util.player.effect.StatusEffectView;
import dev.loki.lovisual.util.pvp.opponents.TotemPopSnapshot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

final class NameTagCache {

    private static final int EFFECT_CACHE_TICKS = 4;
    private static final int ENCHANT_CACHE_TICKS = 2;
    private static final float TOTEM_BADGE_ANIM_SPEED = 10.0f;

    private NameTagCache() {
    }

    static ItemStack[] getCachedEquipmentSlots(NameTags self, Player player) {
        UUID id = player.getUUID();
        ItemStack[] slots = self.slotCache.computeIfAbsent(id, key -> new ItemStack[6]);
        slots[0] = player.getMainHandItem();
        slots[1] = player.getItemBySlot(EquipmentSlot.FEET);
        slots[2] = player.getItemBySlot(EquipmentSlot.LEGS);
        slots[3] = player.getItemBySlot(EquipmentSlot.CHEST);
        slots[4] = player.getItemBySlot(EquipmentSlot.HEAD);
        slots[5] = player.getOffhandItem();
        return slots;
    }

    static List<MobEffectInstance> resolveEffects(NameTags self, Player player) {
        UUID id = player.getUUID();
        NameTagModel.EffectCache cache = self.effectCache.computeIfAbsent(id, key -> new NameTagModel.EffectCache());
        int tick = player.tickCount;
        if (tick - cache.lastTick >= EFFECT_CACHE_TICKS || cache.effects.isEmpty()) {
            cache.effects = StatusEffectView.collectHudEffects(player);
            cache.lastTick = tick;
        }
        return cache.effects;
    }

    static NameTagModel.EnchantCache resolveEnchantCache(NameTags self, Player player, ItemStack[] slots) {
        UUID id = player.getUUID();
        NameTagModel.EnchantCache cache = self.enchantCache.computeIfAbsent(id, key -> new NameTagModel.EnchantCache());
        int tick = player.tickCount;
        int signature = equipmentSignature(slots);
        if (signature != cache.signature
                || tick - cache.lastTick >= ENCHANT_CACHE_TICKS
                || cache.lines.isEmpty()) {
            List<List<NameTagModel.EnchantLine>> lines = NameTagEnchantFormatter.buildEnchantLines(self, slots);
            cache.lines = lines;
            cache.maxLines = NameTagEnchantFormatter.maxEnchantLines(lines);
            cache.lastTick = tick;
            cache.signature = signature;
        }
        return cache;
    }

    static float updateTotemPopAnimation(NameTags self, Player player, TotemPopSnapshot snapshot, float dt) {
        if (player == null) return 0.0f;
        UUID id = player.getUUID();
        boolean visible = snapshot != null && snapshot.visible();
        if (visible) {
            self.totemPopLastCounts.put(id, snapshot.count());
        }

        float current = self.totemPopAnimations.getOrDefault(id, 0.0f);
        float target = visible ? 1.0f : 0.0f;
        float next = AnimationUtility.approach(current, target, dt, TOTEM_BADGE_ANIM_SPEED);
        next = AnimationUtility.snap(next, target, 0.001f);

        if (next <= 0.001f && !visible) {
            self.totemPopAnimations.remove(id);
            self.totemPopLastCounts.remove(id);
            return 0.0f;
        }

        self.totemPopAnimations.put(id, next);
        return next;
    }

    static int resolveTotemPopCount(NameTags self, Player player, TotemPopSnapshot snapshot, float animation) {
        if (player == null) return 0;
        if (snapshot != null && snapshot.visible()) {
            return snapshot.count();
        }
        if (animation > 0.001f) {
            return Math.max(0, self.totemPopLastCounts.getOrDefault(player.getUUID(), 0));
        }
        return 0;
    }

    static void pruneCaches(NameTags self) {
        if (self.seenIds.isEmpty()) {
            self.slotCache.clear();
            self.effectCache.clear();
            self.enchantCache.clear();
            self.totemPopAnimations.clear();
            self.totemPopLastCounts.clear();
            return;
        }
        self.slotCache.keySet().removeIf(id -> !self.seenIds.contains(id));
        self.effectCache.keySet().removeIf(id -> !self.seenIds.contains(id));
        self.enchantCache.keySet().removeIf(id -> !self.seenIds.contains(id));
        self.totemPopAnimations.keySet().removeIf(id -> !self.seenIds.contains(id));
        self.totemPopLastCounts.keySet().removeIf(id -> !self.seenIds.contains(id));
    }

    static void clearCaches(NameTags self) {
        self.enchantCache.clear();
    }

    private static int equipmentSignature(ItemStack[] slots) {
        if (slots == null || slots.length == 0) return 1;
        int hash = 1;
        for (ItemStack stack : slots) {
            hash = 31 * hash + itemSignature(stack);
        }
        return hash;
    }

    private static int itemSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int hash = 17;
        hash = 31 * hash + Item.getId(stack.getItem());
        hash = 31 * hash + stack.getCount();
        hash = 31 * hash + stack.getDamageValue();
        hash = 31 * hash + stack.getComponents().hashCode();
        return hash;
    }
}
