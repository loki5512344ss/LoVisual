package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonSliderRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonStyleRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonHoverState;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonQueue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Buttons HUD facade: settings values in {@link ButtonStyleSettings}, icon
 * drawing in {@link ButtonIconRenderer}, queue/render primitives behind it.
 */
@HudElementRegister(order = 10)
public final class BetterButtons extends AbstractHudElement {

    public static final BetterButtons INSTANCE = new BetterButtons();

    private final ButtonStyleSettings settings = new ButtonStyleSettings();

    private BetterButtons() {
        super("vanilla_buttons", "Buttons", true);
    }

    public static BetterButtons get() {
        return INSTANCE;
    }

    // --- frame / queue delegation ---

    public static void beginFrame(GuiGraphicsExtractor ctx) {
        ButtonQueue.beginFrame(ctx);
    }


    public static void pushScissorContext(int x1, int y1, int x2, int y2) {
        ButtonQueue.pushScissorContext(x1, y1, x2, y2);
    }

    public static void popScissorContext() {
        ButtonQueue.popScissorContext();
    }

    public static void flush() {
        ButtonQueue.flush();
    }

    public static void captureTooltip(AbstractWidget widget, int mouseX, int mouseY) {
        ButtonQueue.captureTooltip(widget, mouseX, mouseY);
    }

    public static boolean hasPending() {
        return ButtonQueue.hasPending();
    }

    public static boolean hasTooltip() {
        return ButtonQueue.hasTooltip();
    }

    public static void enqueueButton(AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused) {
        ButtonQueue.enqueueButton(widget, text, hover, enabled, focused);
    }

    public static void enqueueSlider(AbstractWidget widget, float value, boolean dragging, float hover) {
        ButtonQueue.enqueueSlider(widget, value, dragging, hover);
    }

    public static void enqueueTextIconButton(SpriteIconButton widget, boolean iconOnly, float hover, boolean enabled, boolean focused) {
        ButtonQueue.enqueueTextIconButton(widget, iconOnly, hover, enabled, focused);
    }

    public static void enqueueCyclingButton(CycleButton<?> widget, Identifier icon, boolean showLabel, float hover, boolean enabled, boolean focused) {
        ButtonQueue.enqueueCyclingButton(widget, icon, showLabel, hover, enabled, focused);
    }

    public static void renderTooltip() {
        ButtonQueue.renderTooltip();
    }

    // --- hover / parallax ---

    public static float updateHover(AbstractWidget widget, boolean hovered) {
        return ButtonHoverState.updateHover(widget, hovered);
    }

    public static float updateHover(AbstractWidget widget, boolean hovered, float mouseX, float mouseY) {
        return ButtonHoverState.updateHover(widget, hovered, mouseX, mouseY);
    }

    // --- button rendering ---

    public static void renderButton(GuiGraphicsExtractor ctx, AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused) {
        ButtonStyleRenderer.renderButton(ctx, widget, text, hover, enabled, focused);
    }

    public static void renderButtonUnscaled(GuiGraphicsExtractor ctx, AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused) {
        ButtonStyleRenderer.renderButtonUnscaled(ctx, widget, text, hover, enabled, focused);
    }

    public static void renderSlider(GuiGraphicsExtractor ctx, AbstractWidget widget, float value, boolean dragging, float hover) {
        ButtonSliderRenderer.render(ctx, widget, value, dragging, hover);
    }

    public static void renderTextIconButton(GuiGraphicsExtractor ctx, SpriteIconButton widget, boolean iconOnly, float hover, boolean enabled, boolean focused) {
        ButtonIconRenderer.renderTextIconButton(ctx, widget, iconOnly, hover, enabled, focused);
    }

    public static void renderCyclingButton(GuiGraphicsExtractor ctx, CycleButton<?> widget, Identifier icon, boolean showLabel, float hover, boolean enabled, boolean focused) {
        ButtonIconRenderer.renderCyclingButton(ctx, widget, icon, showLabel, hover, enabled, focused);
    }


    // --- getters for render helpers ---

    public EnumValue<Style> getStyle() { return settings.style; }

    public RGBAColorValue getGradientBaseColor() { return settings.gradientBaseColor; }

    public RGBAColorValue getGradientColor() { return settings.gradientColor; }

    public NumberValue<Float> getGradientAngle() { return settings.gradientAngle; }

    public NumberValue<Float> getGradientIntensity() { return settings.gradientIntensity; }

    public RGBAColorValue getCustomBg() { return settings.customBg; }

    public RGBAColorValue getCustomBgHover() { return settings.customBgHover; }

    public RGBAColorValue getCustomBgDisabled() { return settings.customBgDisabled; }

    public RGBAColorValue getCustomStroke() { return settings.customStroke; }

    public RGBAColorValue getCustomStrokeHover() { return settings.customStrokeHover; }

    public RGBAColorValue getCustomStrokeDisabled() { return settings.customStrokeDisabled; }

    public RGBAColorValue getCustomText() { return settings.customText; }

    public RGBAColorValue getCustomTextDisabled() { return settings.customTextDisabled; }

    public NumberValue<Float> getCustomRadius() { return settings.customRadius; }

    public NumberValue<Float> getCustomSoftness() { return settings.customSoftness; }

    public NumberValue<Float> getCustomStrokeThickness() { return settings.customStrokeThickness; }

    public BooleanValue getCustomGradient() { return settings.customGradient; }

    public RGBAColorValue getCustomGradientColor() { return settings.customGradientColor; }

    public NumberValue<Float> getCustomGradientAngle() { return settings.customGradientAngle; }

    public NumberValue<Float> getCustomGradientIntensity() { return settings.customGradientIntensity; }

    public NumberValue<Float> getCustomTextScale() { return settings.customTextScale; }

    public NumberValue<Float> getCustomTextPadX() { return settings.customTextPadX; }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        settings.defineSettings(defs);
    }

    public boolean useUiButtons() {
        return isEnabled();
    }

    public enum Style implements EnumValue.IdProvider {
        DEFAULT("Default"),
        GRADIENT("Gradient"),
        CUSTOM("Custom");

        private final String id;

        Style(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
