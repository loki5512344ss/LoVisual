package dev.loki.lovisual.features.gui.clickgui.settings.cooldown;

import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.CooldownRulesSetting.Action;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.CooldownRulesSetting.ActionHit;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.CooldownRulesSetting.SettingHit;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the right-hand "detail" panel of {@link CooldownRulesSetting}: item header
 * (icon + name + Add/Remove chip), the vertical editor body and the scrollbar. Also
 * owns the click hit lists consumed by {@link #mouseClicked} and the scroll offset
 * adjusted by {@link #mouseScrolled}.
 */
final class CooldownRulesDetailRenderer {

    static final float WIDTH = 380f;
    private static final float PANEL_PAD = 10f;
    private static final float HEADER_H = 112f;
    private static final float SCROLL_STEP = 18f;
    private static final float SCROLLBAR_W = 3.25f;

    private final CooldownRulesSetting owner;
    private final List<ActionHit> actionHits = new ArrayList<>();
    private final List<SettingHit> settingHits = new ArrayList<>();
    private float scroll;
    private float maxScroll;

    CooldownRulesDetailRenderer(CooldownRulesSetting owner) { this.owner = owner; }

    /** Reset the panel's scroll offset back to the top (called when the focus changes). */
    void resetScroll() { scroll = 0f; }

    /** Repaint the detail panel for one frame; records click hits consumed by {@link #mouseClicked}. */
    void render(float x, float y, float w, float h, String itemId, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        actionHits.clear();
        settingHits.clear();
        ItemCooldownRulesValue rules = owner.rules();
        ClickGuiRenderer.drawRoundedRect(x, y, w, h, 8f, UnifiedSettingsSkin.SURFACE_SOFT);
        ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, 8f, 0.8f, UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 70));
        ClickGuiRenderer.drawText(UnifiedSettingsSkin.fontSemibold(), "Cooldown Rule",
                x + PANEL_PAD, y + 13f, 20f, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        ClickGuiRenderer.drawRoundedRect(x + PANEL_PAD, y + 39f, w - PANEL_PAD * 2f, 1f, 0.5f,
                UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 48));
        boolean hasItem = itemId != null && !itemId.isBlank() && rules != null;
        boolean hasRule = hasItem && rules.containsRule(itemId);
        if (hasItem) {
            ItemStack stack = toStack(itemId);
            drawItemHeader(x, y, w, itemId, stack.isEmpty() ? itemId : stack.getHoverName().getString(), stack, hasRule, mx, my);
        }
        if (!hasItem) {
            scroll = 0f; maxScroll = 0f;
            drawHint(x, y + h * 0.5f - 14f, w, "Select an item on the left.", "A grid click only focuses it; rule creation is explicit.");
        } else if (!hasRule) {
            scroll = 0f; maxScroll = 0f;
            drawHint(x, y + HEADER_H + 14f, w, "No cooldown rule for this item.", "Press Add to create one, then configure it below.");
        } else {
            drawEditorBody(x, y, w, h, owner.editorFor(itemId), mx, my);
        }
    }

    /** Route a left click through the recorded action / setting hit rects. */
    boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        for (ActionHit hit : new ArrayList<>(actionHits)) {
            if (!inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            owner.handleAction(hit.action(), hit.itemId());
            return true;
        }
        for (SettingHit hit : new ArrayList<>(settingHits)) {
            if (!inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            hit.setting().mouseClicked(mx, my, button);
            return true;
        }
        return false;
    }

    /** Forward a release to every editor setting so sliders can end their drag state. */
    void mouseReleased(double mx, double my, int button) {
        for (SettingHit hit : new ArrayList<>(settingHits)) hit.setting().mouseReleased(mx, my, button);
    }
    /** Adjust the vertical scroll; returns true to signal the wheel event was consumed. */
    boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll > 0.5f) scroll = Math.max(0f, Math.min(scroll - (float) (delta * SCROLL_STEP), maxScroll));
        return true;
    }
    private void drawEditorBody(float x, float y, float w, float h, CooldownRuleEditor editor, float mx, float my) {
        if (!editor.isDraggingSlider()) editor.syncFromRule();
        List<Setting> settings = editor.settings;
        float contentX = x + PANEL_PAD;
        float contentY = y + HEADER_H;
        float contentH = Math.max(1f, h - HEADER_H - PANEL_PAD);
        float total = computeSettingsContentHeight(settings);
        maxScroll = Math.max(0f, total - contentH);
        scroll = Math.max(0f, Math.min(scroll, maxScroll));
        float contentW = w - PANEL_PAD * 2f - (maxScroll > 0.5f ? SCROLLBAR_W + 5f : 0f);
        boolean clipped = ScissorFunction.pushRaw(contentX - 1f, contentY - 1f, contentW + 2f, contentH + 2f);
        float cy = contentY - scroll;
        for (Setting setting : settings) {
            float rowH = setting.getHeight();
            if (cy + rowH >= contentY - 2f && cy <= contentY + contentH + 2f) {
                setting.render(contentX, cy, contentW, mx, my);
                settingHits.add(new SettingHit(setting, contentX, cy, contentW, rowH));
            }
            cy += rowH + 2f;
        }
        if (clipped) ScissorFunction.pop();
        drawScrollbar(x, contentY, w, contentH);
    }

    private void drawItemHeader(float x, float y, float w, String itemId, String name, ItemStack stack, boolean hasRule, float mx, float my) {
        TextRenderer titleFont = UnifiedSettingsSkin.fontSemibold();
        TextRenderer font = UnifiedSettingsSkin.fontRegular();
        float iconBox = 46f, iconX = x + PANEL_PAD, iconY = y + 50f;
        ClickGuiRenderer.drawRoundedRect(iconX, iconY, iconBox, iconBox, 7f, UnifiedSettingsSkin.SURFACE);
        ClickGuiRenderer.drawRoundedRectStroke(iconX, iconY, iconBox, iconBox, 7f, 0.75f, UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 75));
        if (!stack.isEmpty()) {
            float scale = 1.85f;
            float px = 16f * scale;
            ClickGuiRenderer.queuePickerIcon(stack, iconX + (iconBox - px) * 0.5f, iconY + (iconBox - px) * 0.5f, scale, iconX, iconY, iconBox, iconBox);
        }
        float textX = iconX + iconBox + 11f;
        float buttonW = hasRule ? 76f : 68f, buttonH = 22f;
        float buttonX = x + w - PANEL_PAD - buttonW, buttonY = y + 62f;
        ClickGuiRenderer.drawText(titleFont, fit(titleFont, name, 15f, buttonX - textX - 10f), textX, y + 53f, 15f, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        ClickGuiRenderer.drawText(font, fit(font, itemId, 11.5f, buttonX - textX - 10f), textX, y + 73f, 11.5f, UnifiedSettingsSkin.TEXT_MUTED, false);
        chip(buttonX, buttonY, buttonW, buttonH, hasRule ? "Remove" : "Add", inside(mx, my, buttonX, buttonY, buttonW, buttonH), hasRule);
        actionHits.add(new ActionHit(hasRule ? Action.REMOVE_RULE : Action.ADD_RULE, itemId, buttonX, buttonY, buttonW, buttonH));
    }

    private void drawHint(float x, float top, float w, String line1, String line2) {
        TextRenderer font = UnifiedSettingsSkin.fontRegular();
        float lineH = ClickGuiRenderer.textHeight(font, 12f) + 4f;
        ClickGuiRenderer.drawText(font, line1, x + PANEL_PAD, top, 12f, UnifiedSettingsSkin.TEXT_MUTED, false);
        ClickGuiRenderer.drawText(font, fit(font, line2, 12f, w - PANEL_PAD * 2f), x + PANEL_PAD, top + lineH, 12f, UnifiedSettingsSkin.TEXT_MUTED, false);
    }

    private void chip(float x, float y, float w, float h, String text, boolean hover, boolean destructive) {
        int bg = destructive ? UnifiedSettingsSkin.withAlpha(0xFFFF5555, 32) : UnifiedSettingsSkin.ACCENT_SOFT;
        int stroke = destructive ? UnifiedSettingsSkin.withAlpha(0xFFFF7777, 120) : UnifiedSettingsSkin.ACCENT;
        if (hover) bg = UnifiedSettingsSkin.mix(bg, UnifiedSettingsSkin.SURFACE_HOVER, 0.45f);
        ClickGuiRenderer.drawRoundedRect(x, y, w, h, 4f, bg);
        ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, 4f, 0.6f, stroke);
        TextRenderer font = UnifiedSettingsSkin.fontRegular();
        float tw = ClickGuiRenderer.textWidth(font, text, 10.5f), th = ClickGuiRenderer.textHeight(font, 10.5f);
        ClickGuiRenderer.drawText(font, text, x + (w - tw) * 0.5f, y + (h - th) * 0.5f, 10.5f, UnifiedSettingsSkin.TEXT_PRIMARY, false);
    }

    private void drawScrollbar(float panelX, float contentY, float panelW, float contentH) {
        if (maxScroll <= 0.5f) return;
        float trackX = panelX + panelW - PANEL_PAD - SCROLLBAR_W, trackY = contentY + 1f;
        float trackH = Math.max(1f, contentH - 2f);
        ClickGuiRenderer.drawRoundedRect(trackX, trackY, SCROLLBAR_W, trackH, SCROLLBAR_W * 0.5f,
                UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 38));
        float thumbH = Math.max(18f, trackH * (contentH / (contentH + maxScroll)));
        float thumbY = trackY + Math.max(1f, trackH - thumbH) * (scroll / Math.max(1f, maxScroll));
        ClickGuiRenderer.drawRoundedRect(trackX, thumbY, SCROLLBAR_W, thumbH, SCROLLBAR_W * 0.5f,
                UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_MUTED, 120));
    }

    private static float computeSettingsContentHeight(List<Setting> settings) {
        float h = 0f;
        for (int i = 0; i < settings.size(); i++) h += settings.get(i).getHeight() + (i + 1 < settings.size() ? 2f : 0f);
        return h;
    }

    private static ItemStack toStack(String itemId) {
        try {
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) return ItemStack.EMPTY;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String fit(TextRenderer font, String text, float size, float width) {
        return ClickGuiRenderer.fitText(font, text == null ? "" : text, size, Math.max(1f, width));
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
