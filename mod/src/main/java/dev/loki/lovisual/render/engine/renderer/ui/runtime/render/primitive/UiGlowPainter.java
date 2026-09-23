/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;

import static dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeContext.UiShapeFrame;

/**
 * Glow/shadow family and circle/arc primitives of the legacy JS shape contract.
 */
public final class UiGlowPainter {
    private UiGlowPainter() {
    }

    public static boolean paint(UiShapeFrame f) {
        return switch (f.shape()) {
            case "rounded-soft-shadow", "rounded_soft_shadow", "soft-shadow", "soft_shadow" -> paintSoftShadow(f);
            case "rounded-shadow", "rounded_shadow", "shadow" -> paintShadow(f);
            case "rounded-glow", "rounded_glow", "glow" -> paintGlow(f);
            case "radial-glow-masked", "radial_glow_masked", "radial-glow", "radial_glow" -> paintRadialGlow(f);
            case "circle-soft-shadow", "circle_soft_shadow" -> paintCircleSoftShadow(f);
            case "circle" -> paintCircle(f);
            case "circle-stroke", "circle_stroke", "ring" -> paintRing(f);
            case "arc", "arc-stroke", "arc_stroke", "arc-flat", "arc_flat", "arc-gradient", "arc_gradient" -> paintArc(f);
            default -> false;
        };
    }

    private static boolean paintSoftShadow(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float blur = f.props().number("blur", f.props().number("shadowBlur", 8.0f));
        float innerAlpha = f.props().number("innerAlpha", f.props().number("shadowInnerAlpha", 0.18f));
        int color = f.color(f.props().get("color"), f.fill());
        if ((color >>> 24) > 0 && blur > 0.0f) {
            f.renderer().roundedRectSoftShadow(f.x(), f.y(), f.w(), f.h(), radius, blur, innerAlpha, color);
        }
        return true;
    }

    private static boolean paintShadow(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float softness = f.props().number("softness", 8.0f);
        float spread = f.props().number("spread", 12.0f);
        int color = f.color(f.props().get("color"), f.fill());
        if ((color >>> 24) > 0) {
            f.renderer().roundedRectShadow(f.x(), f.y(), f.w(), f.h(), radius, softness, spread, color);
        }
        return true;
    }

    private static boolean paintGlow(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float softness = f.props().number("softness", 0.0f);
        float glow = f.props().number("glow", f.props().number("spread", 8.0f));
        int color = f.color(f.props().get("color"), f.fill());
        if ((color >>> 24) > 0 && glow > 0.0f) {
            f.renderer().roundedRectGlow(f.x(), f.y(), f.w(), f.h(), radius, softness, glow, color);
        }
        return true;
    }

    private static boolean paintRadialGlow(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float softness = f.props().number("softness", 0.0f);
        float glowRadius = f.props().number("glowRadius", f.props().number("glow", (float) Math.max(f.w(), f.h()) * 0.5f));
        float cx = f.props().number("cx", (float) (f.w() * 0.5));
        float cy = f.props().number("cy", (float) (f.h() * 0.5));
        int color = f.color(f.props().get("color"), f.fill());
        if ((color >>> 24) > 0 && glowRadius > 0.0f) {
            f.renderer().radialGlowMasked(f.x(), f.y(), f.w(), f.h(), radius, softness, glowRadius,
                    (float) f.x() + cx, (float) f.y() + cy, color);
        }
        return true;
    }

    private static boolean paintCircleSoftShadow(UiShapeFrame f) {
        double radius = f.props().number("radius", (float) (Math.min(f.w(), f.h()) * 0.5));
        double cx = f.x() + f.props().number("cx", (float) (f.w() * 0.5));
        double cy = f.y() + f.props().number("cy", (float) (f.h() * 0.5));
        float blur = f.props().number("blur", f.props().number("shadowBlur", 7.0f));
        float innerAlpha = f.props().number("innerAlpha", f.props().number("shadowInnerAlpha", 0.34f));
        int color = f.color(f.props().get("color"), f.fill());
        if ((color >>> 24) > 0 && radius > 0.0 && blur > 0.0f) {
            f.renderer().circleSoftShadow(cx, cy, radius, blur, innerAlpha, color);
        }
        return true;
    }

    private static boolean paintCircle(UiShapeFrame f) {
        double radius = f.props().number("radius", (float) (Math.min(f.w(), f.h()) * 0.5));
        double cx = f.x() + f.props().number("cx", (float) (f.w() * 0.5));
        double cy = f.y() + f.props().number("cy", (float) (f.h() * 0.5));
        if ((f.fill() >>> 24) > 0) {
            f.renderer().circle(cx, cy, radius, f.fill());
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            f.renderer().circleStroke(cx, cy, radius, f.strokeWidth(), f.stroke());
        }
        return true;
    }

    private static boolean paintRing(UiShapeFrame f) {
        double radius = f.props().number("radius", (float) (Math.min(f.w(), f.h()) * 0.5));
        double cx = f.x() + f.props().number("cx", (float) (f.w() * 0.5));
        double cy = f.y() + f.props().number("cy", (float) (f.h() * 0.5));
        float thickness = f.props().number("thickness", Math.max(1.0f, f.strokeWidth()));
        int color = (f.stroke() >>> 24) > 0 ? f.stroke() : f.fill();
        if ((color >>> 24) > 0 && thickness > 0.0f) {
            f.renderer().circleStroke(cx, cy, radius, thickness, color);
        }
        return true;
    }

    private static boolean paintArc(UiShapeFrame f) {
        Renderer2D renderer = f.renderer();
        float thickness = f.props().number("thickness", Math.max(1.0f, f.strokeWidth()));
        if (thickness <= 0.0f) return true;
        double radius = f.props().number("radius", (float) Math.max(0.0, Math.min(f.w(), f.h()) * 0.5 - thickness * 0.5));
        double cx = f.x() + f.props().number("cx", (float) (f.w() * 0.5));
        double cy = f.y() + f.props().number("cy", (float) (f.h() * 0.5));
        float startAngle = f.props().number("startAngle", 0.0f);
        float endAngle = f.props().number("endAngle", 360.0f);
        int color = (f.stroke() >>> 24) > 0 ? f.stroke() : f.fill();
        if (f.shape().equals("arc-gradient") || f.shape().equals("arc_gradient")) {
            int start = f.color(f.props().get("startColor"), color);
            int end = f.color(f.props().get("endColor"), color);
            if (((start | end) >>> 24) > 0) {
                renderer.arcStrokeGradient(cx, cy, radius, thickness, startAngle, endAngle,
                        f.props().number("softness", 0.0f),
                        start, end, f.props().number("angle", 0.0f), f.props().number("offset", 0.0f));
            }
        } else if ((color >>> 24) > 0) {
            if (f.shape().equals("arc-flat") || f.shape().equals("arc_flat")) {
                renderer.arcStrokeFlat(cx, cy, radius, thickness, startAngle, endAngle, color);
            } else {
                renderer.arcStroke(cx, cy, radius, thickness, startAngle, endAngle, color);
            }
        }
        return true;
    }
}
