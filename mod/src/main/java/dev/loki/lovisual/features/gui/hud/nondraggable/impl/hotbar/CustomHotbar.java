package dev.loki.lovisual.features.gui.hud.nondraggable.impl.hotbar;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.math.ColorMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Module facade for the vanilla-replacement hotbar HUD element. Owns the ClickGui
 * settings, resolves the effective palette (theme vs. custom colours) and delegates
 * drawing to {@link HotbarRenderer} and per-frame animation to {@link HotbarSelection}.
 * Only the tiny surface used by {@code GuiMixin} is public — every palette getter
 * is package-private.
 */
@HudElementRegister(order = 30)
public final class CustomHotbar extends AbstractHudElement {

    /** Sentinel values for the {@code bg_effect} / {@code hotbar_color_mode} mode-settings. */
    private static final String COLOR_THEME = "Theme";
    private static final String COLOR_CUSTOM = "Custom";

    public static final CustomHotbar INSTANCE = new CustomHotbar();

    private final EnumValue<OffHandMode> offHandMode = new EnumValue<>("offhand_mode", OffHandMode.MERGED, OffHandMode.values());
    private final ModeValue colorMode = new ModeValue("hotbar_color_mode", "Custom", COLOR_THEME, COLOR_CUSTOM);
    private final RGBAColorValue bgColor = new RGBAColorValue("bg_color", "#7A191414");
    private final NumberValue<Integer> bgAlpha = new NumberValue<>("hotbar_bg_alpha", 167, 0, 255);
    private final RGBColorValue selectColor = new RGBColorValue("select_color", "#2F6277");
    private final RGBColorValue dividerColor = new RGBColorValue("divider_color", "#C13434");
    private final BooleanValue customSelectColor = new BooleanValue("custom_select_color", false);
    private final BooleanValue customDividerColor = new BooleanValue("custom_divider_color", false);
    private final NumberValue<Integer> accentAlpha = new NumberValue<>("accent_alpha", 255, 0, 255);
    private final ModeValue bgEffect = new ModeValue("bg_effect", HudBgStyles.AUTO, HudBgStyles.NONE, HudBgStyles.BLUR, HudBgStyles.GLASS, HudBgStyles.AUTO);
    private final NumberValue<Integer> blurAlpha = new NumberValue<>("blur_alpha", 255, 20, 255);

    private CustomHotbar() {
        super("vanilla_hotbar", "Hotbar", true);
    }

    /** Singleton accessor used by {@code GuiMixin}. */
    public static CustomHotbar get() {
        return INSTANCE;
    }

    /** Public entry point that draws panel backdrop + selection box. */
    public static void renderBackground(Renderer2D r2d, Minecraft mc) {
        HotbarRenderer.renderBackground(r2d, mc);
    }

    /** Public entry point that draws every item icon in the hotbar row. */
    public static void renderItems(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        HotbarRenderer.renderItems(ctx, tickCounter, mc);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.mode(offHandMode));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.color(bgColor).visibleWhen(() -> isCustomMode() && !isGlassEffect()));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(() -> isThemeMode() && !isGlassEffect()));
        defs.add(SettingDef.bool(customSelectColor));
        defs.add(SettingDef.colorNoAlpha(selectColor).visibleWhen(customSelectColor::get));
        defs.add(SettingDef.bool(customDividerColor));
        defs.add(SettingDef.colorNoAlpha(dividerColor).visibleWhen(customDividerColor::get));
        defs.add(SettingDef.number(accentAlpha));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
    }

    public boolean isHudHotbarEnabled() {
        return isEnabled();
    }

    OffHandMode getHotbarOffHandMode() {
        return offHandMode.get();
    }

    int getHotbarBgArgb() {
        return ColorMath.premultiplyAlpha(getHotbarBgRgb(), getHotbarBgAlpha());
    }

    int getHotbarBgRgb() {
        return isThemeMode() ? theme().windowBg() & 0x00FFFFFF : bgColor.getArgb() & 0x00FFFFFF;
    }

    int getHotbarBgAlpha() {
        return isThemeMode() ? bgAlpha.get() : (bgColor.getArgb() >>> 24) & 0xFF;
    }

    int getHotbarSelectRgb() {
        return customSelectColor.get() ? selectColor.getArgb() & 0x00FFFFFF : theme().accent() & 0x00FFFFFF;
    }

    int getHotbarSelectEndRgb() {
        if (customSelectColor.get()) return HudRenderUtil.mixColor(selectColor.getArgb(), 0xFFFFFFFF, 0.24f) & 0x00FFFFFF;
        return HudRenderUtil.mixColor(theme().accentSoft(), theme().textPrimary(), 0.18f) & 0x00FFFFFF;
    }

    int getHotbarDividerRgb() {
        if (customDividerColor.get()) return dividerColor.getArgb() & 0x00FFFFFF;
        return HudRenderUtil.mixColor(theme().windowStroke(), theme().textMuted(), 0.32f) & 0x00FFFFFF;
    }

    int getHotbarAccentAlpha() {
        return Math.max(0, Math.min(255, accentAlpha.get()));
    }

    boolean isHotbarBlurEnabled() {
        return isBlurEffect();
    }

    boolean isHotbarGlassEnabled() {
        return isGlassEffect();
    }

    int getHotbarBlurAlpha() {
        return blurAlpha.get();
    }

    boolean customSelectColorEnabled() {
        return customSelectColor.get();
    }

    boolean hasEffect() {
        return HudBgStyles.hasEffect(bgEffect.get());
    }

    private boolean isThemeMode() {
        return COLOR_THEME.equals(colorMode.get());
    }

    private boolean isCustomMode() {
        return COLOR_CUSTOM.equals(colorMode.get());
    }

    private boolean isBlurEffect() {
        return HudBgStyles.isBlur(bgEffect.get());
    }

    private boolean isGlassEffect() {
        return HudBgStyles.isGlass(bgEffect.get());
    }

    /** How the off-hand slot is composed with the main 9-slot row. */
    public enum OffHandMode implements EnumValue.IdProvider {
        MERGED("merged"),
        SEPARATELY("separately");

        private final String id;

        OffHandMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
