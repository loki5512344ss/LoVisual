package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonHoverState;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Рендер слайдера: фон через ButtonStyleRenderer + трек/заливка.
 */
public final class ButtonSliderRenderer {
    private ButtonSliderRenderer() {
    }

    public static void render(GuiGraphicsExtractor ctx, AbstractWidget widget, float value, boolean dragging, float hover) {
        if (ctx == null || widget == null) return;

        float x = widget.getX();
        float y = widget.getY();
        float w = widget.getWidth();
        float h = widget.getHeight();
        float scale = ViewportContext.getScaleFactor();

        float alpha = widget.getAlpha();
        float radius = Math.min(4.0f, h * 0.5f);
        ButtonSprites.markRounded(widget, radius);

        ButtonStyleRenderer.renderInternal(ctx, widget, null, hover, widget.isActive(), widget.isFocused(), scale, true, false);

        float trackInset = 8f;
        float trackX = x + trackInset;
        float trackW = Math.max(12f, w - trackInset * 2f);
        float trackH = Math.max(4f, h * 0.24f);
        float trackY = y + (h - trackH) * 0.5f;
        float fillW = trackW * value;

        float hoverEase = ButtonHoverState.smoothHover(hover);
        int track = HudRenderUtil.mixColor(ButtonSprites.forceOpaque(theme().surface()), 0xFF000000, 0.65f);
        int fill = HudRenderUtil.mixColor(ButtonSprites.forceOpaque(theme().accentSoft()), ButtonSprites.forceOpaque(theme().accent()), 0.25f + 0.35f * hoverEase);
        int stroke = HudRenderUtil.mixColor(ButtonSprites.forceOpaque(theme().strokeSoft()), 0xFF000000, 0.55f);
        stroke = HudRenderUtil.mixColor(stroke, theme().accent(), 0.08f * hoverEase);

        track = HudRenderUtil.scaleAlpha(track, alpha);
        fill = HudRenderUtil.scaleAlpha(fill, alpha);
        stroke = HudRenderUtil.scaleAlpha(stroke, 0.6f * alpha);

        float trackXb = trackX * scale;
        float trackYb = trackY * scale;
        float trackWb = trackW * scale;
        float trackHb = trackH * scale;
        float fillWb = fillW * scale;

        ViewportContext.beginUnscaled(null);
        boolean batching = Renderer2D.isBatching();
        if (!batching) {
            Renderer2D.COLOR.begin();
        }
        float barRadius = Math.min(2.5f * scale, trackHb * 0.5f);
        Renderer2D.COLOR.roundedRect(trackXb, trackYb, trackWb, trackHb, barRadius, scale, track);
        if (fillW > 1f) {
            Renderer2D.COLOR.roundedRect(trackXb, trackYb, fillWb, trackHb, barRadius, scale, fill);
        }
        Renderer2D.COLOR.roundedRectStroke(trackXb, trackYb, trackWb, trackHb, barRadius, scale, scale, stroke);
        if (!batching) {
            Renderer2D.COLOR.render();
        }
        ViewportContext.end(null);

        Component text = widget.getMessage();
        ButtonTextRenderer.renderCentered(text, x, y, w, h, alpha, widget.isActive(), hover);
    }
}
