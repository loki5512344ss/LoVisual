/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.buildBezier;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.roundedRectAnchor;

final class Renderer2DConnectors {
    private Renderer2DConnectors() {
    }

    static void connector(Renderer2D r, double x1, double y1, double x2, double y2, double thickness, int argb) {
        double[] c = r.connectorTmp;
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
        Renderer2DPolygons.polyline(r, c, 2, thickness, false, argb, true);
    }

    static void connectorGradient(Renderer2D r, double x1, double y1, double x2, double y2,
                                  double thickness, int startArgb, int endArgb) {
        double[] c = r.connectorTmp;
        c[0] = x1;
        c[1] = y1;
        c[2] = x2;
        c[3] = y2;
        Renderer2DPolygons.polylineGradient(r, c, 2, thickness, false, startArgb, endArgb, true);
    }

    static void orthogonalConnector(Renderer2D r, double x1, double y1, double x2, double y2,
                                    double midX, double thickness, int argb) {
        double[] c = r.connectorTmp;
        c[0] = x1;
        c[1] = y1;
        c[2] = midX;
        c[3] = y1;
        c[4] = midX;
        c[5] = y2;
        c[6] = x2;
        c[7] = y2;
        Renderer2DPolygons.polyline(r, c, 4, thickness, false, argb, true);
    }

    static void orthogonalConnectorGradient(Renderer2D r, double x1, double y1, double x2, double y2,
                                            double midX, double thickness, int startArgb, int endArgb) {
        double[] c = r.connectorTmp;
        c[0] = x1;
        c[1] = y1;
        c[2] = midX;
        c[3] = y1;
        c[4] = midX;
        c[5] = y2;
        c[6] = x2;
        c[7] = y2;
        Renderer2DPolygons.polylineGradient(r, c, 4, thickness, false, startArgb, endArgb, true);
    }

    static void bezierConnector(Renderer2D r, double x1, double y1,
                                double cx1, double cy1,
                                double cx2, double cy2,
                                double x2, double y2,
                                int segments,
                                double thickness,
                                int argb) {
        double[] c = r.connectorTmp;
        int count = buildBezier(c, x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments);
        Renderer2DPolygons.polyline(r, c, count, thickness, false, argb, false);
    }

    static void bezierConnectorGradient(Renderer2D r, double x1, double y1,
                                        double cx1, double cy1,
                                        double cx2, double cy2,
                                        double x2, double y2,
                                        int segments,
                                        double thickness,
                                        int startArgb,
                                        int endArgb) {
        double[] c = r.connectorTmp;
        int count = buildBezier(c, x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments);
        Renderer2DPolygons.polylineGradient(r, c, count, thickness, false, startArgb, endArgb, false);
    }

    static void nodeGraphEdge(Renderer2D r, double x1, double y1, double x2, double y2, double thickness, int argb) {
        double dx = Math.abs(x2 - x1);
        double c = Math.max(24.0, dx * 0.5);
        bezierConnector(r, x1, y1, x1 + c, y1, x2 - c, y2, x2, y2, 20, thickness, argb);
    }

    static void nodeGraphEdgeGradient(Renderer2D r, double x1, double y1, double x2, double y2,
                                      double thickness, int startArgb, int endArgb) {
        double dx = Math.abs(x2 - x1);
        double c = Math.max(24.0, dx * 0.5);
        bezierConnectorGradient(r, x1, y1, x1 + c, y1, x2 - c, y2, x2, y2, 20, thickness, startArgb, endArgb);
    }

    static void roundedRectConnector(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                     double x2, double y2, double w2, double h2, double radius2,
                                     double thickness, int argb) {
        double[] a = r.connectorAnchorTmp;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        connector(r, a[0], a[1], a[2], a[3], thickness, argb);
    }

    static void roundedRectConnectorGradient(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                             double x2, double y2, double w2, double h2, double radius2,
                                             double thickness, int startArgb, int endArgb) {
        double[] a = r.connectorAnchorTmp;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        connectorGradient(r, a[0], a[1], a[2], a[3], thickness, startArgb, endArgb);
    }

    static void roundedRectNodeGraphEdge(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                        double x2, double y2, double w2, double h2, double radius2,
                                        double thickness, int argb) {
        double[] a = r.connectorAnchorTmp;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        nodeGraphEdge(r, a[0], a[1], a[2], a[3], thickness, argb);
    }

    static void roundedRectNodeGraphEdgeGradient(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                 double x2, double y2, double w2, double h2, double radius2,
                                                 double thickness, int startArgb, int endArgb) {
        double[] a = r.connectorAnchorTmp;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        nodeGraphEdgeGradient(r, a[0], a[1], a[2], a[3], thickness, startArgb, endArgb);
    }

    static void roundedRectOrthogonalConnector(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                               double x2, double y2, double w2, double h2, double radius2,
                                               double midX, double thickness, int argb) {
        double[] a = r.connectorAnchorTmp;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        orthogonalConnector(r, a[0], a[1], a[2], a[3], midX, thickness, argb);
    }

    static void roundedRectOrthogonalConnectorGradient(Renderer2D r, double x1, double y1, double w1, double h1, double radius1,
                                                       double x2, double y2, double w2, double h2, double radius2,
                                                       double midX, double thickness, int startArgb, int endArgb) {
        double[] a = r.connectorAnchorTmp;
        roundedRectAnchor(a, 0, x1, y1, w1, h1, radius1, x2 + w2 * 0.5, y2 + h2 * 0.5);
        roundedRectAnchor(a, 2, x2, y2, w2, h2, radius2, x1 + w1 * 0.5, y1 + h1 * 0.5);
        orthogonalConnectorGradient(r, a[0], a[1], a[2], a[3], midX, thickness, startArgb, endArgb);
    }
}