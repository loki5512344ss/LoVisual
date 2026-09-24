package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.ItemIdSetValue;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.util.input.KeyManager;
import dev.loki.lovisual.util.item.enchant.TopEnchantUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

@HudElementRegister(order = 20)
public final class BetterTooltips extends AbstractHudElement {

    private static final String[] TOOLTIP_FONT_OPTIONS = {
            "Onest", "OnestMedium", "OnestLight", "Inter", "InterMedium", "Comfortaa", "Iosevka", "ProFont", "Monsterrat"
    };
    public static final BetterTooltips INSTANCE = new BetterTooltips();
    public static final String GROUP_TOOLTIP_GUI = "vanilla_tooltip_gui";
    public static final String GROUP_TOOLTIP_ITEMS = "vanilla_tooltip_items";
    public static final String GROUP_SHULKER_PREVIEW = "vanilla_shulker_preview";
    private static final String TOOLTIP_PANEL_LAYOUT_ID = "lovisual:api/hud/static/tooltip_panel";
    static final int TOOLTIP_PAD_X = 4;
    static final int TOOLTIP_PAD_Y = 3;
    static final float TOOLTIP_STROKE_WIDTH = 0.55f;
    static final float TOOLTIP_GLOW_SIZE = 3.75f;
    static final int TOOLTIP_BG_ALPHA = 0xEE;
    static final int TOOLTIP_STROKE_ALPHA = 0xDC;
    static final int TOOLTIP_GLOW_ALPHA = 0x5C;
    static final float TOOLTIP_TEXT_SCALE = 0.35f;
    static final int TOOLTIP_LINE_GAP = 0;
    static final int TOOLTIP_PREVIEW_GAP = 3;
    static final int TOOLTIP_TITLE_GAP = 2;
    static final float SHULKER_BG_RADIUS = 1.5f;
    static final float SHULKER_BG_SOFTNESS = 0.45f;
    static final int SHULKER_SLOT_BG = 0xFF1A1C20;
    static final int SHULKER_SLOT_BORDER = 0x8A34383E;
    static final float SHULKER_SLOT_RADIUS = 2.0f;
    static final float SHULKER_SLOT_SOFTNESS = 0.65f;
    static final float SHULKER_SLOT_GAP = 2.0f;
    static final float SHULKER_PADDING = 4.0f;

