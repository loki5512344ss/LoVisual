/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.UiStyle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.theme.UiColor;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.theme.UiThemeRegistry;

/**
 * Resolved shape frame shared by primitive painters plus small color math helpers.
 */
public final class UiShapeContext {
    private UiShapeContext() {
    }

    public static int color(Object value, int fallback, float alpha) {
        int resolved;
        if (value instanceof Number n) {
            resolved = n.intValue();
        } else if (value instanceof String s) {
            int themed = UiThemeRegistry.current().color(s, Integer.MIN_VALUE);
            resolved = themed != Integer.MIN_VALUE ? themed : UiColor.parse(s, fallback);
        } else {
            resolved = fallback;
        }
        return alpha >= 0.999f ? resolved : UiColor.multiplyAlpha(resolved, alpha);
    }

    public static int lighten(int argb, float amount) {
        return adjust(argb, Math.abs(amount));
    }

    public static int darken(int argb, float amount) {
        return adjust(argb, -Math.abs(amount));
    }

    private static int adjust(int argb, float delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = channel((argb >>> 16) & 0xFF, delta);
        int g = channel((argb >>> 8) & 0xFF, delta);
        int b = channel(argb & 0xFF, delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int value, float delta) {
        int out = delta >= 0.0f
                ? value + Math.round((255 - value) * delta)
                : value - Math.round(value * -delta);
        return Math.max(0, Math.min(255, out));
    }

    public static void computeLinearGradientCornerColors(float width, float height, int startArgb, int endArgb,
                                                         float angleDeg, float offsetPx, int[] out) {
        float angle = (float) Math.toRadians(angleDeg);
        float dirX = (float) Math.cos(angle);
        float dirY = (float) Math.sin(angle);
        float p0 = 0.0f;
        float p1 = width * dirX;
        float p2 = height * dirY;
        float p3 = width * dirX + height * dirY;
        float minProj = Math.min(Math.min(p0, p1), Math.min(p2, p3));
        float maxProj = Math.max(Math.max(p0, p1), Math.max(p2, p3));
        float range = Math.max(0.0001f, maxProj - minProj);
        out[0] = mixArgb(startArgb, endArgb, clamp01((p0 + offsetPx - minProj) / range));
        out[1] = mixArgb(startArgb, endArgb, clamp01((p1 + offsetPx - minProj) / range));
        out[2] = mixArgb(startArgb, endArgb, clamp01((p3 + offsetPx - minProj) / range));
        out[3] = mixArgb(startArgb, endArgb, clamp01((p2 + offsetPx - minProj) / range));
    }

    public static int mixArgb(int a, int b, float t) {
        float u = 1.0f - t;
        int aa = Math.round(((a >>> 24) & 0xFF) * u + ((b >>> 24) & 0xFF) * t);
        int rr = Math.round(((a >>> 16) & 0xFF) * u + ((b >>> 16) & 0xFF) * t);
        int gg = Math.round(((a >>> 8) & 0xFF) * u + ((b >>> 8) & 0xFF) * t);
        int bb = Math.round((a & 0xFF) * u + (b & 0xFF) * t);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    public static float clamp01(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }

    /**
     * Immutable snapshot of everything a shape painter needs for one draw call.
     */
    public record UiShapeFrame(Renderer2D renderer, UiProps props, UiStyle style, String shape,
                               double x, double y, double w, double h,
                               int fill, int stroke, float strokeWidth,
                               double cut, double cutTL, double cutTR, double cutBR, double cutBL,
                               boolean linearGradient, int gradStart, int gradEnd,
                               float gradAngle, float gradOffset, float alpha) {

        public static UiShapeFrame of(UiNode node, UiRenderContext context) {
            UiBounds bounds = node.bounds();
            UiProps props = node.props();
            UiStyle style = node.style();
            String shape = props.string("shape", "chamfered").toLowerCase(java.util.Locale.ROOT);
            float alpha = context.alpha();
            int fill = UiShapeContext.color(props.get("fill"), style.backgroundColor() != null ? style.backgroundColor() : 0x00000000, alpha);
            int stroke = UiShapeContext.color(props.get("stroke"), style.strokeColor() != null ? style.strokeColor() : 0x00000000, alpha);
            float strokeWidth = props.number("strokeWidth", style.strokeWidth());
            double x = bounds.x();
            double y = bounds.y();
            double w = Math.max(0.0, props.number("renderWidth", bounds.width()));
            double h = Math.max(0.0, props.number("renderHeight", bounds.height()));
            double cut = props.number("cut", props.number("chamfer", style.radius()));
            double cutTL = UiShapeProps.chamferCorner(props, "TL", "TopLeft", (float) cut);
            double cutTR = UiShapeProps.chamferCorner(props, "TR", "TopRight", (float) cut);
            double cutBR = UiShapeProps.chamferCorner(props, "BR", "BottomRight", (float) cut);
            double cutBL = UiShapeProps.chamferCorner(props, "BL", "BottomLeft", (float) cut);
            boolean linearGradient = props.get("startColor") != null || props.get("endColor") != null;
            int gradStart = UiShapeContext.color(props.get("startColor"), fill, alpha);
            int gradEnd = UiShapeContext.color(props.get("endColor"), fill, alpha);
            float gradAngle = props.number("angle", 90.0f);
            float gradOffset = props.number("offset", 0.0f);
            return new UiShapeFrame(context.renderer(), props, style, shape, x, y, w, h,
                    fill, stroke, strokeWidth, cut, cutTL, cutTR, cutBR, cutBL,
                    linearGradient, gradStart, gradEnd, gradAngle, gradOffset, alpha);
        }

        public boolean hasFillCornerColors() {
            return UiShapeProps.first(props, "topLeftColor", "cTopLeft") != null
                    || UiShapeProps.first(props, "topRightColor", "cTopRight") != null
                    || UiShapeProps.first(props, "bottomRightColor", "cBottomRight") != null
                    || UiShapeProps.first(props, "bottomLeftColor", "cBottomLeft") != null;
        }

        public boolean hasStrokeCornerColors() {
            return props.get("strokeTopLeftColor") != null || props.get("strokeTopRightColor") != null
                    || props.get("strokeBottomRightColor") != null || props.get("strokeBottomLeftColor") != null;
        }

        public boolean hasStrokeLinearGradient() {
            return props.get("strokeStartColor") != null || props.get("strokeEndColor") != null;
        }

        public int color(Object value, int fallback) {
            return UiShapeContext.color(value, fallback, alpha);
        }
    }
}
