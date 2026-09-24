package dev.loki.lovisual.render.engine.renderer.renderer2d.path;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.buildBezier;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.roundedRectAnchor;

/** Wires, cables and node-graph edges built on top of polylines. */
public final class Connectors2D {
    private Connectors2D() {
    }

    public static void connector(Renderer2D r, double x1, double y1, double x2, double y2,
                                 double thickness, int argb) {
        double[] c = Batch2D.CONNECTOR;
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
        Polygons2D.polyline(r, c, 2, thickness, false, argb, true);
    }

    public static void connectorGradient(Renderer2D r, double x1, double y1, double x2, double y2,
                                         double thickness, int startArgb, int endArgb) {
        double[] c = Batch2D.CONNECTOR;
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
        Polygons2D.polylineGradient(r, c, 2, thickness, false, startArgb, endArgb, true);
    }

    public static void orthogonalConnector(Renderer2D r, double x1, double y1, double x2, double y2,
                                           double midX, double thickness, int argb) {
        double[] c = Batch2D.CONNECTOR;
        c[0] = x1;
        c[1] = y1;
        c[2] = midX;
        c[3] = y1;
        c[4] = midX;
        c[5] = y2;
        c[6] = x2;
        c[7] = y2;
        Polygons2D.polyline(r, c, 4, thickness, false, argb, true);
    }

    public static void orthogonalConnectorGradient(Renderer2D r, double x1, double y1, double x2, double y2,
                                                   double midX, double thickness, int startArgb, int endArgb) {
        double[] c = Batch2D.CONNECTOR;
        c[0] = x1;
        c[1] = y1;
        c[2] = midX;
        c[3] = y1;
        c[4] = midX;
        c[5] = y2;
        c[6] = x2;
        c[7] = y2;
        Polygons2D.polylineGradient(r, c, 4, thickness, false, startArgb, endArgb, true);
    }

    public static void bezierConnector(Renderer2D r, double x1, double y1,
                                       double cx1, double cy1,
                                       double cx2, double cy2,
                                       double x2, double y2,
                                       int segments,
                                       double thickness,
                                       int argb) {
        double[] c = Batch2D.CONNECTOR;
        int count = buildBezier(c, x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments);
        Polygons2D.polyline(r, c, count, thickness, false, argb, false);
    }

    public static void bezierConnectorGradient(Renderer2D r, double x1, double y1,
                                               double cx1, double cy1,
                                               double cx2, double cy2,
                                               double x2, double y2,
                                               int segments,
                                               double thickness,
                                               int startArgb,
                                               int endArgb) {
        double[] c = Batch2D.CONNECTOR;
        int count = buildBezier(c, x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments);
        Polygons2D.polylineGradient(r, c, count, thickness, false, startArgb, endArgb, false);
    }

    public static void nodeGraphEdge(Renderer2D r, double x1, double y1, double x2, double y2,
                                     double thickness, int argb) {
        double c = Math.max(24.0, Math.abs(x2 - x1) * 0.5);
        bezierConnector(r, x1, y1, x1 + c, y1, x2 - c, y2, x2, y2, 20, thickness, argb);
    }

    public static void nodeGraphEdgeGradient(Renderer2D r, double x1, double y1, double x2, double y2,
                                             double thickness, int startArgb, int endArgb) {
        double c = Math.max(24.0, Math.abs(x2 - x1) * 0.5);
        bezierConnectorGradient(r, x1, y1, x1 + c, y1, x2 - c, y2, x2, y2, 20, thickness, startArgb, endArgb);
    }

    public static void roundedRectConnector(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                            double x2, double y2, double w2, double h2, double radius2,
                                            double thickness, int argb) {
        double[] a = anchors(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2);
        connector(r, a[0], a[1], a[2], a[3], thickness, argb);
    }

    public static void roundedRectConnectorGradient(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                    double x2, double y2, double w2, double h2, double radius2,
                                                    double thickness, int startArgb, int endArgb) {
        double[] a = anchors(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2);
        connectorGradient(r, a[0], a[1], a[2], a[3], thickness, startArgb, endArgb);
    }

    public static void roundedRectNodeGraphEdge(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                double x2, double y2, double w2, double h2, double radius2,
                                                double thickness, int argb) {
        double[] a = anchors(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2);
        nodeGraphEdge(r, a[0], a[1], a[2], a[3], thickness, argb);
    }

    public static void roundedRectNodeGraphEdgeGradient(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                        double x2, double y2, double w2, double h2, double radius2,
                                                        double thickness, int startArgb, int endArgb) {
        double[] a = anchors(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2);
        nodeGraphEdgeGradient(r, a[0], a[1], a[2], a[3], thickness, startArgb, endArgb);
    }

    public static void roundedRectOrthogonalConnector(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                      double x2, double y2, double w2, double h2, double radius2,
                                                      double midX, double thickness, int argb) {
        double[] a = anchors(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2);
        orthogonalConnector(r, a[0], a[1], a[2], a[3], midX, thickness, argb);
    }

    public static void roundedRectOrthogonalConnectorGradient(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                              double x2, double y2, double w2, double h2, double radius2,
                                                              double midX, double thickness, int startArgb, int endArgb) {
        double[] a = anchors(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2);
        orthogonalConnectorGradient(r, a[0], a[1], a[2], a[3], midX, thickness, startArgb, endArgb);
    }

    /** Anchors both ends of a connector on the visible borders of the two rounded rects. */
    private static double[] anchors(double x1, double y1, double w1, double h1, double radius1,
                                    double x2, double y2, double w2, double h2, double radius2) {
        double[] a = Batch2D.CONNECTOR_ANCHOR;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        return a;
    }
}
