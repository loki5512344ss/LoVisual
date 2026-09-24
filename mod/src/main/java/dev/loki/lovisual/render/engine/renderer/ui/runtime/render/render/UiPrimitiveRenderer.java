package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.render;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapePainter;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.primitive.UiShapeProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.UiStyle;

import java.util.Locale;
import java.util.Map;

/**
 * Entry point for primitive shape and connector rendering; shape families live in
 * {@code render.primitive} / {@code render.rect}, connectors stay here.
 */
public final class UiPrimitiveRenderer {
    private final double[] points = new double[64];

    public void renderShape(UiNode node, UiRenderContext context) {
        UiShapePainter.paint(node, context);
    }

    public void renderConnector(UiNode node, UiRenderContext context) {
        if (node == null || context == null || context.renderer() == null) return;
        renderConnectorInternal(node, context);
    }

    private void renderConnectorInternal(UiNode node, UiRenderContext context) {
        UiBounds bounds = node.bounds();
        UiProps props = node.props();
        UiStyle style = node.style();
        Renderer2D renderer = context.renderer();
        float alpha = context.alpha();
        String type = props.string("connector", "line").toLowerCase(Locale.ROOT);
        int stroke = UiShapeContext.color(props.get("stroke"), style.strokeColor() != null ? style.strokeColor() : 0xFFFFFFFF, alpha);
        float thickness = props.number("strokeWidth", Math.max(1.0f, style.strokeWidth()));
        boolean gradient = props.get("startColor") != null || props.get("endColor") != null
                || props.get("strokeStartColor") != null || props.get("strokeEndColor") != null;
        int start = UiShapeContext.color(props.get("startColor"), UiShapeContext.color(props.get("strokeStartColor"), stroke, alpha), alpha);
        int end = UiShapeContext.color(props.get("endColor"), UiShapeContext.color(props.get("strokeEndColor"), stroke, alpha), alpha);
        double x1 = bounds.x() + props.number("x1", 0.0f);
        double y1 = bounds.y() + props.number("y1", bounds.height() * 0.5f);
        double x2 = bounds.x() + props.number("x2", bounds.width());
        double y2 = bounds.y() + props.number("y2", bounds.height() * 0.5f);

        switch (type) {
            case "rounded-edge", "rounded_edge", "rounded-line", "rounded_line" -> renderRoundedConnector(
                    renderer, bounds, props, thickness, stroke, gradient, start, end, RoundedConnectorMode.LINE);
            case "rounded-node-edge", "rounded_node_edge", "rounded-edge-bezier", "rounded_edge_bezier" -> renderRoundedConnector(
                    renderer, bounds, props, thickness, stroke, gradient, start, end, RoundedConnectorMode.NODE_EDGE);
            case "rounded-orthogonal", "rounded_orthogonal", "rounded-orthogonal-connector",
                 "rounded_orthogonal_connector" -> renderRoundedConnector(
                    renderer, bounds, props, thickness, stroke, gradient, start, end, RoundedConnectorMode.ORTHOGONAL);
            case "cable" -> renderCable(renderer, props, x1, y1, x2, y2, thickness, stroke, gradient, start, end, alpha);
            case "bezier", "bezier-connector", "bezier_connector" -> {
                double cx1 = bounds.x() + props.number("cx1", props.number("x1", 0.0f) + bounds.width() * 0.33f);
                double cy1 = bounds.y() + props.number("cy1", props.number("y1", bounds.height() * 0.5f));
                double cx2 = bounds.x() + props.number("cx2", props.number("x2", bounds.width()) - bounds.width() * 0.33f);
                double cy2 = bounds.y() + props.number("cy2", props.number("y2", bounds.height() * 0.5f));
                int segments = (int) props.number("segments", 20.0f);
                if (gradient) renderer.bezierConnectorGradient(x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments, thickness, start, end);
                else renderer.bezierConnector(x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments, thickness, stroke);
            }
            case "orthogonal", "orthogonal-connector", "orthogonal_connector" -> {
                double midX = bounds.x() + props.number("midX", bounds.width() * 0.5f);
                if (gradient) renderer.orthogonalConnectorGradient(x1, y1, x2, y2, midX, thickness, start, end);
                else renderer.orthogonalConnector(x1, y1, x2, y2, midX, thickness, stroke);
            }
            case "node-edge", "node_edge", "edge" -> {
                if (gradient) renderer.nodeGraphEdgeGradient(x1, y1, x2, y2, thickness, start, end);
                else renderer.nodeGraphEdge(x1, y1, x2, y2, thickness, stroke);
            }
            case "spline" -> {
                int count = readPoints(props.get("points"), bounds.x(), bounds.y());
                if (count >= 2) {
                    if (gradient) renderer.splineGradient(points, count, thickness, props.bool("closed", false), start, end);
                    else renderer.spline(points, count, thickness, props.bool("closed", false), stroke);
                }
            }
            case "wire" -> {
                if (gradient) renderer.wireGradient(x1, y1, x2, y2, thickness, start, end);
                else renderer.wire(x1, y1, x2, y2, thickness, stroke);
            }
            default -> {
                if (gradient) renderer.connectorGradient(x1, y1, x2, y2, thickness, start, end);
                else renderer.connector(x1, y1, x2, y2, thickness, stroke);
            }
        }
    }

