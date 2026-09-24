package dev.loki.lovisual.features.gui.clickgui.settings.cooldown;

import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.bool.BooleanSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.mode.ModeSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.slider.SliderSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundles the seven per-item controls (enabled / seconds / uses / window / scope /
 * trigger / block mode) shown inside the picker detail panel for a focused item.
 * The editor owns its {@code ConfigValue} mirrors so text edits and slider drags
 * commit back into {@link ItemCooldownRulesValue} only after the user's action.
 */
final class CooldownRuleEditor {

    final String itemId;
    final BooleanValue enabledValue = new BooleanValue("cooldown_rule_enabled", false);
    final NumberValue<Integer> secondsValue = new NumberValue<>("cooldown_rule_seconds", 0, 0, 600);
    final NumberValue<Integer> usesValue = new NumberValue<>("cooldown_rule_uses", 1, 1, 10);
    final NumberValue<Integer> windowValue = new NumberValue<>("cooldown_rule_window", 0, 0, 300);
    final ModeValue scopeValue = new ModeValue("cooldown_rule_scope",
            ItemCooldownRulesValue.Scope.PVP_GRACE.getId(), "always", "pvp_only", "pvp_grace");
    final ModeValue triggerValue = new ModeValue("cooldown_rule_trigger",
            ItemCooldownRulesValue.Trigger.CONSUME_FINISH.getId(), "interact_accept", "consume_finish", "totem_pop");
    final ModeValue blockValue = new ModeValue("cooldown_rule_block",
            ItemCooldownRulesValue.UseBlockMode.NONE.getId(), "none", "cooldown", "window", "any");

    final List<Setting> settings = new ArrayList<>();

    private final CooldownRulesSetting owner;
    private final RuleBooleanSetting enabledSetting;
    private final RuleSliderSetting secondsSetting;
    private final RuleSliderSetting usesSetting;
    private final RuleSliderSetting windowSetting;

    CooldownRuleEditor(CooldownRulesSetting owner, String itemId) {
        this.owner = owner;
        this.itemId = itemId;
        enabledSetting = new RuleBooleanSetting("Enabled", enabledValue, this);
        secondsSetting = new RuleSliderSetting("Time (s)", secondsValue, this);
        usesSetting = new RuleSliderSetting("Uses", usesValue, this);
        windowSetting = new RuleSliderSetting("Window (s)", windowValue, this);
        settings.add(enabledSetting);
        settings.add(secondsSetting);
        settings.add(usesSetting);
        settings.add(windowSetting);
        settings.add(new ModeSetting("Scope", scopeValue, this::commit));
        settings.add(new ModeSetting("Trigger", triggerValue, this::commit));
        settings.add(new ModeSetting("Block", blockValue, this::commit));
    }

    /** Look up a {@link ItemCooldownRulesValue.Scope} by its persisted id or enum name; falls back to PVP_GRACE. */
    static ItemCooldownRulesValue.Scope scopeById(String id) {
        if (id != null) {
            for (ItemCooldownRulesValue.Scope scope : ItemCooldownRulesValue.Scope.values()) {
                if (scope.getId().equals(id) || scope.name().equalsIgnoreCase(id)) return scope;
            }
        }
        return ItemCooldownRulesValue.Scope.PVP_GRACE;
    }

    /** Look up a {@link ItemCooldownRulesValue.Trigger} by id or name; falls back to CONSUME_FINISH. */
    static ItemCooldownRulesValue.Trigger triggerById(String id) {
        if (id != null) {
            for (ItemCooldownRulesValue.Trigger trigger : ItemCooldownRulesValue.Trigger.values()) {
                if (trigger.getId().equals(id) || trigger.name().equalsIgnoreCase(id)) return trigger;
            }
        }
        return ItemCooldownRulesValue.Trigger.CONSUME_FINISH;
    }

    /** Look up a {@link ItemCooldownRulesValue.UseBlockMode} by id or name; falls back to NONE. */
    static ItemCooldownRulesValue.UseBlockMode blockById(String id) {
        if (id != null) {
            for (ItemCooldownRulesValue.UseBlockMode mode : ItemCooldownRulesValue.UseBlockMode.values()) {
                if (mode.getId().equals(id) || mode.name().equalsIgnoreCase(id)) return mode;
            }
        }
        return ItemCooldownRulesValue.UseBlockMode.NONE;
    }

    /** Pull the persisted rule (if any) into the seven editor values so the UI mirrors storage. */
    void syncFromRule() {
        ItemCooldownRulesValue rules = owner.rules();
        if (rules == null) return;
        ItemCooldownRulesValue.Rule rule = rules.getRule(itemId);
        if (rule == null) return;
        enabledValue.set(rule.enabled());
        secondsValue.set(rule.seconds());
        usesValue.set(rule.uses());
        windowValue.set(rule.windowSeconds());
        scopeValue.set(rule.scope().getId());
        triggerValue.set(rule.trigger().getId());
        blockValue.set(rule.blockMode().getId());
    }

    /** True while any of the three sliders is mid-drag; used to skip the read-back sync for that frame. */
    boolean isDraggingSlider() {
        return secondsSetting.ui().dragging || usesSetting.ui().dragging || windowSetting.ui().dragging;
    }

    /** Normalise the current editor values and push them back into the underlying rules store. */
    void commit() {
        ItemCooldownRulesValue rules = owner.rules();
        if (rules == null || !rules.containsRule(itemId)) return;
        ItemCooldownRulesValue.Rule current = rules.getRule(itemId);
        if (current == null) current = rules.getDefaultRule(itemId);

        ItemCooldownRulesValue.Rule next = new ItemCooldownRulesValue.Rule(
                enabledValue.get(),
                secondsValue.get(),
                usesValue.get(),
                windowValue.get(),
                scopeById(scopeValue.get()),
                triggerById(triggerValue.get()),
                blockById(blockValue.get())
        ).normalized();

        rules.putRule(itemId, next);
        syncFromRule();
        owner.syncBridge();
        owner.saveParentConfig();
    }

    /** Boolean toggle that commits the whole editor after a single click. */
    private static final class RuleBooleanSetting extends BooleanSetting {
        private final CooldownRuleEditor editor;

        RuleBooleanSetting(String name, BooleanValue value, CooldownRuleEditor editor) {
            super(name, value);
            this.editor = editor;
        }

        @Override
        public void mouseClicked(double mx, double my, int button) {
            boolean before = get();
            super.mouseClicked(mx, my, button);
            if (before != get()) editor.commit();
        }
    }

    /** Integer slider that commits the whole editor after value changes or a completed drag. */
    private static final class RuleSliderSetting extends SliderSetting<Integer> {
        private final CooldownRuleEditor editor;

        RuleSliderSetting(String name, NumberValue<Integer> value, CooldownRuleEditor editor) {
            super(name, value);
            this.editor = editor;
        }

        @Override
        public void mouseClicked(double mx, double my, int button) {
            int before = value().get();
            super.mouseClicked(mx, my, button);
            if (before != value().get()) editor.commit();
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            boolean wasDragging = ui().dragging;
            int before = value().get();
            super.mouseReleased(mx, my, button);
            if (wasDragging || before != value().get()) editor.commit();
        }
    }
}
