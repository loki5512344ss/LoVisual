/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl;

import dev.loki.lovisual.config.SettingDef;
import dev.loki.lovisual.config.values.*;
import dev.loki.lovisual.features.gui.hud.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudNotifier;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.util.input.KeyManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;

@HudElementRegister(order = 40)
public final class CustomBar extends AbstractHudElement {

    public static final CustomBar INSTANCE = new CustomBar();
    static final float XP_BAR_WIDTH = 182f;
    static final double XP_TEXT_SCALE = 0.59;
    static final int XP_BAR_OFFSET = 2;
    static final int XP_TEXT_OFFSET = 0;
    static final float XP_BAR_HEIGHT = 5.0f;
    static final float XP_BAR_RADIUS = 2.5f;
    static final float BAR_SOFTNESS = 1.0f;
    static final float BAR_BLUR_ALPHA = 0.72f;
    static final float BAR_GLASS_ALPHA = 0.58f;
    static final float BAR_TRACK_ALPHA = 0.54f;
    static final float BAR_FILL_ALPHA = 0.82f;
    static final float BAR_STROKE_ALPHA = 0.48f;
    static final Identifier XP_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "hud/experience_bar_background");
    static final Identifier XP_PROGRESS = Identifier.fromNamespaceAndPath("minecraft", "hud/experience_bar_progress");
    static final Identifier LOCATOR_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_background");
    static final Identifier JUMP_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "hud/jump_bar_background");
    static final Identifier JUMP_COOLDOWN = Identifier.fromNamespaceAndPath("minecraft", "hud/jump_bar_cooldown");
    static final Identifier JUMP_PROGRESS = Identifier.fromNamespaceAndPath("minecraft", "hud/jump_bar_progress");
    static final float LOCATOR_BAR_HEIGHT = XP_BAR_HEIGHT;
    static final int LOCATOR_TICK_COUNT = 6;
    static final float LOCATOR_HEAD_SIZE = 6.5f;
    static final float LOCATOR_HEAD_RADIUS = 1.6f;
    static final float LOCATOR_HEAD_GAP = 0f;
    static final float LOCATOR_HEAD_RANGE = XP_BAR_WIDTH - LOCATOR_HEAD_SIZE;
    static final float LOCATOR_ANGLE_RANGE = 60f;
    static final float LOCATOR_HEAD_SCALE_START = 24f;
    static final float LOCATOR_HEAD_SCALE_END = 96f;
    static final float LOCATOR_HEAD_MIN_SCALE = 0.7f;
    static final float LOCATOR_OUTLINE_DISTANCE = 64f;
    static final float LOCATOR_OUTLINE_MIN = 0.6f;
    static final float LOCATOR_OUTLINE_MAX = 1.6f;
    static final Identifier LOCATOR_ARROW_UP = Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_arrow_up");
    static final Identifier LOCATOR_ARROW_DOWN = Identifier.fromNamespaceAndPath("minecraft", "hud/locator_bar_arrow_down");
    static final int LOCATOR_ARROW_W = 6;
    static final int LOCATOR_ARROW_H = 4;
    static final int LOCATOR_ARROW_GAP = 1;
    static final float LOCATOR_LABEL_SCALE = 0.285f;
    static final float LOCATOR_LABEL_PADDING_X = 3.0f;
    static final float LOCATOR_LABEL_PADDING_Y = 2.0f;
    static final float LOCATOR_LABEL_RADIUS = 4.8f;
    static final float LOCATOR_LABEL_OFFSET = 3.4f;
    static final float LOCATOR_LABEL_ANIM_SPEED = 14f;
    static final float LOCATOR_LABEL_ROW_GAP = 0.35f;
    static final float LOCATOR_LABEL_CHIP_PAD_X = 3.1f;
    static final float LOCATOR_LABEL_CHIP_PAD_Y = 1.35f;
    static final float LOCATOR_LABEL_CHIP_RADIUS = 3.6f;
    static final float LOCATOR_LABEL_ACCENT_W = 1.15f;
    static final int LOCATOR_LABEL_MAX_ROWS = 4;
    static final String COLOR_THEME = "Theme";
    static final String COLOR_CUSTOM = "Custom";

    private final NumberValue<Integer> overlayAlpha =
            new NumberValue<>("overlay_alpha", 255, 30, 255);
    private final BooleanValue xpBar =
            new BooleanValue("xp_bar", true);
    private final BooleanValue xpBarShowLevel =
            new BooleanValue("xp_bar_show_level", true);
    private final EnumValue<CustomBarModel.BarMode> barMode =
            new EnumValue<>("bar_mode", CustomBarModel.BarMode.LEVEL, CustomBarModel.BarMode.values());
    private final ModeValue colorMode =
            new ModeValue("bar_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    private final RGBColorValue xpBarBgColor =
            new RGBColorValue("xp_bar_bg_color", "#151515");
    private final RGBColorValue xpBarFillStart =
            new RGBColorValue("xp_bar_fill_start", "#4CB2FF");
    private final RGBColorValue xpBarFillEnd =
            new RGBColorValue("xp_bar_fill_end", "#76F0FF");
    private final RGBColorValue xpBarTextColor =
            new RGBColorValue("xp_bar_text_color", "#A0E5FF");
    private final EnumValue<CustomBarModel.XpBarStyle> xpBarStyle =
            new EnumValue<>("xp_bar_style", CustomBarModel.XpBarStyle.GRADIENT, CustomBarModel.XpBarStyle.values());
    final RGBAColorValue customBgColor =
            new RGBAColorValue("custom_bg_color", "#88151515");
    final RGBAColorValue customFillColor =
            new RGBAColorValue("custom_fill_color", "#CC4CB2FF");
    private final BooleanValue customFillGradient =
            new BooleanValue("custom_fill_gradient", true);
    final RGBAColorValue customFillGradientColor =
            new RGBAColorValue("custom_fill_gradient_color", "#CC76F0FF");
    private final NumberValue<Float> customFillGradientAngle =
            new NumberValue<>("custom_fill_gradient_angle", 0.0f, 0.0f, 360.0f);
    private final RGBAColorValue customTextColor =
            new RGBAColorValue("custom_text_color", "#FFA0E5FF");
    private final NumberValue<Float> customTextScale =
            new NumberValue<>("custom_text_scale", (float) XP_TEXT_SCALE, 0.3f, 1.2f);
    private final BooleanValue locatorLabels =
            new BooleanValue("locator_labels", true);
    private final EnumValue<CustomBarModel.LocatorLabelMode> locatorLabelMode =
            new EnumValue<>("locator_label_mode", CustomBarModel.LocatorLabelMode.CENTER, CustomBarModel.LocatorLabelMode.values());
    private final NumberValue<Float> locatorLabelAngle =
            new NumberValue<>("locator_label_angle", 6.0f, 4.0f, 45.0f);
    private final BooleanValue locatorLabelDistance =
            new BooleanValue("locator_label_distance", true);
    private final BooleanValue locatorSearchEnabled =
            new BooleanValue("locator_search_enabled", true);
    private final SetValue locatorSearchPlayers =
            new SetValue("locator_search_list", new java.util.LinkedHashSet<>(java.util.List.of("imhorny")));
    private final RGBColorValue locatorSearchColor =
            new RGBColorValue("locator_search_color", "#FFAA00");
    private final RGBColorValue locatorDimmedColor =
            new RGBColorValue("locator_dimmed_color", "#777777");
    private final BooleanValue locatorDimmedScaleOnly =
            new BooleanValue("locator_dimmed_scale_only", true);
    private final BooleanValue locatorMapFilterEnabled =
            new BooleanValue("locator_map_filter_enabled", false);
    private final StringValue locatorMapServer =
            new StringValue("locator_map_server", "");
    private final StringValue locatorMapUrl =
            new StringValue("locator_map_url", "example");
    private final KeyBindValue locatorTargetToggle =
            new KeyBindValue("locator_target_toggle", "NONE");
    private final KeyBindValue locatorBarToggle =
            new KeyBindValue("locator_bar_toggle", "U");

    private CustomBar() {
        super("vanilla_bar", "Bar", true);
    }

    public static CustomBar get() {
        return INSTANCE;
    }

    public static boolean shouldUseLocator(Minecraft mc, CustomBar bm) {
        if (mc == null || bm == null) return false;
        return switch (bm.getHotbarBarMode()) {
            case LOCATOR -> true;
            case LEVEL -> false;
            case AUTO -> mc.getConnection() != null
                    && mc.getConnection().getWaypointManager().hasWaypoints();
        };
    }

    public static boolean shouldUseJumpBar(Minecraft mc) {
        return CustomJumpBarRenderer.shouldUseJumpBar(mc);
    }

    public static void renderXpBarBackground(Renderer2D r2d, Minecraft mc) {
        CustomXpBarRenderer.renderXpBarBackground(r2d, mc);
    }

    public static void renderXpBarTexture(GuiGraphicsExtractor ctx, Minecraft mc) {
        CustomXpBarRenderer.renderXpBarTexture(ctx, mc);
    }

    public static void renderJumpBarBackground(Renderer2D r2d, Minecraft mc) {
        CustomJumpBarRenderer.renderJumpBarBackground(r2d, mc);
    }

    public static void renderJumpBarTexture(GuiGraphicsExtractor ctx, Minecraft mc) {
        CustomJumpBarRenderer.renderJumpBarTexture(ctx, mc);
    }

    public static void renderLocatorBackground(Renderer2D r2d, Minecraft mc) {
        CustomLocatorRenderer.renderLocatorBackground(r2d, mc);
    }

    public static void renderLocatorTexture(GuiGraphicsExtractor ctx, Minecraft mc) {
        CustomLocatorRenderer.renderLocatorTexture(ctx, mc);
    }

    public static void renderLocatorArrows(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        CustomLocatorRenderer.renderLocatorArrows(ctx, tickCounter, mc);
    }

    public static void renderLocatorHeads(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        CustomLocatorRenderer.renderLocatorHeads(ctx, tickCounter, mc);
    }

    public static void renderLocatorMarkers(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        CustomLocatorRenderer.renderLocatorMarkers(ctx, tickCounter, mc);
    }

    public static void renderXpBarText(GuiGraphicsExtractor ctx, Minecraft mc) {
        CustomXpBarRenderer.renderXpBarText(ctx, mc);
    }

    public static void renderPreview(Renderer2D r2d, float x, float y, float scale, float progress) {
        CustomXpBarRenderer.renderPreview(r2d, x, y, scale, progress);
    }

    public static CustomBarModel.XpTextInfo buildPreviewText(float barX, float barY, float scale, int level) {
        return CustomXpBarRenderer.buildPreviewText(barX, barY, scale, level);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.bool(xpBar));
        defs.add(SettingDef.bool(xpBarShowLevel).visibleWhen(xpBar::get));
        defs.add(SettingDef.mode(barMode).visibleWhen(xpBar::get));
        defs.add(SettingDef.mode(colorMode).visibleWhen(xpBar::get));
        defs.add(SettingDef.color(customBgColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(customFillColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(customFillGradientColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(customTextColor).visibleWhen(this::isCustomMode));

        defs.add(SettingDef.bool(locatorLabels).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.mode(locatorLabelMode)
                .visibleWhen(() -> locatorModeEnabled() && locatorLabels.get()));
        defs.add(SettingDef.number(locatorLabelAngle)
                .visibleWhen(() -> locatorModeEnabled()
                        && locatorLabels.get()
                        && locatorLabelModeUsesCenter()));
        defs.add(SettingDef.bool(locatorLabelDistance)
                .visibleWhen(() -> locatorModeEnabled() && locatorLabels.get()));

        defs.add(SettingDef.bool(locatorSearchEnabled).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.textList(locatorSearchPlayers)
                .visibleWhen(() -> locatorModeEnabled() && locatorSearchEnabled.get()));
        defs.add(SettingDef.colorNoAlpha(locatorDimmedColor)
                .visibleWhen(() -> locatorModeEnabled()
                        && locatorSearchEnabled.get()
                        && !locatorSearchPlayers.get().isEmpty()));
        defs.add(SettingDef.colorNoAlpha(locatorSearchColor)
                .visibleWhen(() -> locatorModeEnabled()
                        && locatorSearchEnabled.get()
                        && !locatorSearchPlayers.get().isEmpty()));
        defs.add(SettingDef.bool(locatorDimmedScaleOnly).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.bool(locatorMapFilterEnabled).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.text(locatorMapServer)
                .visibleWhen(() -> locatorModeEnabled() && locatorMapFilterEnabled.get()));
        defs.add(SettingDef.text(locatorMapUrl)
                .visibleWhen(() -> locatorModeEnabled() && locatorMapFilterEnabled.get()));

        defs.add(SettingDef.bind(locatorTargetToggle, BindMode.PRESS).visibleWhen(this::locatorModeEnabled));
        defs.add(SettingDef.bind(locatorBarToggle, BindMode.PRESS).visibleWhen(this::locatorModeEnabled));
    }

    @Override
    protected void onLoaded() {
        registerBind(locatorTargetToggle);
        registerBind(locatorBarToggle);
    }

    @Override
    public void onTick() {
        if (KeyManager.wasPressed(bindingName(locatorTargetToggle))) {
            boolean next = !locatorSearchEnabled.get();
            locatorSearchEnabled.set(next);
            HudNotifier.pushState("Locator target", next);
            saveConfig();
        }

        if (KeyManager.wasPressed(bindingName(locatorBarToggle))) {
            CustomBarModel.BarMode mode = barMode.get();
            if (mode != CustomBarModel.BarMode.AUTO) {
                barMode.set(mode == CustomBarModel.BarMode.LOCATOR ? CustomBarModel.BarMode.LEVEL : CustomBarModel.BarMode.LOCATOR);
                saveConfig();
            }
        }
    }

    public boolean isHudBarEnabled() {
        return isEnabled();
    }

    public float getHudOverlayAlphaFactor() {
        return getHudOverlayAlpha() / 255f;
    }

    public int getHudOverlayAlpha() {
        int v = overlayAlpha.get();
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    public boolean isHotbarXpBarEnabled() {
        return xpBar.get();
    }

    public boolean isHotbarXpBarShowLevel() {
        return xpBarShowLevel.get();
    }

    public CustomBarModel.BarMode getHotbarBarMode() {
        return barMode.get();
    }

    public int getHotbarXpBarBgRgb() {
        return xpBarBgColor.getArgb() & 0x00FFFFFF;
    }

    public int getHotbarXpBarFillStartRgb() {
        return xpBarFillStart.getArgb() & 0x00FFFFFF;
    }

    public int getHotbarXpBarFillEndRgb() {
        return xpBarFillEnd.getArgb() & 0x00FFFFFF;
    }

    public int getHotbarXpBarTextRgb() {
        return xpBarTextColor.getArgb() & 0x00FFFFFF;
    }

    public CustomBarModel.XpBarStyle getHotbarXpBarStyle() {
        return xpBarStyle.get();
    }

    public float getBarWidth() {
        return XP_BAR_WIDTH;
    }

    public float getBarHeight() {
        return XP_BAR_HEIGHT;
    }

    private boolean isCustomXpStyleEnabled() {
        return xpBar.get() && isCustomMode();
    }

    float getXpTextScale() {
        return (float) XP_TEXT_SCALE;
    }

    int getXpTextArgb() {
        if (isCustomXpStyleEnabled()) {
            return customTextColor.getArgb();
        }
        return HudRenderUtil.mixColor(theme().textPrimary(), theme().accent(), 0.32f);
    }

    boolean isCustomMode() {
        return COLOR_CUSTOM.equals(colorMode.get());
    }

    public boolean isLocatorSearchEnabled() {
        return locatorModeEnabled() && locatorSearchEnabled.get();
    }

    public Set<String> getLocatorSearchPlayers() {
        return locatorSearchPlayers.get();
    }

    public int getLocatorDimmedColorRgb() {
        return locatorDimmedColor.getArgb() & 0x00FFFFFF;
    }

    public int getLocatorSearchColorRgb() {
        return locatorSearchColor.getArgb() & 0x00FFFFFF;
    }

    public boolean isLocatorDimmedScaleOnly() {
        return locatorDimmedScaleOnly.get();
    }

    public boolean isLocatorMapFilterEnabled() {
        return locatorMapFilterEnabled.get();
    }

    public String getLocatorMapServer() {
        return locatorMapServer.get();
    }

    public String getLocatorMapUrl() {
        return locatorMapUrl.get();
    }

    public boolean isLocatorLabelsEnabled() {
        return locatorModeEnabled() && locatorLabels.get();
    }

    public boolean isLocatorLabelDistanceEnabled() {
        return isLocatorLabelsEnabled() && locatorLabelDistance.get();
    }

    public float getLocatorLabelAngle() {
        return locatorLabelAngle.get();
    }

    public CustomBarModel.LocatorLabelMode getLocatorLabelMode() {
        return locatorLabelMode.get();
    }

    private boolean locatorModeEnabled() {
        if (!xpBar.get()) return false;
        return barMode.get() != CustomBarModel.BarMode.LEVEL;
    }

    private boolean locatorLabelModeUsesCenter() {
        CustomBarModel.LocatorLabelMode mode = locatorLabelMode.get();
        return mode == CustomBarModel.LocatorLabelMode.CENTER || mode == CustomBarModel.LocatorLabelMode.CENTER_TARGETS;
    }

    private String bindingName(KeyBindValue value) {
        return name() + ":" + value.getName();
    }

    private void registerBind(KeyBindValue value) {
        String name = bindingName(value);
        KeyManager.unregisterAll(name);
        if (!value.isNone()) {
            KeyManager.registerCombo(name, value.get());
        }
    }
}