    private static void renderCable(Renderer2D renderer, UiProps props, double x1, double y1, double x2, double y2,
                                    float thickness, int stroke, boolean gradient, int start, int end, float alpha) {
        if (gradient) {
            renderer.cableGradient(x1, y1, x2, y2, thickness,
                    UiShapeContext.color(props.get("outerStartColor"), UiShapeContext.darken(start, 0.35f), alpha),
                    UiShapeContext.color(props.get("outerEndColor"), UiShapeContext.darken(end, 0.35f), alpha),
                    UiShapeContext.color(props.get("innerStartColor"), start, alpha),
                    UiShapeContext.color(props.get("innerEndColor"), end, alpha));
        } else {
            renderer.cable(x1, y1, x2, y2, thickness,
                    UiShapeContext.color(props.get("outer"), UiShapeContext.darken(stroke, 0.35f), alpha),
                    UiShapeContext.color(props.get("inner"), stroke, alpha));
        }
    }

    private static void renderRoundedConnector(Renderer2D renderer, UiBounds bounds, UiProps props,
                                               double thickness, int stroke, boolean gradient,
                                               int start, int end, RoundedConnectorMode mode) {
        double sx = bounds.x() + props.number("sourceX", props.number("x1", 0.0f));
        double sy = bounds.y() + props.number("sourceY", props.number("y1", 0.0f));
        double sw = props.number("sourceWidth", props.number("sourceW", 1.0f));
        double sh = props.number("sourceHeight", props.number("sourceH", 1.0f));
        double sr = props.number("sourceRadius", 0.0f);
        double tx = bounds.x() + props.number("targetX", props.number("x2", bounds.width()));
        double ty = bounds.y() + props.number("targetY", props.number("y2", 0.0f));
        double tw = props.number("targetWidth", props.number("targetW", 1.0f));
        double th = props.number("targetHeight", props.number("targetH", 1.0f));
        double tr = props.number("targetRadius", 0.0f);

        switch (mode) {
            case NODE_EDGE -> {
                if (gradient)
                    renderer.roundedRectNodeGraphEdgeGradient(sx, sy, sw, sh, sr, tx, ty, tw, th, tr, thickness, start, end);
                else renderer.roundedRectNodeGraphEdge(sx, sy, sw, sh, sr, tx, ty, tw, th, tr, thickness, stroke);
            }
            case ORTHOGONAL -> {
                double midX = bounds.x() + props.number("midX", bounds.width() * 0.5f);
                if (gradient)
                    renderer.roundedRectOrthogonalConnectorGradient(sx, sy, sw, sh, sr, tx, ty, tw, th, tr, midX, thickness, start, end);
                else renderer.roundedRectOrthogonalConnector(sx, sy, sw, sh, sr, tx, ty, tw, th, tr, midX, thickness, stroke);
            }
            default -> {
                if (gradient)
                    renderer.roundedRectConnectorGradient(sx, sy, sw, sh, sr, tx, ty, tw, th, tr, thickness, start, end);
                else renderer.roundedRectConnector(sx, sy, sw, sh, sr, tx, ty, tw, th, tr, thickness, stroke);
            }
        }
    }

    private int readPoints(Object value, double offsetX, double offsetY) {
        int count = 0;
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (count >= points.length / 2) break;
                if (item instanceof Map<?, ?> map) {
                    points[count * 2] = offsetX + UiShapeProps.number(map.get("x"), 0.0f);
                    points[count * 2 + 1] = offsetY + UiShapeProps.number(map.get("y"), 0.0f);
                    count++;
                } else if (item instanceof Number) {
                    break;
                }
            }
            if (count == 0) {
                double pendingX = Double.NaN;
                for (Object item : iterable) {
                    if (!(item instanceof Number n)) continue;
                    if (Double.isNaN(pendingX)) {
                        pendingX = n.doubleValue();
                    } else {
                        if (count >= points.length / 2) break;
                        points[count * 2] = offsetX + pendingX;
                        points[count * 2 + 1] = offsetY + n.doubleValue();
                        count++;
                        pendingX = Double.NaN;
                    }
                }
            }
        }
        return count;
    }

    private enum RoundedConnectorMode {
        LINE,
        NODE_EDGE,
        ORTHOGONAL
    }
}
