package dev.loki.lovisual.features.module.modules.combat.pvp;

import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.relations.PlayerRelations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.pvp.parser.PvpChatParser;
import dev.loki.lovisual.util.pvp.parser.PvpOverlayParser;
import dev.loki.lovisual.util.pvp.state.PvpState;
import dev.loki.lovisual.util.pvp.client.CooldownsState;

import java.util.*;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.CooldownRulesSetting;

/**
 * Handles PvP cooldown rendering and optional local rule synthesis.
 */
@ModuleInfo(
        id = "pvpcooldowns",
        displayName = "PvpCooldowns",
        category = ModuleCategory.MISC
)
public class PvpCooldowns extends Module {

    private final BooleanValue renderSlots = bool("pvpcd_render_slots", "render_slots", true);
    private final BooleanValue renderWidget = bool("pvpcd_widget", "show_in_hud", true);
    private final NumberValue<Integer> targetGlowTtlMs =
            num("pvpcd_target_glow_ttl_ms", "target_glow_ttl_ms", 1500, 0, 10000);
    private final BooleanValue hideTargetGlow = bool("pvpcd_hide_target_glow", "hide_target_glow", false);
    private final BooleanValue dynamicEnemyEnabled = bool("pvpcd_dynamic_enemy", "dynamic_enemy", false);
    private final NumberValue<Integer> dynamicEnemyTime = visibleWhen(
            num("pvpcd_dynamic_enemy_time", "dynamic_enemy_time", 60, 0, 600),
            dynamicEnemyEnabled::get
    );
    private final BooleanValue blockDisconnect = bool("pvpcd_block_disconnect", "block_disconnect", true);

    private final BooleanMapValue stateSources = group(
            "pvpcd_state_sources",
            "state_sources",
            PvpCooldownRules.defaultStateSources()
    );

    private final SetValue chatGivePatterns = visibleWhen(textList(
            "pvpcd_chat_give_patterns",
            "chat_give_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpChatParser.defaultGivePatterns()
    ), () -> sourceEnabled("chat") || sourceEnabled("game_message"));

    private final SetValue chatReceivePatterns = visibleWhen(textList(
            "pvpcd_chat_receive_patterns",
            "chat_receive_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpChatParser.defaultReceivePatterns()
    ), () -> sourceEnabled("chat") || sourceEnabled("game_message"));

    private final SetValue chatActivePatterns = visibleWhen(textList(
            "pvpcd_chat_active_patterns",
            "chat_active_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpChatParser.defaultActivePatterns()
    ), () -> sourceEnabled("chat") || sourceEnabled("game_message"));

    private final SetValue chatExitPatterns = visibleWhen(textList(
            "pvpcd_chat_exit_patterns",
            "chat_exit_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpChatParser.defaultExitPatterns()
    ), () -> sourceEnabled("chat") || sourceEnabled("game_message"));

    private final SetValue overlayActivePatterns = visibleWhen(textList(
            "pvpcd_overlay_active_patterns",
            "overlay_active_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpOverlayParser.defaultActivePatterns()
    ), () -> sourceEnabled("overlay"));

    private final SetValue overlayExitPatterns = visibleWhen(textList(
            "pvpcd_overlay_exit_patterns",
            "overlay_exit_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpOverlayParser.defaultExitPatterns()
    ), () -> sourceEnabled("overlay"));

    private final SetValue tabActivePatterns = visibleWhen(textList(
            "pvpcd_tab_active_patterns",
            "tab_active_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpOverlayParser.defaultActivePatterns()
    ), () -> sourceEnabled("tab"));

    private final SetValue tabExitPatterns = visibleWhen(textList(
            "pvpcd_tab_exit_patterns",
            "tab_exit_patterns",
            TextListSetting.PickerMode.TEXT,
            PvpOverlayParser.defaultExitPatterns()
    ), () -> sourceEnabled("tab"));

    /**
     * Off by default. Server-provided vanilla item cooldown packets must remain the primary flow.
     * Enable only for servers that lock items without sending cooldown data.
     */
    private final BooleanValue localRulesEnabled = bool("pvpcd_local_rules", "local_rules", false);

    /**
     * Object-shaped per-item local cooldown rules. Rendered by CooldownRulesSetting.
     */
    private final ItemCooldownRulesValue itemRules = new ItemCooldownRulesValue(
            "pvpcd_item_rules",
            PvpCooldownRules.defaultItemRules()
    );

