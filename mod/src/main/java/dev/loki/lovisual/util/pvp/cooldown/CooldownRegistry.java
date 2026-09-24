package dev.loki.lovisual.util.pvp.cooldown;

import net.minecraft.world.item.Item;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;

import java.util.Collections;
import java.util.Set;
import dev.loki.lovisual.util.pvp.cooldown.ItemCooldownRuleProvider;

public enum CooldownRegistry {
    ;

    private static volatile ItemCooldownRuleProvider provider;

    public static void setProvider(ItemCooldownRuleProvider newProvider) {
        provider = newProvider;
    }

    public static void clearProvider(ItemCooldownRuleProvider oldProvider) {
        if (provider == oldProvider) {
            provider = null;
        }
    }

    public static ItemCooldownRulesValue.Rule getRule(Item item) {
        ItemCooldownRuleProvider current = provider;
        if (current == null || !current.isItemCooldownRulesEnabled()) return null;
        return current.getCooldownRule(item);
    }

    public static int getCooldown(Item item) {
        ItemCooldownRulesValue.Rule rule = getRule(item);
        return rule != null ? rule.seconds() : 0;
    }

    public static int getMaxUses(Item item) {
        ItemCooldownRulesValue.Rule rule = getRule(item);
        return rule != null ? rule.uses() : 1;
    }

    public static int getWindowSeconds(Item item) {
        ItemCooldownRulesValue.Rule rule = getRule(item);
        return rule != null ? rule.windowSeconds() : 0;
    }

    public static boolean isTracked(Item item) {
        ItemCooldownRulesValue.Rule rule = getRule(item);
        return rule != null && rule.enabled() && (rule.seconds() > 0 || rule.uses() > 1);
    }

    public static Set<Item> trackedItems() {
        ItemCooldownRuleProvider current = provider;
        if (current == null || !current.isItemCooldownRulesEnabled()) return Collections.emptySet();
        Set<Item> items = current.getTrackedCooldownItems();
        return items != null ? Collections.unmodifiableSet(items) : Collections.emptySet();
    }
}
