package dev.loki.lovisual.util.pvp.cooldown;

import net.minecraft.world.item.Item;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;

import java.util.Set;

/**
 * Supplies configurable item cooldown rules to the generic item-use cooldown engine.
 * <p>
 * The engine itself is owner-based and does not know whether an owner is the local player,
 * an opponent, a fake preview, or another subsystem. PvP modules can provide rules through
 * this interface without making the storage local-player-only.
 */
public interface ItemCooldownRuleProvider {
    boolean isItemCooldownRulesEnabled();

    ItemCooldownRulesValue.Rule getCooldownRule(Item item);

    Set<Item> getTrackedCooldownItems();
}
