package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonSliderRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonSprites;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonStyleRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonTextRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonHoverState;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonParallax;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonQueue;
import dev.loki.lovisual.mixins.accessors.hud.TextIconButtonWidgetAccessor;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

@HudElementRegister(order = 10)
public final class BetterButtons extends AbstractHudElement {

    public static final BetterButtons INSTANCE = new BetterButtons();

    private final EnumValue<Style> style =
            new EnumValue<>("style", Style.DEFAULT, Style.values());
    private final RGBAColorValue gradientBaseColor =
            new RGBAColorValue("gradient_base_color", "#FF2F2929");
    private final RGBAColorValue gradientColor =
            new RGBAColorValue("gradient_color", "#FF902F2F");
    private final NumberValue<Float> gradientAngle =
            new NumberValue<>("gradient_angle", 74.25f, 0.0f, 360.0f);
    private final NumberValue<Float> gradientIntensity =
            new NumberValue<>("gradient_intensity", 0.6f, 0.0f, 1.0f);
    private final RGBAColorValue customBg =
            new RGBAColorValue("custom_bg", "#FF1B1B1B");
    private final RGBAColorValue customBgHover =
            new RGBAColorValue("custom_bg_hover", "#FF2A2A2A");
    private final RGBAColorValue customBgDisabled =
            new RGBAColorValue("custom_bg_disabled", "#FF141414");
    private final RGBAColorValue customStroke =
            new RGBAColorValue("custom_stroke", "#FF9B5DB8");
    private final RGBAColorValue customStrokeHover =
            new RGBAColorValue("custom_stroke_hover", "#FFA710B2");
    private final RGBAColorValue customStrokeDisabled =
            new RGBAColorValue("custom_stroke_disabled", "#FF2A2A2A");
    private final RGBAColorValue customText =
            new RGBAColorValue("custom_text", "#FFECECEC");
    private final RGBAColorValue customTextDisabled =
            new RGBAColorValue("custom_text_disabled", "#FF7A7A7A");
    private final NumberValue<Float> customRadius =
            new NumberValue<>("custom_radius", 6.0f, 0.0f, 20.0f);
    private final NumberValue<Float> customSoftness =
            new NumberValue<>("custom_softness", 1.2f, 0.0f, 4.0f);
    private final NumberValue<Float> customStrokeThickness =
            new NumberValue<>("custom_stroke_thickness", 0.7f, 0.1f, 3.0f);
    private final BooleanValue customGradient =
            new BooleanValue("custom_gradient", true);
    private final RGBAColorValue customGradientColor =
            new RGBAColorValue("custom_gradient_color", "#73521BA7");
    private final NumberValue<Float> customGradientAngle =
            new NumberValue<>("custom_gradient_angle", 90.0f, 0.0f, 360.0f);
    private final NumberValue<Float> customGradientIntensity =
            new NumberValue<>("custom_gradient_intensity", 0.5f, 0.0f, 1.0f);
    private final NumberValue<Float> customTextScale =
            new NumberValue<>("custom_text_scale", 1.0f, 0.6f, 1.6f);
    private final NumberValue<Float> customTextPadX =
            new NumberValue<>("custom_text_pad_x", 6.0f, 0.0f, 16.0f);

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

    public static GuiGraphicsExtractor getLastContext() {
        return ButtonQueue.lastContext();
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

    static void renderButtonInternal(GuiGraphicsExtractor ctx, AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused, float scale, boolean manageProjection, boolean allowParallax) {
        ButtonStyleRenderer.renderInternal(ctx, widget, text, hover, enabled, focused, scale, manageProjection, allowParallax);
    }

    public static void renderSlider(GuiGraphicsExtractor ctx, AbstractWidget widget, float value, boolean dragging, float hover) {
        ButtonSliderRenderer.render(ctx, widget, value, dragging, hover);
    }

    public static void renderTextIconButton(GuiGraphicsExtractor ctx, SpriteIconButton widget, boolean iconOnly, float hover, boolean enabled, boolean focused) {
        if (ctx == null || widget == null) return;
        renderButton(ctx, widget, null, hover, enabled, focused);

        TextIconButtonWidgetAccessor accessor = (TextIconButtonWidgetAccessor) widget;
        int texW = accessor.lovisual$getTextureWidth();
        int texH = accessor.lovisual$getTextureHeight();
        int iconX;
        int iconY = widget.getY() + widget.getHeight() / 2 - texH / 2;
        if (iconOnly) {
            iconX = widget.getX() + widget.getWidth() / 2 - texW / 2;
        } else {
            iconX = widget.getX() + widget.getWidth() - texW - 2;
        }
        boolean hoverState = widget.isActive() && (widget.isHoveredOrFocused() || focused || hover > 0.001f);
        Identifier spriteId = accessor.lovisual$getTexture().get(widget.isActive(), hoverState);
        float sx = ButtonParallax.shiftX(widget, hover);
        float sy = ButtonParallax.shiftY(widget, hover);
        ButtonSprites.drawGuiSprite(spriteId, Math.round(iconX + sx), Math.round(iconY + sy), texW, texH, widget.getAlpha());

        if (!iconOnly) {
            float left = widget.getX() + 2f;
            float right = widget.getX() + widget.getWidth() - texW - 4f;
            float top = widget.getY();
            float bottom = widget.getY() + widget.getHeight();
            Component text = widget.getMessage();
            ButtonParallax.Data parallax = ButtonParallax.compute(widget, hover);
            try (RenderWarpStack.Scope ignored = ButtonParallax.pushWarp(parallax, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight())) {
                ButtonTextRenderer.renderLeftAligned(text, left, top, right, bottom, widget.getAlpha(), enabled, hover, !parallax.active());
            }
        }
    }

    public static void renderCyclingButton(GuiGraphicsExtractor ctx, CycleButton<?> widget, Identifier icon, boolean showLabel, float hover, boolean enabled, boolean focused) {
        if (ctx == null || widget == null) return;
        renderButton(ctx, widget, null, hover, enabled, focused);

        if (icon != null) {
            int size = Math.min(widget.getWidth(), widget.getHeight()) - 6;
            float sx = ButtonParallax.shiftX(widget, hover);
            float sy = ButtonParallax.shiftY(widget, hover);
            int ix = Math.round(widget.getX() + (widget.getWidth() - size) / 2 + sx);
            int iy = Math.round(widget.getY() + (widget.getHeight() - size) / 2 + sy);
            ctx.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, ix, iy, size, size, widget.getAlpha());
        }

        if (showLabel) {
            Component text = widget.getMessage();
            ButtonParallax.Data parallax = ButtonParallax.compute(widget, hover);
            try (RenderWarpStack.Scope ignored = ButtonParallax.pushWarp(parallax, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight())) {
                ButtonTextRenderer.renderCentered(text, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                        widget.getAlpha(), enabled, hover, !parallax.active());
            }
        }
    }

