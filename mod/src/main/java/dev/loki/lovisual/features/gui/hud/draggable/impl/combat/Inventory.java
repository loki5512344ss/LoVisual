package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedInventoryHudPanel;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@HudElementRegister(order = 220)
public final class Inventory extends DraggableHudElement {

    {
        defaultLayout(1255.352f, 658.36005f);
    }

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final ScriptedInventoryHudPanel scriptedPanel = new ScriptedInventoryHudPanel();

    final KeyBindValue toggleBind = new KeyBindValue("toggle_bind", "I");
    final NumberValue<Double> scaleValue = new NumberValue<>("inventory_scale", 1.32, HudPanelLayoutModes.SCALE_MIN, HudPanelLayoutModes.SCALE_MAX);
    final ModeValue layoutMode = new ModeValue("inventory_layout", "Unified Divider", HudPanelLayoutModes.SPLIT_HEADER, HudPanelLayoutModes.UNIFIED_DIVIDER);
    final ModeValue colorMode = new ModeValue("inventory_color_mode", "Theme", InventoryConstants.COLOR_THEME, InventoryConstants.COLOR_CUSTOM);
    final ModeValue bgEffect = new ModeValue("inventory_bg_effect", "Blur", InventoryConstants.EFFECT_NONE, InventoryConstants.EFFECT_BLUR);
    final RGBAColorValue bg = new RGBAColorValue("inventory_bg", "#F7343434");
    final RGBAColorValue bg2 = new RGBAColorValue("inventory_bg_secondary", "#F7161616");
    final NumberValue<Integer> bgAlpha = new NumberValue<>("inventory_bg_alpha", 194, 0, 255);
    final RGBColorValue stroke = new RGBColorValue("inventory_stroke", "#5A5A5A");
    final RGBColorValue text = new RGBColorValue("inventory_text", "#FFFFFF");
    final RGBColorValue muted = new RGBColorValue("inventory_muted", "#A5A5A5");
    final NumberValue<Integer> blurAlpha = new NumberValue<>("inventory_blur_alpha", 255, 0, 255);
    final BooleanValue headerIconPulse = new BooleanValue("inventory_header_icon_pulse", true);
    final NumberValue<Integer> headerIconPulseSpeed = new NumberValue<>("inventory_header_icon_pulse_speed", 15, 1, 60);
    final NumberValue<Integer> headerIconPulseIntensity = new NumberValue<>("inventory_header_icon_pulse_intensity", 100, 0, 100);

    final List<QueuedItemIcon> pendingItemTasks = new ArrayList<>(InventoryConstants.COLS * InventoryConstants.ROWS);

    float displayWidth = -1.0f;
    float displayHeight = -1.0f;
    float visibilityAnim = 0.0f;
    int uiHeaderLeft;
    int uiHeaderRight;
    int uiBodyLeft;
    int uiBodyRight;
    int uiOutline;
    int uiText;
    int uiMuted;
    int uiCounter;
    int uiTitleText;
    int uiDivider;
    int uiGridDivider;
    int uiBlurTint;
    boolean layoutReady;
    boolean hitboxActive;
    boolean displayVisible = true;
    boolean toggleArmed = true;
    boolean toggleInit = false;

    public Inventory() {
        super("inventory", "Inventory", true);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.bind(toggleBind, BindMode.PRESS));
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
        defs.add(SettingDef.bool(headerIconPulse));
        defs.add(SettingDef.number(headerIconPulseSpeed).visibleWhen(headerIconPulse::get));
        defs.add(SettingDef.number(headerIconPulseIntensity).visibleWhen(headerIconPulse::get));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 385.0f;
        this.y = 40.0f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void onTick() {
        InventoryRenderHelper.handleTick(this);
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        InventoryRenderHelper.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public void renderEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        InventoryRenderHelper.renderForeground(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean contains(float mx, float my) {
        if (!hitboxActive && !DraggableHudElementRegistry.isForceVisible()) return false;
        return super.contains(mx, my);
    }

    boolean isThemeMode() {
        return InventoryConstants.COLOR_THEME.equals(colorMode.get());
    }

    boolean isCustomMode() {
        return InventoryConstants.COLOR_CUSTOM.equals(colorMode.get());
    }

    boolean hasEffect() {
        return !InventoryConstants.EFFECT_NONE.equals(bgEffect.get());
    }

    void setSize(float w, float h) {
        setBounds(getX(), getY(), w, h);
    }

    record QueuedItemIcon(ItemStack stack, float x, float y, float scale, int seed) {}
}
