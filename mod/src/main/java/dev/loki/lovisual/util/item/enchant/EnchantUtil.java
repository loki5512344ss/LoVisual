package dev.loki.lovisual.util.item.enchant;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Shared helpers for enchantment validation/formatting.
 */
public enum EnchantUtil {
    ;

    /**
     * Checks if the provided enchantment level exceeds its max level.
     */
    public static boolean isInvalidLevel(Holder<Enchantment> enchant, int level) {
        if (enchant == null || level <= 0) return false;
        Enchantment value = enchant.value();
        int max = value.getMaxLevel();
        return level > max;
    }
}
