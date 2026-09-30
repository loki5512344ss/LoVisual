package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonSprites;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render.ButtonTextRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonParallax;
import dev.loki.lovisual.mixins.accessors.hud.TextIconButtonWidgetAccessor;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Icon placement for text-icon and cycling buttons: sprites plus optional
 * label, all offset by the shared parallax shift.
 */
final class ButtonIconRenderer {

    private ButtonIconRenderer() {
    }

    static void renderTextIconButton(GuiGraphicsExtractor ctx,
                                     SpriteIconButton widget,
                                     boolean iconOnly,
                                     float hover,
                                     boolean enabled,
                                     boolean focused) {
        if (ctx == null || widget == null) return;
        BetterButtons.renderButton(ctx, widget, null, hover, enabled, focused);

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

    static void renderCyclingButton(GuiGraphicsExtractor ctx,
                                    CycleButton<?> widget,
                                    Identifier icon,
                                    boolean showLabel,
                                    float hover,
                                    boolean enabled,
                                    boolean focused) {
        if (ctx == null || widget == null) return;
        BetterButtons.renderButton(ctx, widget, null, hover, enabled, focused);

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
}