    private final PvpCooldownRules localRules =
            new PvpCooldownRules(this::isEnabled, localRulesEnabled, itemRules);

    {
        setting(SettingDef.cooldownRules("cooldown_rules", itemRules).visibleWhen(localRulesEnabled::get));
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            CooldownsState.MANAGER.clear();
        }
        syncDynamicEnemySettings();
    }

    @Override
    public void onDisable() {
        CooldownsState.MANAGER.clear();
        CooldownsState.PENDING.clear();
        syncDynamicEnemySettings();
    }

    @Override
    public void onEnable() {
        syncDynamicEnemySettings();
    }

    public boolean isSystemEnabled() {
        return isEnabled();
    }

    public boolean shouldRenderSlots() {
        return renderSlots.get();
    }

    /**
     * Legacy API kept for callers compiled against it. The old hard-coded use blocker is gone.
     */
    public boolean shouldBlockUsage() {
        return false;
    }

    public boolean shouldRenderWidget() {
        return renderWidget.get();
    }

    public int getTargetGlowTtlMs() {
        return targetGlowTtlMs.get();
    }

    public boolean shouldHideTargetGlow() {
        return isEnabled() && hideTargetGlow.get();
    }

    public boolean isDynamicEnemyEnabled() {
        return dynamicEnemyEnabled.get();
    }

    public int getDynamicEnemySeconds() {
        return dynamicEnemyTime.get();
    }

    public boolean shouldBlockDisconnect() {
        return isEnabled() && blockDisconnect.get() && CooldownsState.MANAGER.isInPvp();
    }

    public long getDisconnectBlockRemainingMs() {
        if (!shouldBlockDisconnect()) {
            return 0L;
        }
        return PvpState.getPredictedRemainingMs();
    }

    public Component getDisconnectBlockTooltip() {
        long remainingMs = getDisconnectBlockRemainingMs();
        if (remainingMs <= 0L) {
            return Component.literal("PvP cooldown active");
        }
        double seconds = Math.ceil(remainingMs / 100.0) / 10.0;
        return Component.literal(String.format(Locale.ROOT, "PvP cooldown: %.1fs", seconds));
    }

    public boolean shouldBlockItemUse(Item item) {
        return localRules.shouldBlockItemUse(item);
    }

    public boolean shouldReadChatMessages() {
        return isEnabled() && sourceEnabled("chat");
    }

    public boolean shouldReadGameMessages() {
        return isEnabled() && sourceEnabled("game_message");
    }

    public boolean shouldReadOverlayMessages() {
        return isEnabled() && sourceEnabled("overlay");
    }

    public boolean shouldReadTabText() {
        return isEnabled() && sourceEnabled("tab");
    }

    public Set<String> getChatGivePatterns() {
        return chatGivePatterns.get();
    }

    public Set<String> getChatReceivePatterns() {
        return chatReceivePatterns.get();
    }

    public Set<String> getChatActivePatterns() {
        return chatActivePatterns.get();
    }

    public Set<String> getChatExitPatterns() {
        return chatExitPatterns.get();
    }

    public Set<String> getOverlayActivePatterns() {
        return overlayActivePatterns.get();
    }

    public Set<String> getOverlayExitPatterns() {
        return overlayExitPatterns.get();
    }

    public Set<String> getTabActivePatterns() {
        return tabActivePatterns.get();
    }

    public Set<String> getTabExitPatterns() {
        return tabExitPatterns.get();
    }

    public int getLocalCooldownSeconds(Item item) {
        return localRules.cooldownSeconds(item);
    }

    public int getLocalRuleUses(Item item) {
        return localRules.ruleUses(item);
    }

    public int getLocalRuleWindowSeconds(Item item) {
        return localRules.windowSeconds(item);
    }

    public ItemCooldownRulesValue.Rule getLocalRule(Item item) {
        return localRules.rule(item);
    }

    public Set<Item> getLocalCooldownItems() {
        return localRules.cooldownItems();
    }

    public boolean tryStartLocalCooldown(Item item, ItemCooldownRulesValue.Trigger trigger) {
        return localRules.tryStart(item, trigger);
    }

    private void syncDynamicEnemySettings() {
        PlayerRelations.setDynamicEnemyEnabled(
                isEnabled() && dynamicEnemyEnabled.get()
        );
        PlayerRelations.setDynamicEnemyDurationSeconds(
                dynamicEnemyTime.get()
        );
    }

    private boolean sourceEnabled(String key) {
        return stateSources.get(key);
    }
}
