/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.rect;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeContext.UiShapeFrame;

import static dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeProps.first;

/**
 * Per-corner radius shapes and the chamfer/bevel/notch fallback family.
 */
public final class UiCornerPainter {
    private UiCornerPainter() {
    }

    public static boolean paint(UiShapeFrame f) {
        if (!f.shape().equals("rounded-corners") && !f.shape().equals("rounded_corners")
                && !f.shape().equals("rounded-rect-corners") && !f.shape().equals("rounded_rect_corners")) {
            return false;
        }
        paintCorners(f);
        return true;
    }

    private static void paintCorners(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float radiusTL = f.props().number("radiusTL", radius);
        float radiusTR = f.props().number("radiusTR", radius);
        float radiusBR = f.props().number("radiusBR", radius);
        float radiusBL = f.props().number("radiusBL", radius);
        int[] cornerColors = new int[4];
        if (f.hasFillCornerColors()) {
            fillCorners(f, radiusTL, radiusTR, radiusBR, radiusBL);
        } else if (f.linearGradient()) {
            UiShapeContext.computeLinearGradientCornerColors((float) f.w(), (float) f.h(), f.gradStart(), f.gradEnd(),
                    f.gradAngle(), f.gradOffset(), cornerColors);
            f.renderer().roundedRectCornersQuad(f.x(), f.y(), f.w(), f.h(), radiusTL, radiusTR, radiusBR, radiusBL,
                    cornerColors[0], cornerColors[1], cornerColors[2], cornerColors[3]);
        } else if ((f.fill() >>> 24) > 0) {
            f.renderer().roundedRectCorners(f.x(), f.y(), f.w(), f.h(), radiusTL, radiusTR, radiusBR, radiusBL, f.fill());
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            f.renderer().roundedRectStrokeCorners(f.x(), f.y(), f.w(), f.h(), radiusTL, radiusTR, radiusBR, radiusBL,
                    f.strokeWidth(), f.stroke());
        }
    }

    private static void fillCorners(UiShapeFrame f, float radiusTL, float radiusTR, float radiusBR, float radiusBL) {
        f.renderer().roundedRectCornersQuad(f.x(), f.y(), f.w(), f.h(), radiusTL, radiusTR, radiusBR, radiusBL,
                f.color(first(f.props(), "topLeftColor", "cTopLeft"), f.gradStart()),
                f.color(first(f.props(), "topRightColor", "cTopRight"), f.gradEnd()),
                f.color(first(f.props(), "bottomRightColor", "cBottomRight"), f.gradEnd()),
                f.color(first(f.props(), "bottomLeftColor", "cBottomLeft"), f.gradStart()));
    }

    /**
     * Legacy fallback path: beveled/notched/chamfered fills plus optional gradient stroke.
     */
    public static void paintFallback(UiShapeFrame f) {
        if ((f.fill() >>> 24) > 0 || (f.linearGradient() && ((f.gradStart() | f.gradEnd()) >>> 24) > 0)) {
            paintFallbackFill(f);
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            paintFallbackStroke(f);
        }
    }

    private static void paintFallbackFill(UiShapeFrame f) {
        switch (f.shape()) {
            case "beveled", "bevel" -> {
                if (f.linearGradient()) {
                    f.renderer().beveledRectGradient(f.x(), f.y(), f.w(), f.h(),
                            f.props().number("bevel", (float) f.cut()), f.gradStart(), f.gradEnd(),
                            f.color(f.props().get("highlight"), UiShapeContext.lighten(f.gradStart(), 0.20f)),
                            f.color(f.props().get("shadow"), UiShapeContext.darken(f.gradEnd(), 0.24f)),
                            f.gradAngle(), f.gradOffset());
                } else {
                    f.renderer().beveledRect(f.x(), f.y(), f.w(), f.h(),
                            f.props().number("bevel", (float) f.cut()), f.fill(),
                            f.color(f.props().get("highlight"), UiShapeContext.lighten(f.fill(), 0.20f)),
                            f.color(f.props().get("shadow"), UiShapeContext.darken(f.fill(), 0.24f)));
                }
            }
            case "notched", "notch" -> {
                double notchWidth = f.props().number("notchWidth", (float) Math.min(f.w() * 0.18, 18.0));
                double notchDepth = f.props().number("notchDepth", (float) Math.min(f.h() * 0.28, 8.0));
                if (f.linearGradient()) {
                    f.renderer().notchedRectGradient(f.x(), f.y(), f.w(), f.h(), notchWidth, notchDepth,
                            f.gradStart(), f.gradEnd(), f.gradAngle(), f.gradOffset());
                } else {
                    f.renderer().notchedRect(f.x(), f.y(), f.w(), f.h(), notchWidth, notchDepth, f.fill());
                }
            }
            default -> {
                if (f.linearGradient()) {
                    f.renderer().chamferedRectGradient(f.x(), f.y(), f.w(), f.h(),
                            f.cutTL(), f.cutTR(), f.cutBR(), f.cutBL(),
                            f.gradStart(), f.gradEnd(), f.gradAngle(), f.gradOffset());
                } else {
                    f.renderer().chamferedRect(f.x(), f.y(), f.w(), f.h(), f.cutTL(), f.cutTR(), f.cutBR(), f.cutBL(), f.fill());
                }
            }
        }
    }

    private static void paintFallbackStroke(UiShapeFrame f) {
        int strokeStart = f.color(f.props().get("strokeStartColor"), f.stroke());
        int strokeEnd = f.color(f.props().get("strokeEndColor"), f.stroke());
        float strokeAngle = f.props().number("strokeAngle", f.gradAngle());
        float strokeOffset = f.props().number("strokeOffset", f.gradOffset());
        boolean strokeGradient = f.hasStrokeLinearGradient() || f.linearGradient();
        switch (f.shape()) {
            case "beveled", "bevel" -> {
                if (strokeGradient) {
                    f.renderer().beveledRectStrokeGradient(f.x(), f.y(), f.w(), f.h(),
                            f.props().number("bevel", (float) f.cut()), f.strokeWidth(), strokeStart, strokeEnd, strokeAngle, strokeOffset);
                } else {
                    f.renderer().beveledRectStroke(f.x(), f.y(), f.w(), f.h(),
                            f.props().number("bevel", (float) f.cut()), f.strokeWidth(), f.stroke());
                }
            }
            case "notched", "notch" -> {
                double notchWidth = f.props().number("notchWidth", (float) Math.min(f.w() * 0.18, 18.0));
                double notchDepth = f.props().number("notchDepth", (float) Math.min(f.h() * 0.28, 8.0));
                if (strokeGradient) {
                    f.renderer().notchedRectStrokeGradient(f.x(), f.y(), f.w(), f.h(), notchWidth, notchDepth, f.strokeWidth(),
                            strokeStart, strokeEnd, strokeAngle, strokeOffset);
                } else {
                    f.renderer().notchedRectStroke(f.x(), f.y(), f.w(), f.h(), notchWidth, notchDepth, f.strokeWidth(), f.stroke());
                }
            }
            default -> {
                if (strokeGradient) {
                    f.renderer().chamferedRectStrokeGradient(f.x(), f.y(), f.w(), f.h(),
                            f.cutTL(), f.cutTR(), f.cutBR(), f.cutBL(),
                            f.strokeWidth(), strokeStart, strokeEnd, strokeAngle, strokeOffset);
                } else {
                    f.renderer().chamferedRectStroke(f.x(), f.y(), f.w(), f.h(), f.cutTL(), f.cutTR(), f.cutBR(), f.cutBL(),
                            f.strokeWidth(), f.stroke());
                }
            }
        }
    }
}
