/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonHoverState;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state.ButtonParallax;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import static dev.loki.lovisual.features.theme.Theme.theme;
/**
 * Стилизованный рендер кнопки: DEFAULT / GRADIENT / CUSTOM.
 */
public final class ButtonStyleRenderer {
    private static final float BUTTON_RADIUS = 6.0f;
    private static final float BUTTON_SOFTNESS = 1.2f;
    private ButtonStyleRenderer() {
    }
    public static void renderButton(GuiGraphicsExtractor ctx, AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused) {
        renderInternal(ctx, widget, text, hover, enabled, focused, ViewportContext.getScaleFactor(), true, true);
    }
    public static void renderButtonUnscaled(GuiGraphicsExtractor ctx, AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused) {
        renderInternal(ctx, widget, text, hover, enabled, focused, 1.0f, false, true);
    }
    public static void renderInternal(GuiGraphicsExtractor ctx, AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused, float scale, boolean manageProjection, boolean allowParallax) {
        if (ctx == null || widget == null) return;
        float x = widget.getX();
        float y = widget.getY();
        float w = widget.getWidth();
        float h = widget.getHeight();
        BetterButtons cfg = BetterButtons.get();
        BetterButtons.Style mode = cfg.getStyle().get();
        float radiusBase = BUTTON_RADIUS;
        float softness = 0.0f;
        float strokeSoft = 0.0f;
        if (mode == BetterButtons.Style.GRADIENT) {
            softness = BUTTON_SOFTNESS;
            strokeSoft = BUTTON_SOFTNESS;
        } else if (mode == BetterButtons.Style.CUSTOM) {
            radiusBase = cfg.getCustomRadius().get();
            softness = cfg.getCustomSoftness().get();
            strokeSoft = cfg.getCustomSoftness().get();
        }
        float radius = Math.min(radiusBase, h * 0.5f);
        ButtonSprites.markRounded(widget, radius);
        float alpha = widget.getAlpha();
        float hoverEase = ButtonHoverState.smoothHover(hover);
        float xb = x * scale;
        float yb = y * scale;
        float wb = w * scale;
        float hb = h * scale;
        float rb = radius * scale;
        float softnessPx = softness * scale;
        float strokeSoftPx = strokeSoft * scale;
        ButtonParallax.Data parallax = allowParallax ? ButtonParallax.compute(widget, hover) : ButtonParallax.Data.NONE;
        if (manageProjection) {
            ViewportContext.beginUnscaled(null);
        }
        boolean batching = Renderer2D.isBatching();
        if (!batching) {
            Renderer2D.COLOR.begin();
        }
        try (RenderWarpStack.Scope ignored = ButtonParallax.pushWarp(parallax, xb, yb, wb, hb)) {
            if (mode == BetterButtons.Style.DEFAULT) {
                renderDefault(xb, yb, wb, hb, rb, alpha, hoverEase, enabled, focused, scale);
            } else if (mode == BetterButtons.Style.GRADIENT) {
                renderGradient(cfg, xb, yb, wb, hb, rb, softnessPx, strokeSoftPx, alpha, hoverEase, enabled, focused, scale);
            } else {
                renderCustom(cfg, xb, yb, wb, hb, rb, softnessPx, strokeSoftPx, alpha, hoverEase, enabled, focused, scale);
            }
        }
        if (!batching) {
            Renderer2D.COLOR.render();
        }
        if (manageProjection) {
            ViewportContext.end(null);
        }
        if (text != null && !text.getString().isEmpty()) {
            try (RenderWarpStack.Scope ignored = ButtonParallax.pushWarp(parallax, x, y, w, h)) {
                ButtonTextRenderer.renderCentered(text, x, y, w, h, alpha, enabled, hover, !parallax.active());
            }
        }
    }
    private static void renderDefault(float xb, float yb, float wb, float hb, float rb, float alpha, float hoverEase, boolean enabled, boolean focused, float scale) {
        int baseBg = enabled ? 0xFF1B1B1B : 0xFF141414;
        int hoverBg = enabled ? 0xFF2A2A2A : 0xFF141414;
        int accent = theme().accent();
        int darkBase = HudRenderUtil.mixColor(baseBg, 0xFF000000, 0.45f);
        float hoverMix = enabled ? (0.08f + 0.22f * hoverEase) : 0.0f;
        int bgBase = HudRenderUtil.mixColor(darkBase, hoverBg, hoverMix);
        bgBase = HudRenderUtil.mixColor(bgBase, accent, 0.18f * hoverEase);
        int strokeBase = HudRenderUtil.mixColor(bgBase, 0xFFFFFFFF, 0.18f);
        strokeBase = HudRenderUtil.mixColor(strokeBase, accent, 0.16f * hoverEase + (focused ? 0.06f : 0.0f));
        int bg = HudRenderUtil.scaleAlpha(bgBase, alpha);
        int stroke = HudRenderUtil.scaleAlpha(strokeBase, 0.82f * alpha);
        Renderer2D.COLOR.roundedRect(xb, yb, wb, hb, rb, 0.0f, bg);
        int gradLight = HudRenderUtil.mixColor(bg, 0xFFFFFFFF, 0.36f + 0.10f * hoverEase);
        int gradMid = HudRenderUtil.mixColor(bg, 0xFF000000, 0.03f);
        int gradDark = HudRenderUtil.mixColor(bg, 0xFF000000, 0.20f + 0.10f * hoverEase);
        if (hoverEase > 0.001f) {
            int accentTop = HudRenderUtil.mixColor(theme().accent(), bg, 0.65f);
            int accentBottom = HudRenderUtil.mixColor(theme().accentSoft(), bg, 0.75f);
            float t = Math.min(1.0f, hoverEase * 0.6f);
            gradLight = HudRenderUtil.mixColor(gradLight, accentTop, t);
            gradDark = HudRenderUtil.mixColor(gradDark, accentBottom, t * 0.85f);
        }
        Renderer2D.COLOR.roundedRectGradientQuad(xb, yb, wb, hb, rb, 0.0f, gradLight, gradMid, gradDark, gradMid);
        float strokeThickness = 0.7f * scale;
        int strokeInner = HudRenderUtil.scaleAlpha(strokeBase, 0.9f * alpha);
        int strokeTop = HudRenderUtil.mixColor(strokeInner, 0xFFFFFFFF, 0.2f);
        int strokeBottom = HudRenderUtil.mixColor(strokeInner, 0xFF000000, 0.25f);
        Renderer2D.COLOR.roundedRectStrokeGradient(xb, yb, wb, hb, rb, 0.0f, strokeThickness, strokeTop, strokeBottom, 90f);
        float halo = (focused ? 0.45f : 0.25f) * hoverEase;
        if (enabled && halo > 0.01f) {
            int haloColor = HudRenderUtil.scaleAlpha(strokeBase, 0.35f * halo * alpha);
            Renderer2D.COLOR.roundedRectStroke(xb, yb, wb, hb, rb, 0.0f, strokeThickness + 1.1f * scale, haloColor);
        }
    }
    private static void renderGradient(BetterButtons cfg, float xb, float yb, float wb, float hb, float rb, float softnessPx, float strokeSoftPx, float alpha, float hoverEase, boolean enabled, boolean focused, float scale) {
        int base = cfg.getGradientBaseColor().getArgb();
        int grad = cfg.getGradientColor().getArgb();
        float intensity = clamp01(cfg.getGradientIntensity().get());
        float mix = clamp01(intensity + (enabled ? hoverEase * 0.2f : 0.0f));

        int start = HudRenderUtil.mixColor(base, grad, mix);
        int end = HudRenderUtil.mixColor(base, grad, intensity * 0.2f);
        if (!enabled) {
            start = HudRenderUtil.mixColor(start, 0xFF000000, 0.2f);
            end = HudRenderUtil.mixColor(end, 0xFF000000, 0.2f);
        }

        int strokeBase = HudRenderUtil.mixColor(base, 0xFFFFFFFF, 0.18f);
        strokeBase = HudRenderUtil.mixColor(strokeBase, grad, 0.12f * hoverEase);
        if (!enabled) {
            strokeBase = HudRenderUtil.mixColor(strokeBase, 0xFF000000, 0.25f);
        }

        int bgStart = HudRenderUtil.scaleAlpha(start, alpha);
        int bgEnd = HudRenderUtil.scaleAlpha(end, alpha);
        int stroke = HudRenderUtil.scaleAlpha(strokeBase, 0.85f * alpha);

        Renderer2D.COLOR.roundedRectGradient(xb, yb, wb, hb, rb, softnessPx, bgStart, bgEnd, cfg.getGradientAngle().get());

        float strokeThickness = 0.7f * scale;
        Renderer2D.COLOR.roundedRectStroke(xb, yb, wb, hb, rb, strokeSoftPx, strokeThickness, stroke);

        float halo = (focused ? 0.45f : 0.25f) * hoverEase;
        if (enabled && halo > 0.01f) {
            int haloColor = HudRenderUtil.scaleAlpha(strokeBase, 0.35f * halo * alpha);
            Renderer2D.COLOR.roundedRectStroke(xb, yb, wb, hb, rb, strokeSoftPx, strokeThickness + 1.1f * scale, haloColor);
        }
    }

