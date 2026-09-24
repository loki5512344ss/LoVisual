package dev.loki.lovisual.features.gui.clickgui.settings.cooldown;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.features.gui.clickgui.settings.owner.PickerDetailOwner;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.util.wav.util.ClickGuiSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Picker-backed editor for per-item cooldown rules.
 * <p>
 * Grid clicks only focus an item. Adding/removing a rule and editing its fields happens in the
 * picker detail panel. The field rows intentionally reuse the regular settings renderer instead
 * of custom miniature buttons, and numeric values use the normal slider setting.
 */
public final class CooldownRulesSetting extends TextListSetting implements PickerDetailOwner {

    private final ItemCooldownRulesValue rules;
    private final Map<String, CooldownRuleEditor> editors = new HashMap<>();
    private final CooldownRulesDetailRenderer detail;
    private String focusedItemId;

    public CooldownRulesSetting(String name, ItemCooldownRulesValue rules) {
        super(name, bridgeValue(rules), TextListSetting.PickerMode.ITEMS);
        this.rules = rules;
        this.detail = new CooldownRulesDetailRenderer(this);
        syncBridge();
    }

    private static SetValue bridgeValue(ItemCooldownRulesValue rules) {
        return new SetValue(rules != null ? rules.getName() + "_picker_items" : "cooldown_rules_picker_items",
                rules != null ? rules.getItemIds() : Set.of());
    }

    @Override
    public ConfigValue<?> getConfigValue() {
        return rules;
    }

    @Override
    public Object save() {
        return rules.toJson();
    }

    @Override
    public void load(Object o) {
        rules.fromJson(o);
        editors.clear();
        syncBridge();
    }

    @Override
    public Set<String> getValueSet() {
        return rules == null ? new LinkedHashSet<>() : rules.getItemIds();
    }

    @Override
    public void setValueSet(Set<String> next) {
        if (rules == null) return;
        Set<String> normalized = new LinkedHashSet<>();
        if (next != null) {
            for (String raw : next) {
                String id = normalizeEntry(raw);
                if (id != null && !id.isBlank()) normalized.add(id);
            }
        }
        for (String existing : new ArrayList<>(rules.getItemIds())) {
            if (!normalized.contains(existing)) {
                rules.removeRule(existing);
                editors.remove(existing);
            }
        }
        for (String id : normalized) rules.ensureRule(id);
        if (focusedItemId != null && !normalized.contains(focusedItemId) && normalized.isEmpty()) focusedItemId = null;
        syncBridge();
        saveParentConfig();
    }

    @Override
    public String normalizeEntry(String line) {
        String normalized = ItemCooldownRulesValue.normalizeItemId(line);
        return normalized != null ? normalized : "";
    }

    @Override
    public String getEditorText() {
        return String.join("\n", getValueSet());
    }

    @Override
    public float pickerDetailWidth() {
        return CooldownRulesDetailRenderer.WIDTH;
    }

    @Override
    public boolean shouldToggleSelectionOnCardClick() {
        return false;
    }

    @Override
    public void onPickerFocusChanged(String id) {
        String normalized = normalizeEntry(id);
        if (normalized == null || normalized.isBlank()) return;
        if (!normalized.equals(focusedItemId)) detail.resetScroll();
        focusedItemId = normalized;
    }

    @Override
    public void renderPickerDetails(float x, float y, float w, float h, String focusedId, float mouseX, float mouseY) {
        detail.render(x, y, w, h, resolveFocusedId(focusedId), mouseX, mouseY);
    }

    @Override
    public boolean mouseClickedPickerDetails(float mouseX, float mouseY, int button) {
        return detail.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleasedPickerDetails(float mouseX, float mouseY, int button) {
        detail.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolledPickerDetails(float mouseX, float mouseY, double delta) {
        return detail.mouseScrolled(mouseX, mouseY, delta);
    }

    /** Actions routed from the header Add/Remove chip. */
    public enum Action { ADD_RULE, REMOVE_RULE }

    /** Screen rect of a chip button in the detail panel. */
    record ActionHit(Action action, String itemId, float x, float y, float w, float h) { }

    /** Screen rect of a nested editor setting inside the detail panel body. */
    record SettingHit(dev.loki.lovisual.features.gui.clickgui.settings.core.Setting setting, float x, float y, float w, float h) { }

    /** Lazily create / fetch the per-item editor; keeps slider drag state alive across frames. */
    CooldownRuleEditor editorFor(String itemId) {
        return editors.computeIfAbsent(itemId, id -> new CooldownRuleEditor(this, id));
    }

    ItemCooldownRulesValue rules() { return rules; }

    /** Resolve which item id to show in the detail panel, falling back to the first selected item. */
    String resolveFocusedId(String focusedId) {
        String normalized = normalizeEntry(focusedId);
        if (normalized != null && !normalized.isBlank()) {
            focusedItemId = normalized;
            return normalized;
        }
        if (focusedItemId != null && !focusedItemId.isBlank()) return focusedItemId;
        Set<String> ids = getValueSet();
        if (ids.isEmpty()) return null;
        focusedItemId = ids.iterator().next();
        return focusedItemId;
    }

    /** Apply an ADD / REMOVE chip click to the underlying rules store and refresh the picker bridge. */
    void handleAction(Action action, String itemId) {
        if (rules == null || itemId == null || itemId.isBlank()) return;
        switch (action) {
            case ADD_RULE -> {
                rules.ensureRule(itemId);
                focusedItemId = itemId;
                detail.resetScroll();
                editorFor(itemId).syncFromRule();
                syncBridge();
                saveParentConfig();
                ClickGuiSounds.changeMode();
            }
            case REMOVE_RULE -> {
                rules.removeRule(itemId);
                editors.remove(itemId);
                focusedItemId = itemId;
                detail.resetScroll();
                syncBridge();
                saveParentConfig();
                ClickGuiSounds.changeMode();
            }
        }
    }

    /** Mirror the current rules' item-id set into the parent {@link TextListSetting} picker value. */
    void syncBridge() {
        if (super.getConfigValue() instanceof SetValue set) set.set(getValueSet());
    }

    void saveParentConfig() {
        if (getParent() != null) getParent().saveConfig();
    }
}
