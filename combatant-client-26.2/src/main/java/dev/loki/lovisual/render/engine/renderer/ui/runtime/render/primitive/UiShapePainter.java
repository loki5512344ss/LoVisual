/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive;

import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiBoxShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiEdgeSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiStroke;
import dev.loki.lovisual.render.engine.renderer.ui.draw.paint.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeContext.UiShapeFrame;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.rect.UiCornerPainter;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.rect.UiRectPainter;

/**
 * Orchestrates the legacy JS shape contract: blur pre-pass, flexible-box path,
 * then glow/rect/corner painter families with a chamfer fallback.
 */
public final class UiShapePainter {
    private UiShapePainter() {
    }

    public static void paint(UiNode node, UiRenderContext context) {
        if (node == null || context == null || context.renderer() == null) return;
        if (node.bounds().width() <= 0.0f || node.bounds().height() <= 0.0f) return;
        UiShapeFrame f = UiShapeFrame.of(node, context);
        renderShapeBlur(f);
        if (UiShapeProps.isBoxShape(f.shape(), f.props())) {
            paintBoxShape(f);
            return;
        }
        if (UiGlowPainter.paint(f)) return;
        if (UiRectPainter.paint(f)) return;
        if (UiCornerPainter.paint(f)) return;
        UiCornerPainter.paintFallback(f);
    }

    private static void paintBoxShape(UiShapeFrame f) {
        UiBoxShape boxShape = buildBoxShape(f);
        UiPaint fillPaint = buildPaint(f);
        if ((fillPaint.solidColor() >>> 24) > 0 || f.linearGradient() || f.hasFillCornerColors()) {
            f.renderer().box(boxShape, fillPaint);
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            f.renderer().boxStroke(boxShape, buildStrokePaint(f), UiStroke.of(f.strokeWidth()));
        }
    }

    private static UiBoxShape buildBoxShape(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float cut = f.props().number("cut", f.props().number("chamfer", f.style().radius()));
        UiCornerSpec defaultCorner = switch (f.shape()) {
            case "rounded", "rounded-rect", "rounded_rect", "rounded-gradient", "rounded_gradient",
                 "rounded-rect-gradient", "rounded_rect_gradient" -> UiCornerSpec.rounded(radius, radius);
            case "chamfered", "beveled", "bevel", "cut", "cut-corner", "cut_corner" -> UiCornerSpec.chamfered(cut);
            default -> UiCornerSpec.square();
        };
        if (f.shape().equals("rounded-corners") || f.shape().equals("rounded_corners")
                || f.shape().equals("rounded-rect-corners") || f.shape().equals("rounded_rect_corners")) {
            defaultCorner = UiCornerSpec.rounded(radius, radius);
        }
        UiCornerSpec tl = UiShapeProps.corner(f.props(), "TL", "TopLeft", defaultCorner, radius, cut);
        UiCornerSpec tr = UiShapeProps.corner(f.props(), "TR", "TopRight", defaultCorner, radius, cut);
        UiCornerSpec br = UiShapeProps.corner(f.props(), "BR", "BottomRight", defaultCorner, radius, cut);
        UiCornerSpec bl = UiShapeProps.corner(f.props(), "BL", "BottomLeft", defaultCorner, radius, cut);

        Object edges = f.props().get("edges");
        UiEdgeSpec top = UiShapeProps.edgeFromObject(UiShapeProps.edgeValue(edges, "top"), f.w(), true);
        UiEdgeSpec right = UiShapeProps.edgeFromObject(UiShapeProps.edgeValue(edges, "right"), f.h(), false);
        UiEdgeSpec bottom = UiShapeProps.edgeFromObject(UiShapeProps.edgeValue(edges, "bottom"), f.w(), true);
        UiEdgeSpec left = UiShapeProps.edgeFromObject(UiShapeProps.edgeValue(edges, "left"), f.h(), false);
        if (f.shape().equals("notched") || f.shape().equals("notch")) {
            top = UiEdgeSpec.notchedCenter(f.props().number("notchWidth", (float) Math.min(f.w() * 0.18, 18.0)),
                    f.props().number("notchDepth", (float) Math.min(f.h() * 0.28, 8.0)));
        }
        if (f.props().get("edgeTop") != null || f.props().get("topEdge") != null)
            top = UiShapeProps.edgeFromObject(UiShapeProps.first(f.props(), "edgeTop", "topEdge"), f.w(), true);
        if (f.props().get("edgeRight") != null || f.props().get("rightEdge") != null)
            right = UiShapeProps.edgeFromObject(UiShapeProps.first(f.props(), "edgeRight", "rightEdge"), f.h(), false);
        if (f.props().get("edgeBottom") != null || f.props().get("bottomEdge") != null)
            bottom = UiShapeProps.edgeFromObject(UiShapeProps.first(f.props(), "edgeBottom", "bottomEdge"), f.w(), true);
        if (f.props().get("edgeLeft") != null || f.props().get("leftEdge") != null)
            left = UiShapeProps.edgeFromObject(UiShapeProps.first(f.props(), "edgeLeft", "leftEdge"), f.h(), false);
        return UiBoxShape.rect(f.x(), f.y(), f.w(), f.h()).corners(tl, tr, br, bl)
                .edges(top, right, bottom, left).build();
    }