    private static void renderCustom(BetterButtons cfg, float xb, float yb, float wb, float hb, float rb, float softnessPx, float strokeSoftPx, float alpha, float hoverEase, boolean enabled, boolean focused, float scale) {
        int base = cfg.getCustomBg().getArgb();
        int hoverBg = cfg.getCustomBgHover().getArgb();
        int disabledBg = cfg.getCustomBgDisabled().getArgb();
        int bg = enabled ? HudRenderUtil.mixColor(base, hoverBg, hoverEase) : disabledBg;

        int strokeBase = enabled
                ? HudRenderUtil.mixColor(cfg.getCustomStroke().getArgb(), cfg.getCustomStrokeHover().getArgb(), hoverEase)
                : cfg.getCustomStrokeDisabled().getArgb();

        float strokeThickness = cfg.getCustomStrokeThickness().get() * scale;

        if (cfg.getCustomGradient().get()) {
            float t = clamp01(cfg.getCustomGradientIntensity().get());
            int grad = cfg.getCustomGradientColor().getArgb();
            int start = HudRenderUtil.mixColor(bg, grad, t);
            int end = HudRenderUtil.mixColor(bg, grad, t * 0.2f);
            int bgStart = HudRenderUtil.scaleAlpha(start, alpha);
            int bgEnd = HudRenderUtil.scaleAlpha(end, alpha);
            Renderer2D.COLOR.roundedRectGradient(xb, yb, wb, hb, rb, softnessPx, bgStart, bgEnd, cfg.getCustomGradientAngle().get());
        } else {
            int fill = HudRenderUtil.scaleAlpha(bg, alpha);
            Renderer2D.COLOR.roundedRect(xb, yb, wb, hb, rb, softnessPx, fill);
        }

        int stroke = HudRenderUtil.scaleAlpha(strokeBase, 0.85f * alpha);
        Renderer2D.COLOR.roundedRectStroke(xb, yb, wb, hb, rb, strokeSoftPx, strokeThickness, stroke);

        float halo = (focused ? 0.45f : 0.25f) * hoverEase;
        if (enabled && halo > 0.01f) {
            int haloColor = HudRenderUtil.scaleAlpha(strokeBase, 0.35f * halo * alpha);
            Renderer2D.COLOR.roundedRectStroke(xb, yb, wb, hb, rb, strokeSoftPx, strokeThickness + 1.1f * scale, haloColor);
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
