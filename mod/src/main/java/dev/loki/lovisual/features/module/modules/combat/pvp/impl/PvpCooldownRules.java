package dev.loki.lovisual.features.module.modules.combat.pvp;

import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.util.pvp.client.CooldownsState;
import dev.loki.lovisual.util.pvp.cooldown.ItemCooldownSnapshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Local per-item cooldown-rule engine for {@link PvpCooldowns}: default state-source and item-rule
 * tables plus the enable/scope-gated evaluation of blocking, snapshot lookup and use recording.
 * Reads the module's enablement through a {@link BooleanSupplier} and the live configuration values
 * so behaviour stays identical to the inlined implementation.
 */
final class PvpCooldownRules {
    private static final long LOCAL_RULE_GRACE_MS = 10_000L;

    private final BooleanSupplier moduleEnabled;
    private final BooleanValue localRulesEnabled;
    private final ItemCooldownRulesValue itemRules;

    PvpCooldownRules(BooleanSupplier moduleEnabled, BooleanValue localRulesEnabled, ItemCooldownRulesValue itemRules) {
        this.moduleEnabled = moduleEnabled;
        this.localRulesEnabled = localRulesEnabled;
        this.itemRules = itemRules;
    }

    static Map<String, Boolean> defaultStateSources() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put("chat", true);
        defaults.put("game_message", true);
        defaults.put("overlay", true);
        defaults.put("tab", true);
        return defaults;
    }

    static Map<String, ItemCooldownRulesValue.Rule> defaultItemRules() {
        Map<String, ItemCooldownRulesValue.Rule> rules = new LinkedHashMap<>();
        rules.put("minecraft:golden_apple", new ItemCooldownRulesValue.Rule(
                false,
                30,
                2,
                10,
                ItemCooldownRulesValue.Scope.PVP_GRACE,
                ItemCooldownRulesValue.Trigger.CONSUME_FINISH,
                false
        ));
        rules.put("minecraft:enchanted_golden_apple", new ItemCooldownRulesValue.Rule(
                false,
                60,
                2,
                10,
                ItemCooldownRulesValue.Scope.PVP_GRACE,
                ItemCooldownRulesValue.Trigger.CONSUME_FINISH,
                false
        ));
        rules.put("minecraft:totem_of_undying", new ItemCooldownRulesValue.Rule(
                false,
                0,
                1,
                0,
                ItemCooldownRulesValue.Scope.PVP_GRACE,
                ItemCooldownRulesValue.Trigger.TOTEM_POP,
                false
        ));
        return rules;
    }

    boolean shouldBlockItemUse(Item item) {
        if (!moduleEnabled.getAsBoolean()) return false;
        if (item == null || item == Items.AIR) return false;

        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;

        if (!localRulesEnabled.get()) return false;

        ItemCooldownRulesValue.Rule rule = itemRules.getRule(id.toString());
        if (rule == null || !rule.enabled() || !scopeAllows(rule.scope())) return false;

        ItemCooldownRulesValue.UseBlockMode blockMode = rule.blockMode() != null
                ? rule.blockMode()
                : ItemCooldownRulesValue.UseBlockMode.NONE;
        if (blockMode == ItemCooldownRulesValue.UseBlockMode.NONE) return false;

        ItemCooldownSnapshot snapshot = CooldownsState.MANAGER.snapshot(item);
        return switch (blockMode) {
            case NONE -> false;
            case COOLDOWN -> snapshot.cooling();
            case WINDOW -> snapshot.useWindowActive();
            case ANY -> snapshot.visible();
        };
    }

    int cooldownSeconds(Item item) {
        ItemCooldownRulesValue.Rule rule = configuredRuleFor(item);
        return rule != null ? rule.seconds() : 0;
    }

    int ruleUses(Item item) {
        ItemCooldownRulesValue.Rule rule = configuredRuleFor(item);
        return rule != null ? rule.uses() : 1;
    }

    int windowSeconds(Item item) {
        ItemCooldownRulesValue.Rule rule = configuredRuleFor(item);
        return rule != null ? rule.windowSeconds() : 0;
    }

    ItemCooldownRulesValue.Rule rule(Item item) {
        return configuredRuleFor(item);
    }

    Set<Item> cooldownItems() {
        Set<Item> out = new LinkedHashSet<>();
        if (!moduleEnabled.getAsBoolean() || !localRulesEnabled.get()) return out;

        for (var entry : itemRules.getRules().entrySet()) {
            ItemCooldownRulesValue.Rule rule = entry.getValue();
            if (rule == null || !rule.enabled()) continue;
            if (rule.seconds() <= 0 && rule.uses() <= 1) continue;
            try {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getKey()));
                if (item != null && item != Items.AIR) {
                    out.add(item);
                }
            } catch (RuntimeException ignored) {
                // Invalid custom item id in config: ignore this rule instead of breaking the module.
            }
        }

        return out;
    }

    boolean tryStart(Item item, ItemCooldownRulesValue.Trigger trigger) {
        ItemCooldownRulesValue.Rule rule = configuredRuleFor(item);
        if (rule == null || rule.trigger() != trigger) return false;
        if (!scopeAllows(rule.scope())) return false;
        CooldownsState.MANAGER.recordRuleUse(item, rule);
        return true;
    }

    private ItemCooldownRulesValue.Rule configuredRuleFor(Item item) {
        if (!moduleEnabled.getAsBoolean() || !localRulesEnabled.get()) return null;
        if (item == null || item == Items.AIR) return null;

        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;

        ItemCooldownRulesValue.Rule rule = itemRules.getRule(id.toString());
        if (rule == null || !rule.enabled()) return null;
        if (rule.seconds() <= 0 && rule.uses() <= 1) return null;
        return rule;
    }

    private boolean scopeAllows(ItemCooldownRulesValue.Scope scope) {
        return switch (scope != null ? scope : ItemCooldownRulesValue.Scope.PVP_GRACE) {
            case ALWAYS -> true;
            case PVP_ONLY -> CooldownsState.MANAGER.isInPvp();
            case PVP_GRACE -> CooldownsState.MANAGER.isPvpGraceActive(LOCAL_RULE_GRACE_MS);
        };
    }
}
