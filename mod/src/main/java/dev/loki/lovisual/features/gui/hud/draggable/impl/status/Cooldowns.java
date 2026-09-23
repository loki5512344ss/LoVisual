/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@HudElementRegister(order = 30)
public final class Cooldowns extends DraggableHudElement {

    {
        defaultLayout(1160.4823f, 466.0f);
    }

    static final float HEADER_HEIGHT = 15.5f;
    static final float BODY_Y_OFFSET = 18.5f;
    static final float CONTENT_START_Y = 25.0f;
    static final float BODY_INSET_Y = 6.5f;
    static final float ROW_STEP = 11.0f;
    static final float MIN_WIDTH = 110.0f;
    static final float PANEL_RADIUS = 4.0f;
    static final float PANEL_SOFTNESS = 1.0f;
    static final float PANEL_STROKE = 0.55f;
    static final float HEADER_DIVIDER_X = 18.0f;
    static final float HEADER_DIVIDER_Y = 5.0f;
    static final float HEADER_DIVIDER_H = 6.0f;
    static final float ROW_DIVIDER_X = 15.0f;
    static final float ROW_DIVIDER_H = 6.0f;
    static final float TITLE_ICON_X = 4.0f;
    static final float TITLE_TEXT_X = 22.0f;
    static final float COUNT_LABEL_OFFSET = 22.0f;
    static final float COUNT_VALUE_OFFSET = 2.5f;
    static final float ROW_ICON_X = 3.5f;
    static final float ROW_ICON_VISUAL_SIZE = 8.0f;
    static final float ROW_TEXT_X = 18.0f;
    static final float ROW_TIME_RIGHT = 8.0f;
    static final float ROW_CONTENT_CENTER_OFFSET = 1.9f;
    static final int BLINK_THRESHOLD_TICKS = 100;
    static final double BLINK_SPEED = 0.008;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";
    static final String EFFECT_NONE = "None";
    static final String EFFECT_BLUR = "Blur";
    static final int PVP_ACTIVE_COLOR = 0xFFFFA500;
    static final int PVP_IDLE_COLOR = 0xFF00A0FF;
    static final int PVP_BLOCKED_COLOR = 0xFFFF5656;
    static final Item[] PREVIEW_ITEMS = new Item[]{Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE, Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP};
    static ItemStack[] previewStacks;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final ScriptedListHudPanel scriptedPanel = new ScriptedListHudPanel();
    final NumberValue<Double> scaleValue = new NumberValue<>("cooldowns_scale", 1.0, HudPanelLayoutModes.SCALE_MIN, HudPanelLayoutModes.SCALE_MAX);
    final ModeValue layoutMode = new ModeValue("cooldowns_layout", "Split Header", HudPanelLayoutModes.SPLIT_HEADER, HudPanelLayoutModes.UNIFIED_DIVIDER);
    final ModeValue colorMode = new ModeValue("cooldowns_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    final ModeValue bgEffect = new ModeValue("cooldowns_bg_effect", "Blur", EFFECT_NONE, EFFECT_BLUR);
    final BooleanValue blink = new BooleanValue("cooldowns_blink", true);
    final RGBAColorValue bg = new RGBAColorValue("cooldowns_bg", "#EB111318");
    final RGBAColorValue bg2 = new RGBAColorValue("cooldowns_bg_secondary", "#F7161616");
    final NumberValue<Integer> bgAlpha = new NumberValue<>("cooldowns_bg_alpha", 185, 0, 255);
    final RGBColorValue stroke = new RGBColorValue("cooldowns_stroke", "#5A5A5A");
    final RGBColorValue text = new RGBColorValue("cooldowns_text", "#FFFFFF");
    final RGBColorValue muted = new RGBColorValue("cooldowns_muted", "#A5A5A5");
    final NumberValue<Integer> blurAlpha = new NumberValue<>("cooldowns_blur_alpha", 255, 0, 255);
    final BooleanValue headerIconPulse = new BooleanValue("cooldowns_header_icon_pulse", true);
    final NumberValue<Integer> headerIconPulseSpeed = new NumberValue<>("cooldowns_header_icon_pulse_speed", 18, 1, 60);
    final NumberValue<Integer> headerIconPulseIntensity = new NumberValue<>("cooldowns_header_icon_pulse_intensity", 100, 0, 100);

    final Map<String, Float> entryAnim = new LinkedHashMap<>();
    final Map<String, Row> lastEntries = new LinkedHashMap<>();
    final List<QueuedItemIcon> pendingItemTasks = new ArrayList<>(9);
    float displayWidth = -1.0f;
    float displayHeight = -1.0f;
    float visibilityAnim = 0.0f;
    boolean foregroundReady;
    int uiHeaderLeft; int uiHeaderRight; int uiBodyLeft; int uiBodyRight; int uiOutline; int uiText; int uiMuted; int uiCounter; int uiTitleText; int uiDivider; int uiBlurTint;

    public Cooldowns() { super("cooldowns", "Cooldowns", true); }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scaleValue));
        defs.add(SettingDef.mode(layoutMode));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.color(bg).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(bg2).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(stroke).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(this::isThemeMode));
        defs.add(SettingDef.colorNoAlpha(text).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(muted).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.bool(blink));
        defs.add(SettingDef.bool(headerIconPulse));
        defs.add(SettingDef.number(headerIconPulseSpeed).visibleWhen(headerIconPulse::get));
        defs.add(SettingDef.number(headerIconPulseIntensity).visibleWhen(headerIconPulse::get));
    }

    @Override public void applyDefaultPosition(int screenW, int screenH) { this.x = screenW - 132.0f; this.y = 20.0f; }
    @Override public boolean usesEngineRenderer() { return true; }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        CooldownsRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public void renderEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        if (!foregroundReady || ctx == null || pendingItemTasks.isEmpty()) { pendingItemTasks.clear(); return; }
        for (QueuedItemIcon task : pendingItemTasks) renderer.item(task.stack(), task.x(), task.y(), task.scale(), task.seed(), Renderer2D.ITEM_OVERLAY_NONE, null);
        pendingItemTasks.clear();
    }

    @Override public boolean isDraggable() { return super.isDraggable(); }

    boolean isThemeMode() { return CooldownsPalette.isThemeMode(this); }
    boolean isCustomMode() { return CooldownsPalette.isCustomMode(this); }
    boolean hasEffect() { return CooldownsPalette.hasEffect(this); }
    boolean isForceVisibleForRenderer() { return dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry.isForceVisible() || dev.loki.lovisual.util.screen.ClientScreen.current() instanceof net.minecraft.client.gui.screens.ChatScreen; }

    void setSize(float w, float h) { setBounds(getX(), getY(), w, h); }
    void setEmpty() { setBounds(getX(), getY(), 0.0f, 0.0f); }

    record Row(String key, ItemStack stack, String name, String duration, int remainingTicks, int timeColor) {}
    record AnimatedRow(Row row, float anim) {}
    record QueuedItemIcon(ItemStack stack, float x, float y, float scale, int seed) {}
}