    private final BooleanValue itemTooltipEnabled = bool("item_tooltip", true);
    private final BooleanValue shulkerPreviewEnabled = bool("shulker_preview", true);
    private final BooleanValue itemInfoColorize = bool("item_info_colorize", true);
    private final ModeValue tooltipFont = mode("tooltip_font", "Iosevka", TOOLTIP_FONT_OPTIONS);
    private final NumberValue<Float> tooltipTextSize = num("tooltip_text_size", 1.0f, 0.5f, 2.0f);
    private final ItemIdSetValue topIgnore = registerTopIgnore();
    private final NumberValue<Float> shulkerPreviewSlotSize =
            visibleWhen(num("shulker_slot_size", 18.0f, 12.0f, 24.0f), this::isShulkerPreviewEnabled);
    private final KeyBindValue shulkerPreviewHold =
            visibleWhen(bind("shulker_preview_hold", "LEFT_SHIFT", BindMode.HOLD), this::isShulkerPreviewEnabled);
    private final UiScriptModuleHandle tooltipPanelHandle = HudScriptLayouts.handle(TOOLTIP_PANEL_LAYOUT_ID);
    private final CachedUiScriptRuntime tooltipRuntime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());

    private BetterTooltips() {
        super("vanilla_tooltips", "Tooltips", true);
    }

    public static BetterTooltips get() { return INSTANCE; }
    public static boolean isGroupId(String id) { return SettingsGroup.fromId(id) != null; }
    public static void beginTooltipFrame() { TooltipState.beginFrame(); }
    public static boolean hasTooltip() { return TooltipState.hasTooltip(); }
    public static void captureTooltipOrdered(List<? extends FormattedCharSequence> l, ClientTooltipPositioner p, int x, int y) {
        TooltipState.captureOrdered(l, p, x, y);
    }
    public static void captureItemTooltipOrdered(List<? extends FormattedCharSequence> l, ClientTooltipPositioner p, int x, int y, ItemStack s, GuiGraphicsExtractor c) {
        TooltipState.captureItemOrdered(l, p, x, y, s, c);
    }
    public static void setDrawContext(GuiGraphicsExtractor c) { TooltipState.setDrawContext(c); }
    public static void setLastTooltipStack(ItemStack s) { TooltipState.setLastStack(s); }
    public static ItemStack consumeLastTooltipStack() { return TooltipState.consumeLastStack(); }
    public static void renderTooltip() {
        if (!hasTooltip()) return;
        renderTooltipInternal(TooltipState.getLines(), TooltipState.getPositioner(), TooltipState.getMouseX(), TooltipState.getMouseY());
    }
    public static void setTooltipScaleOverride(float s) { TooltipState.setScaleOverride(s); }
    public static void resetTooltipScaleOverride() { TooltipState.resetScaleOverride(); }
    public static void renderTooltipWithContext(GuiGraphicsExtractor c) {
        if (c != null && TooltipState.getCtx() == null) TooltipState.setDrawContext(c);
        renderTooltip();
    }

    private static void renderTooltipInternal(List<TooltipLine> lines, ClientTooltipPositioner pos, int mouseX, int mouseY) {
        TooltipPanelRenderer.renderTooltipInternal(INSTANCE, lines, pos, mouseX, mouseY);
    }
    String tooltipFontValue() { return tooltipFont.get(); }
    float tooltipTextSizeValue() { return tooltipTextSize.get(); }
    UiScriptModuleHandle tooltipPanelHandle() { return tooltipPanelHandle; }
    CachedUiScriptRuntime tooltipRuntime() { return tooltipRuntime; }

    @Override protected void defineSettings(List<SettingDef> defs) { defs.add(0, enabledSettingDef()); }
    @Override protected void onLoaded() { registerBind(shulkerPreviewHold); }

    public List<SettingDef> getSettingsGroup(String groupId) {
        SettingsGroup g = SettingsGroup.fromId(groupId);
        if (g == null) return List.of();
        List<SettingDef> defs = new ArrayList<>();
        switch (g) {
            case TOOLTIP_GUI -> defs.add(enabledSettingDef());
            case TOOLTIP_ITEMS -> add(defs, itemTooltipEnabled, itemInfoColorize, topIgnore);
            case SHULKER_PREVIEW -> add(defs, shulkerPreviewEnabled, shulkerPreviewSlotSize, shulkerPreviewHold);
        }
        return defs;
    }

    private ItemIdSetValue registerTopIgnore() {
        ItemIdSetValue v = TopEnchantUtil.ignoreValue();
        return declareSetting(v, SettingDef.textList(v));
    }

    private void add(List<SettingDef> defs, ConfigValue<?>... vs) {
        for (ConfigValue<?> v : vs) { SettingDef d = settingDef(v); if (d != null) defs.add(d); }
    }

    public boolean useCustomGuiTooltips() { BetterButtons b = BetterButtons.get(); if (b != null && b.useUiButtons()) return true; return isEnabled(); }
    public boolean useCustomItemTooltips() { BetterButtons b = BetterButtons.get(); if (b != null && b.useUiButtons()) return true; return itemTooltipEnabled.get(); }
    public boolean isItemInfoColorizeEnabled() { return itemInfoColorize.get(); }
    public boolean isTooltipAlphaEnabled() { return anyTooltipsEnabled(); }
    public int tooltipColor() { return (TOOLTIP_BG_ALPHA << 24) | 0x00101114; }
    public boolean isShulkerPreviewEnabled() { return shulkerPreviewEnabled.get(); }
    public void setShulkerPreviewEnabled(boolean v) { shulkerPreviewEnabled.set(v); saveConfig(); }
    public boolean isGuiTooltipEnabled() { return isEnabled(); }
    public void setGuiTooltipEnabled(boolean v) { setEnabled(v); }
    public boolean isItemTooltipEnabled() { return itemTooltipEnabled.get(); }
    public void setItemTooltipEnabled(boolean v) { itemTooltipEnabled.set(v); saveConfig(); }
    public boolean isShulkerPreviewHeld() { return KeyManager.isHeldAllowScreen(bindingName(shulkerPreviewHold)); }
    public String getShulkerPreviewBind() { return shulkerPreviewHold.get(); }
    public int shulkerPreviewBgRgb() { return Theme.theme().windowBg() & 0x00FFFFFF; }
    public int shulkerPreviewBgAlpha() { return TOOLTIP_BG_ALPHA; }
    public float shulkerPreviewBgRadius() { return SHULKER_BG_RADIUS; }
    public float shulkerPreviewBgSoftness() { return SHULKER_BG_SOFTNESS; }
    public int shulkerPreviewSlotBgRgb() { return SHULKER_SLOT_BG & 0x00FFFFFF; }
    public int shulkerPreviewSlotBorderRgb() { return SHULKER_SLOT_BORDER & 0x00FFFFFF; }
    public int shulkerPreviewSlotBorderAlpha() { return (SHULKER_SLOT_BORDER >>> 24) & 0xFF; }
    public float shulkerPreviewSlotRadius() { return SHULKER_SLOT_RADIUS; }
    public float shulkerPreviewSlotSoftness() { return SHULKER_SLOT_SOFTNESS; }
    public float shulkerPreviewSlotSize() { return shulkerPreviewSlotSize.get(); }
    public float shulkerPreviewSlotGap() { return SHULKER_SLOT_GAP; }
    public float shulkerPreviewPadding() { return SHULKER_PADDING; }

    private boolean anyTooltipsEnabled() { if (useCustomGuiTooltips()) return true; if (itemTooltipEnabled.get()) return true; return shulkerPreviewEnabled.get(); }
    private String bindingName(KeyBindValue v) { return name() + ":" + v.getName(); }
    private void registerBind(KeyBindValue v) {
        String n = bindingName(v); KeyManager.unregisterAll(n);
        if (!v.isNone()) KeyManager.registerCombo(n, v.get());
    }

    private enum SettingsGroup {
        TOOLTIP_GUI(GROUP_TOOLTIP_GUI), TOOLTIP_ITEMS(GROUP_TOOLTIP_ITEMS), SHULKER_PREVIEW(GROUP_SHULKER_PREVIEW);
        private final String id;
        SettingsGroup(String id) { this.id = id; }
        static SettingsGroup fromId(String id) {
            if (id == null) return null;
            for (SettingsGroup g : values()) if (g.id.equals(id)) return g;
            return null;
        }
    }
}
