package dev.loki.lovisual.util.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import dev.loki.lovisual.util.player.effect.tracker.StatusEffectAccess;

/**
 * Applies Minecraft's post-impact damage reductions (difficulty, armor, fire-resistance
 * potion, blast / general protection enchantments) to the raw explosion impact.
 * Package-private: called only from {@link ExplosionDamageUtil}.
 */
final class ExplosionReductions {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private ExplosionReductions() {}
    /** Scales raw damage by world difficulty: half-rounded on Easy, +50% on Hard. */
    static float applyDifficulty(float damage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return damage;
        return switch (mc.level.getDifficulty()) {
            case EASY -> Math.min(damage / 2.0f + 1.0f, damage);
            case HARD -> damage * 1.5f;
            default -> damage;
        };
    }
    /** Vanilla armor + toughness formula, evaluated against a synthetic explosion damage source. */
    static float applyArmor(LivingEntity entity, float damage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return damage;
        DamageSource source = Explosion.getDefaultDamageSource(mc.level, mc.player);
        float armor = entity.getArmorValue();
        float toughness = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        return CombatRules.getDamageAfterAbsorb(entity, damage, source, armor, toughness);
    }
    /** Fire-resistance effect (level 1 = 20% reduction, +5% per amplifier). */
    static float applyResistance(LivingEntity entity, float damage) {
        MobEffectInstance resistance = StatusEffectAccess.get(entity, MobEffects.RESISTANCE);
        if (resistance == null) return damage;
        int reduced = 25 - (resistance.getAmplifier() + 1) * 5;
        return Math.max(damage * reduced / 25.0f, 0.0f);
    }
    /** Blast Protection + Protection enchantment reduction, applied last. Never returns a negative value. */
    static float applyProtection(LivingEntity entity, float damage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || damage <= 0.0f) return Math.max(damage, 0.0f);
        float protection = totalProtection(entity);
        return protection > 0.0f ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
    }
    private static float totalProtection(LivingEntity entity) {
        float total = 0.0f;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += protectionOf(entity.getItemBySlot(slot));
        }
        return total;
    }
    private static float protectionOf(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || stack == null || stack.isEmpty()) return 0.0f;
        return enchantmentLevel(Enchantments.BLAST_PROTECTION, stack) * 2.0f
                + enchantmentLevel(Enchantments.PROTECTION, stack);
    }
    private static int enchantmentLevel(ResourceKey<Enchantment> key, ItemStack stack) {
        Holder<Enchantment> entry = enchantmentEntry(key);
        return entry != null ? EnchantmentHelper.getItemEnchantmentLevel(entry, stack) : 0;
    }
    private static Holder<Enchantment> enchantmentEntry(ResourceKey<Enchantment> key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || key == null) return null;
        Registry<Enchantment> registry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Enchantment enchantment = registry.getValue(key);
        return enchantment != null ? registry.wrapAsHolder(enchantment) : null;
    }
}