    public static void renderCenteredText(Component text, float x, float y, float w, float h, float alpha, boolean enabled, float hover) {
        ButtonTextRenderer.renderCentered(text, x, y, w, h, alpha, enabled, hover);
    }

    public static void renderLeftAlignedText(Component text, float x0, float y0, float x1, float y1, float alpha, boolean enabled, float hover) {
        ButtonTextRenderer.renderLeftAligned(text, x0, y0, x1, y1, alpha, enabled, hover);
    }

    // --- getters for render helpers ---

    public EnumValue<Style> getStyle() {
        return style;
    }

    public RGBAColorValue getGradientBaseColor() {
        return gradientBaseColor;
    }

    public RGBAColorValue getGradientColor() {
        return gradientColor;
    }

    public NumberValue<Float> getGradientAngle() {
        return gradientAngle;
    }

    public NumberValue<Float> getGradientIntensity() {
        return gradientIntensity;
    }

    public RGBAColorValue getCustomBg() {
        return customBg;
    }

    public RGBAColorValue getCustomBgHover() {
        return customBgHover;
    }

    public RGBAColorValue getCustomBgDisabled() {
        return customBgDisabled;
    }

    public RGBAColorValue getCustomStroke() {
        return customStroke;
    }

    public RGBAColorValue getCustomStrokeHover() {
        return customStrokeHover;
    }

    public RGBAColorValue getCustomStrokeDisabled() {
        return customStrokeDisabled;
    }

    public RGBAColorValue getCustomText() {
        return customText;
    }

    public RGBAColorValue getCustomTextDisabled() {
        return customTextDisabled;
    }

    public NumberValue<Float> getCustomRadius() {
        return customRadius;
    }

    public NumberValue<Float> getCustomSoftness() {
        return customSoftness;
    }

    public NumberValue<Float> getCustomStrokeThickness() {
        return customStrokeThickness;
    }

    public BooleanValue getCustomGradient() {
        return customGradient;
    }

    public RGBAColorValue getCustomGradientColor() {
        return customGradientColor;
    }

    public NumberValue<Float> getCustomGradientAngle() {
        return customGradientAngle;
    }

    public NumberValue<Float> getCustomGradientIntensity() {
        return customGradientIntensity;
    }

    public NumberValue<Float> getCustomTextScale() {
        return customTextScale;
    }

    public NumberValue<Float> getCustomTextPadX() {
        return customTextPadX;
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.mode(style));
        defs.add(SettingDef.color(gradientBaseColor).visibleWhen(this::isGradientStyle));
        defs.add(SettingDef.color(gradientColor).visibleWhen(this::isGradientStyle));
        defs.add(SettingDef.number(gradientAngle).visibleWhen(this::isGradientStyle));
        defs.add(SettingDef.number(gradientIntensity).visibleWhen(this::isGradientStyle));

        defs.add(SettingDef.color(customBg).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customBgHover).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customBgDisabled).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customStroke).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customStrokeHover).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customStrokeDisabled).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customText).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customTextDisabled).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customRadius).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customSoftness).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customStrokeThickness).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.bool(customGradient).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.color(customGradientColor).visibleWhen(this::isCustomGradientStyle));
        defs.add(SettingDef.number(customGradientAngle).visibleWhen(this::isCustomGradientStyle));
        defs.add(SettingDef.number(customGradientIntensity).visibleWhen(this::isCustomGradientStyle));
        defs.add(SettingDef.number(customTextScale).visibleWhen(this::isCustomStyle));
        defs.add(SettingDef.number(customTextPadX).visibleWhen(this::isCustomStyle));
    }

    public boolean useUiButtons() {
        return isEnabled();
    }

    private boolean isGradientStyle() {
        return style.get() == Style.GRADIENT;
    }

    private boolean isCustomStyle() {
        return style.get() == Style.CUSTOM;
    }

    private boolean isCustomGradientStyle() {
        return isCustomStyle() && customGradient.get();
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
