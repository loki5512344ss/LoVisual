package dev.loki.lovisual.render.engine.renderer.facade;

import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Chamfered2D;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.*;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Connectors2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Polygons2D;

/** Chamfered, connector, cable and polygon family of the Renderer2D facade. */
public class Renderer2DPath extends Renderer2DRounded {

    protected Renderer2DPath(boolean textured) {
        super(textured);
    }

    public void chamferedRect(double x, double y, double width, double height,
                              double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                              int argb) {
        chamferedRectQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                argb, argb, argb, argb);
    }

    public void chamferedRectGradient(double x, double y, double width, double height,
                                      double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                      int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectStroke(double x, double y, double width, double height,
                                    double chamfer, double thickness, int argb) {
        chamferedRectStrokeQuad(x, y, width, height, chamfer, thickness, argb, argb, argb, argb);
    }

    public void chamferedRectStroke(double x, double y, double width, double height,
                                    double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                    double thickness, int argb) {
        chamferedRectStrokeQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                thickness, argb, argb, argb, argb);
    }

    public void chamferedRectStrokeGradient(double x, double y, double width, double height,
                                            double chamfer, double thickness,
                                            int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectStrokeQuad(x, y, width, height, chamfer, thickness,
                gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectStrokeGradient(double x, double y, double width, double height,
                                            double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                            double thickness,
                                            int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectStrokeQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                thickness, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectQuad(double x, double y, double w, double h,
                                  double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                  int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        normalizeChamfers(w, h,
                (float) chamferTL,
                (float) chamferTR,
                (float) chamferBR,
                (float) chamferBL,
                Batch2D.CORNER_RADII);
        chamferedRectQuadAxes(x, y, w, h,
                Batch2D.CORNER_RADII[0], Batch2D.CORNER_RADII[0],
                Batch2D.CORNER_RADII[1], Batch2D.CORNER_RADII[1],
                Batch2D.CORNER_RADII[2], Batch2D.CORNER_RADII[2],
                Batch2D.CORNER_RADII[3], Batch2D.CORNER_RADII[3],
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectQuadAxes(double x, double y, double w, double h,
                                      double chamferTLX, double chamferTLY,
                                      double chamferTRX, double chamferTRY,
                                      double chamferBRX, double chamferBRY,
                                      double chamferBLX, double chamferBLY,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Chamfered2D.chamferedRectQuadAxes(self(), x, y, w, h,
                chamferTLX, chamferTLY, chamferTRX, chamferTRY, chamferBRX, chamferBRY, chamferBLX, chamferBLY,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectStrokeQuad(double x, double y, double w, double h,
                                        double chamfer, double thickness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        chamferedRectStrokeQuad(x, y, w, h,
                chamfer, chamfer, chamfer, chamfer,
                thickness, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectStrokeQuad(double x, double y, double w, double h,
                                        double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                        double thickness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Chamfered2D.chamferedRectStrokeQuad(self(), x, y, w, h,
                chamferTL, chamferTR, chamferBR, chamferBL, thickness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void notchedRect(double x, double y, double width, double height,
                             double notchWidth, double notchDepth, int argb) {
        int count = buildNotchedRect(Batch2D.RECT_SHAPE, x, y, width, height, notchWidth, notchDepth);
        Polygons2D.polygon(self(), Batch2D.RECT_SHAPE, count, argb);
    }

    public void notchedRectGradient(double x, double y, double width, double height,
                                    double notchWidth, double notchDepth,
                                    int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int count = buildNotchedRect(Batch2D.RECT_SHAPE, x, y, width, height, notchWidth, notchDepth);
        Polygons2D.polygonGradient(self(), Batch2D.RECT_SHAPE, count, x, y, width, height, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void notchedRectStroke(double x, double y, double width, double height,
                                  double notchWidth, double notchDepth, double thickness, int argb) {
        int count = buildNotchedRect(Batch2D.RECT_SHAPE, x, y, width, height, notchWidth, notchDepth);
        Polygons2D.polygonStroke(self(), Batch2D.RECT_SHAPE, count, thickness, true, argb);
    }

    public void notchedRectStrokeGradient(double x, double y, double width, double height,
                                          double notchWidth, double notchDepth, double thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int count = buildNotchedRect(Batch2D.RECT_SHAPE, x, y, width, height, notchWidth, notchDepth);
        Polygons2D.polylineLinearGradient(self(), Batch2D.RECT_SHAPE, count, thickness, true,
                x, y, width, height, startArgb, endArgb, angleDeg, offsetPx, false);
    }

    public void beveledRect(double x, double y, double width, double height,
                            double bevel, int fillArgb, int highlightArgb, int shadowArgb) {
        Chamfered2D.beveledRect(self(), x, y, width, height, bevel, fillArgb, highlightArgb, shadowArgb);
    }

    public void beveledRectStroke(double x, double y, double width, double height,
                                  double bevel, double thickness, int argb) {
        chamferedRectStroke(x, y, width, height, bevel, thickness, argb);
    }

    public void beveledRectGradient(double x, double y, double width, double height,
                                    double bevel, int startArgb, int endArgb,
                                    int highlightArgb, int shadowArgb, float angleDeg, float offsetPx) {
        Chamfered2D.beveledRectGradient(self(), x, y, width, height, bevel, startArgb, endArgb,
                highlightArgb, shadowArgb, angleDeg, offsetPx);
    }

    public void beveledRectStrokeGradient(double x, double y, double width, double height,
                                          double bevel, double thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        chamferedRectStrokeGradient(x, y, width, height, bevel, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void connector(double x1, double y1, double x2, double y2, double thickness, int argb) {
        Connectors2D.connector(self(), x1, y1, x2, y2, thickness, argb);
    }

    public void connectorGradient(double x1, double y1, double x2, double y2,
                                  double thickness, int startArgb, int endArgb) {
        Connectors2D.connectorGradient(self(), x1, y1, x2, y2, thickness, startArgb, endArgb);
    }

    public void wire(double x1, double y1, double x2, double y2, double thickness, int argb) {
        connector(x1, y1, x2, y2, thickness, argb);
    }

    public void wireGradient(double x1, double y1, double x2, double y2,
                             double thickness, int startArgb, int endArgb) {
        connectorGradient(x1, y1, x2, y2, thickness, startArgb, endArgb);
    }

    public void cable(double x1, double y1, double x2, double y2,
                      double thickness, int outerArgb, int innerArgb) {
        connector(x1, y1, x2, y2, thickness + 2.0, outerArgb);
        connector(x1, y1, x2, y2, Math.max(1.0, thickness), innerArgb);
    }

    public void cableGradient(double x1, double y1, double x2, double y2,
                              double thickness, int outerStartArgb, int outerEndArgb,
                              int innerStartArgb, int innerEndArgb) {
        connectorGradient(x1, y1, x2, y2, thickness + 2.0, outerStartArgb, outerEndArgb);
        connectorGradient(x1, y1, x2, y2, Math.max(1.0, thickness), innerStartArgb, innerEndArgb);
    }

    public void orthogonalConnector(double x1, double y1, double x2, double y2,
                                    double midX, double thickness, int argb) {
        Connectors2D.orthogonalConnector(self(), x1, y1, x2, y2, midX, thickness, argb);
    }

    public void orthogonalConnectorGradient(double x1, double y1, double x2, double y2,
                                            double midX, double thickness, int startArgb, int endArgb) {
        Connectors2D.orthogonalConnectorGradient(self(), x1, y1, x2, y2, midX, thickness, startArgb, endArgb);
    }

    public void bezierConnector(double x1, double y1,
                                double cx1, double cy1,
                                double cx2, double cy2,
                                double x2, double y2,
                                int segments,
                                double thickness,
                                int argb) {
        Connectors2D.bezierConnector(self(), x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments, thickness, argb);
    }

    public void bezierConnectorGradient(double x1, double y1,
                                        double cx1, double cy1,
                                        double cx2, double cy2,
                                        double x2, double y2,
                                        int segments,
                                        double thickness,
                                        int startArgb,
                                        int endArgb) {
        Connectors2D.bezierConnectorGradient(self(), x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments, thickness, startArgb, endArgb);
    }

    public void nodeGraphEdge(double x1, double y1, double x2, double y2, double thickness, int argb) {
        Connectors2D.nodeGraphEdge(self(), x1, y1, x2, y2, thickness, argb);
    }

    public void nodeGraphEdgeGradient(double x1, double y1, double x2, double y2,
                                      double thickness, int startArgb, int endArgb) {
        Connectors2D.nodeGraphEdgeGradient(self(), x1, y1, x2, y2, thickness, startArgb, endArgb);
    }

    public void spline(double[] points, int pointCount, double thickness, boolean closed, int argb) {
        if (points == null || pointCount < 2) return;
        Polygons2D.polyline(self(), points, pointCount, thickness, closed, argb, true);
    }

    public void splineGradient(double[] points, int pointCount, double thickness, boolean closed,
                               int startArgb, int endArgb) {
        if (points == null || pointCount < 2) return;
        Polygons2D.polylineGradient(self(), points, pointCount, thickness, closed, startArgb, endArgb, true);
    }

    public void polygon(double[] points, int pointCount, int argb) {
        Polygons2D.polygon(self(), points, pointCount, argb);
    }

}