    private static UiPaint buildPaint(UiShapeFrame f) {
        if (f.hasFillCornerColors()) {
            return UiPaint.corners(
                    f.color(UiShapeProps.first(f.props(), "topLeftColor", "cTopLeft"), f.gradStart()),
                    f.color(UiShapeProps.first(f.props(), "topRightColor", "cTopRight"), f.gradEnd()),
                    f.color(UiShapeProps.first(f.props(), "bottomRightColor", "cBottomRight"), f.gradEnd()),
                    f.color(UiShapeProps.first(f.props(), "bottomLeftColor", "cBottomLeft"), f.gradStart()));
        }
        if (f.linearGradient()) return UiPaint.linear(f.gradStart(), f.gradEnd(), f.gradAngle(), f.gradOffset());
        return UiPaint.solid(f.fill());
    }

    private static UiPaint buildStrokePaint(UiShapeFrame f) {
        if (f.hasStrokeCornerColors()) {
            return UiPaint.corners(
                    f.color(f.props().get("strokeTopLeftColor"), f.stroke()),
                    f.color(f.props().get("strokeTopRightColor"), f.stroke()),
                    f.color(f.props().get("strokeBottomRightColor"), f.stroke()),
                    f.color(f.props().get("strokeBottomLeftColor"), f.stroke()));
        }
        if (f.hasStrokeLinearGradient() || f.linearGradient()) {
            return UiPaint.linear(
                    f.color(f.props().get("strokeStartColor"), f.color(f.props().get("startColor"), f.stroke())),
                    f.color(f.props().get("strokeEndColor"), f.color(f.props().get("endColor"), f.stroke())),
                    f.props().number("strokeAngle", f.props().number("angle", f.gradAngle())),
                    f.props().number("strokeOffset", f.props().number("offset", f.gradOffset())));
        }
        return UiPaint.solid(f.stroke());
    }

    private static void renderShapeBlur(UiShapeFrame f) {
        if (!f.props().bool("blur", false)) return;
        float quality = f.props().number("blurQuality", f.style().blurQuality());
        float brightness = f.props().number("blurBrightness", f.style().blurBrightness());
        float alpha = f.props().number("blurAlpha", f.style().blurAlpha()) * f.alpha();
        if (alpha <= 0.001f) return;
        switch (f.shape()) {
            case "rounded", "round" -> f.renderer().blurRect(f.x(), f.y(), f.w(), f.h(),
                    f.props().number("radius", f.style().radius()), quality, brightness, alpha, 0xFFFFFF);
            case "rounded-corners", "rounded_corners" -> {
                float radius = f.props().number("radius", f.style().radius());
                float radiusTL = f.props().number("radiusTL", radius);
                float radiusTR = f.props().number("radiusTR", radius);
                float radiusBR = f.props().number("radiusBR", radius);
                float radiusBL = f.props().number("radiusBL", radius);
                f.renderer().blurComposite(blur -> blur.roundedRectCorners(f.x(), f.y(), f.w(), f.h(),
                        radiusTL, radiusTR, radiusBR, radiusBL, quality, brightness, alpha, 0xFFFFFF));
            }
            default -> f.renderer().blurChamferedRect(f.x(), f.y(), f.w(), f.h(), f.cut(), quality, brightness, alpha, 0xFFFFFF);
        }
    }
}
