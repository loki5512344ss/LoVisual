package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.rect;

import static dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeContext.UiShapeFrame;
import static dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeProps.first;

/**
 * Rect/rounded-rect fill and gradient families of the legacy JS shape contract.
 */
public final class UiRectPainter {
    private UiRectPainter() {
    }

    public static boolean paint(UiShapeFrame f) {
        return switch (f.shape()) {
            case "rect", "quad" -> paintQuad(f);
            case "gradient", "rect-gradient", "rect_gradient", "quad-gradient", "quad_gradient" -> paintQuadGradient(f);
            case "rounded", "rounded-rect", "rounded_rect" -> paintRounded(f);
            case "rounded-gradient", "rounded_gradient", "rounded-rect-gradient", "rounded_rect_gradient" -> paintRoundedGradient(f);
            case "rounded-progress-gradient", "rounded_progress_gradient", "progress-rounded-gradient",
                 "progress_rounded_gradient" -> paintProgress(f);
            case "rounded-smoke-fill", "rounded_smoke_fill", "smoke-fill", "smoke_fill" -> paintSmoke(f);
            default -> false;
        };
    }

    private static boolean paintQuad(UiShapeFrame f) {
        if ((f.fill() >>> 24) > 0) {
            f.renderer().quad(f.x(), f.y(), f.w(), f.h(), f.fill());
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            f.renderer().roundedRectStroke(f.x(), f.y(), f.w(), f.h(), 0.0f, f.strokeWidth(), f.stroke());
        }
        return true;
    }

    private static boolean paintQuadGradient(UiShapeFrame f) {
        int start = f.color(f.props().get("startColor"), f.fill());
        int end = f.color(f.props().get("endColor"), f.fill());
        if (f.hasFillCornerColors()) {
            f.renderer().quadGradient(f.x(), f.y(), f.w(), f.h(),
                    f.color(first(f.props(), "topLeftColor", "cTopLeft"), start),
                    f.color(first(f.props(), "topRightColor", "cTopRight"), end),
                    f.color(first(f.props(), "bottomRightColor", "cBottomRight"), end),
                    f.color(first(f.props(), "bottomLeftColor", "cBottomLeft"), start));
        } else if (((start | end) >>> 24) > 0) {
            f.renderer().quadGradientLinear(f.x(), f.y(), f.w(), f.h(), start, end,
                    f.props().number("angle", 0.0f), f.props().number("offset", 0.0f));
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            if (f.hasStrokeCornerColors()) {
                f.renderer().roundedRectStrokeGradientQuad(f.x(), f.y(), f.w(), f.h(), 0.0f, 0.0f, f.strokeWidth(),
                        f.color(f.props().get("strokeTopLeftColor"), f.stroke()),
                        f.color(f.props().get("strokeTopRightColor"), f.stroke()),
                        f.color(f.props().get("strokeBottomRightColor"), f.stroke()),
                        f.color(f.props().get("strokeBottomLeftColor"), f.stroke()));
            } else {
                f.renderer().quadStrokeGradientLinear(f.x(), f.y(), f.w(), f.h(), f.strokeWidth(),
                        f.color(f.props().get("strokeStartColor"), f.stroke()),
                        f.color(f.props().get("strokeEndColor"), f.stroke()),
                        f.props().number("strokeAngle", f.props().number("angle", 0.0f)),
                        f.props().number("strokeOffset", f.props().number("offset", 0.0f)));
            }
        }
        return true;
    }

    private static boolean paintRounded(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        if ((f.fill() >>> 24) > 0) {
            f.renderer().roundedRect(f.x(), f.y(), f.w(), f.h(), radius, f.fill());
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            f.renderer().roundedRectStroke(f.x(), f.y(), f.w(), f.h(), radius, f.strokeWidth(), f.stroke());
        }
        return true;
    }

    private static boolean paintRoundedGradient(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        int start = f.color(f.props().get("startColor"), f.fill());
        int end = f.color(f.props().get("endColor"), f.fill());
        if (f.hasFillCornerColors()) {
            f.renderer().roundedRectGradientQuad(f.x(), f.y(), f.w(), f.h(), radius,
                    f.color(first(f.props(), "topLeftColor", "cTopLeft"), start),
                    f.color(first(f.props(), "topRightColor", "cTopRight"), end),
                    f.color(first(f.props(), "bottomRightColor", "cBottomRight"), end),
                    f.color(first(f.props(), "bottomLeftColor", "cBottomLeft"), start));
        } else if (((start | end) >>> 24) > 0) {
            f.renderer().roundedRectGradient(f.x(), f.y(), f.w(), f.h(), radius, start, end,
                    f.props().number("angle", 90.0f), f.props().number("offset", 0.0f));
        }
        if ((f.stroke() >>> 24) > 0 && f.strokeWidth() > 0.0f) {
            if (f.hasStrokeCornerColors()) {
                f.renderer().roundedRectStrokeGradientQuad(f.x(), f.y(), f.w(), f.h(), radius, f.strokeWidth(),
                        f.color(f.props().get("strokeTopLeftColor"), f.stroke()),
                        f.color(f.props().get("strokeTopRightColor"), f.stroke()),
                        f.color(f.props().get("strokeBottomRightColor"), f.stroke()),
                        f.color(f.props().get("strokeBottomLeftColor"), f.stroke()));
            } else {
                f.renderer().roundedRectStrokeGradient(f.x(), f.y(), f.w(), f.h(), radius, f.strokeWidth(),
                        f.color(f.props().get("strokeStartColor"), f.stroke()),
                        f.color(f.props().get("strokeEndColor"), f.stroke()),
                        f.props().number("strokeAngle", f.props().number("angle", 90.0f)),
                        f.props().number("strokeOffset", f.props().number("offset", 0.0f)));
            }
        }
        return true;
    }

    private static boolean paintProgress(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float progress = f.props().number("progress", 1.0f);
        boolean fromRight = f.props().bool("fromRight", false) || "right".equals(f.props().string("direction", ""));
        int start = f.color(f.props().get("startColor"), f.fill());
        int end = f.color(f.props().get("endColor"), start);
        if (((start | end) >>> 24) > 0) {
            f.renderer().roundedProgressRectGradient(f.x(), f.y(), f.w(), f.h(), radius, progress, fromRight,
                    start, end, f.props().number("angle", 0.0f), f.props().number("offset", 0.0f));
        }
        return true;
    }

    private static boolean paintSmoke(UiShapeFrame f) {
        float radius = f.props().number("radius", f.style().radius());
        float fillRatio = f.props().number("fillRatio", f.props().number("progress", 1.0f));
        boolean fromRight = f.props().bool("fromRight", false) || "right".equals(f.props().string("direction", ""));
        int first = f.color(first(f.props(), "firstColor", "startColor"), f.fill());
        int second = f.color(first(f.props(), "secondColor", "endColor"), first);
        int third = f.color(f.props().get("thirdColor"), second);
        if (((first | second | third) >>> 24) > 0) {
            f.renderer().roundedSmokeFill(f.x(), f.y(), f.w(), f.h(), radius, fillRatio, fromRight,
                    first, second, third,
                    f.props().number("time", 0.0f),
                    f.props().number("smokeScale", 3.0f),
                    f.props().number("smokeMix", 0.72f),
                    Math.round(f.props().number("octaves", 4.0f)),
                    f.props().number("flowX", 0.08f),
                    f.props().number("flowY", -0.05f),
                    f.props().number("intensity", 1.0f));
        }
        return true;
    }
}
