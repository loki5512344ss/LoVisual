package dev.loki.lovisual.render.engine.renderer.facade;

import dev.loki.lovisual.render.engine.renderer.renderer2d.Rects2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Strokes2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.fx.Effects2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.fx.Smoke2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Connectors2D;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.*;

/** Rounded-rect drawing family of the Renderer2D facade. */
public class Renderer2DRounded extends Renderer2DBase {

    protected Renderer2DRounded(boolean textured) {
        super(textured);
    }

    public void roundedRect(double x, double y, double w, double h,
                            float radius, float softness, int argb) {
        roundedRectMasked(x, y, w, h, x, y, w, h, radius, 0.0f, argb);
    }

    public void roundedRect(double x, double y, double w, double h,
                            float radius, int argb) {
        roundedRect(x, y, w, h, radius, 0.0f, argb);
    }

    public void roundedRectMasked(double x, double y, double w, double h,
                                  double maskX, double maskY, double maskW, double maskH,
                                  float radius, float softness, int argb) {
        roundedRectMaskedQuad(x, y, w, h, maskX, maskY, maskW, maskH, radius, 0.0f, argb, argb, argb, argb);
    }

    public void roundedRectStroke(double x, double y, double w, double h,
                                  float radius, float softness, float thickness, int argb) {
        Strokes2D.roundedRectStroke(self(), x, y, w, h, radius, softness, thickness, argb);
    }

    public void roundedRectStroke(double x, double y, double w, double h,
                                  float radius, float thickness, int argb) {
        roundedRectStroke(x, y, w, h, radius, 0.0f, thickness, argb);
    }

    public void roundedRectStrokeAngularGradient(double x, double y, double w, double h,
                                                 float radius, float softness, float thickness,
                                                 int startArgb, int endArgb, float offset) {
        Strokes2D.roundedRectStrokeAngularGradient(self(), x, y, w, h, radius, softness, thickness,
                startArgb, endArgb, offset);
    }

    public void roundedRectStrokeCorners(double x, double y, double w, double h,
                                         float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                         float softness, float thickness, int argb) {
        Strokes2D.roundedRectStrokeCorners(self(), x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL,
                softness, thickness, argb);
    }

    public void roundedRectStrokeCorners(double x, double y, double w, double h,
                                         float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                         float thickness, int argb) {
        roundedRectStrokeCorners(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 0.0f, thickness, argb);
    }

    public void roundedRectCorners(double x, double y, double w, double h,
                                   float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                   float softness, int argb) {
        roundedRectCornersQuad(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                0.0f,
                argb, argb, argb, argb);
    }

    public void roundedRectCorners(double x, double y, double w, double h,
                                   float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                   int argb) {
        roundedRectCorners(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 0.0f, argb);
    }

    public void roundedRectCornersQuad(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Rects2D.roundedRectCornersQuad(self(), x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, softness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectCornersQuad(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectCornersQuad(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 0.0f,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius, float softness,
                                    int startArgb, int endArgb, float angleDeg) {
        roundedRectGradient(x, y, w, h, radius, 0.0f, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius, int startArgb, int endArgb, float angleDeg) {
        roundedRectGradient(x, y, w, h, radius, 0.0f, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius, float softness,
                                    int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) w, (float) h, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        roundedRectGradientQuad(x, y, w, h, radius, 0.0f, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius,
                                    int startArgb, int endArgb, float angleDeg, float offsetPx) {
        roundedRectGradient(x, y, w, h, radius, 0.0f, startArgb, endArgb, angleDeg, offsetPx);
    }

    /**
     * Draws a progress segment clipped by the full rounded bar outline.
     *
     * <p>Unlike drawing a rounded rectangle with the current fill width, self() keeps the moving
     * progress edge straight and lets only the fixed outer bar bounds define rounded ends. That
     * prevents small health values from becoming a pill/capsule and prevents the fill from leaking
     * outside the left/right rounded shell when the progress edge crosses an outer corner.</p>
     */
    public void roundedProgressRectGradient(double x, double y, double w, double h,
                                            float radius, float softness,
                                            float progress,
                                            boolean fromRight,
                                            int startArgb, int endArgb,
                                            float angleDeg, float offsetPx) {
        Strokes2D.roundedProgressRectGradient(self(), x, y, w, h, radius, softness, progress, fromRight,
                startArgb, endArgb, angleDeg, offsetPx);
    }

    public void roundedProgressRectGradient(double x, double y, double w, double h,
                                            float radius,
                                            float progress,
                                            boolean fromRight,
                                            int startArgb, int endArgb,
                                            float angleDeg, float offsetPx) {
        roundedProgressRectGradient(x, y, w, h, radius, 0.0f, progress, fromRight,
                startArgb, endArgb, angleDeg, offsetPx);
    }

    public void roundedSmokeFill(double x, double y, double w, double h,
                                 float radius, float softness,
                                 float fillRatio,
                                 boolean fromRight,
                                 int firstArgb,
                                 int secondArgb,
                                 int thirdArgb,
                                 float time,
                                 float smokeScale,
                                 float smokeMix,
                                 int octaves,
                                 float flowX,
                                 float flowY,
                                 float intensity) {
        Smoke2D.roundedSmokeFill(self(), x, y, w, h, radius, softness, fillRatio, fromRight,
                firstArgb, secondArgb, thirdArgb, time, smokeScale, smokeMix, octaves, flowX, flowY, intensity);
    }

    public void roundedSmokeFill(double x, double y, double w, double h,
                                 float radius,
                                 float fillRatio,
                                 boolean fromRight,
                                 int firstArgb,
                                 int secondArgb,
                                 int thirdArgb,
                                 float time,
                                 float smokeScale,
                                 float smokeMix,
                                 int octaves,
                                 float flowX,
                                 float flowY,
                                 float intensity) {
        roundedSmokeFill(x, y, w, h, radius, 0.0f, fillRatio, fromRight,
                firstArgb, secondArgb, thirdArgb, time, smokeScale, smokeMix, octaves, flowX, flowY, intensity);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float softness, float thickness,
                                          int startArgb, int endArgb, float angleDeg) {
        roundedRectStrokeGradient(x, y, w, h, radius, 0.0f, thickness, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float thickness,
                                          int startArgb, int endArgb, float angleDeg) {
        roundedRectStrokeGradient(x, y, w, h, radius, 0.0f, thickness, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float softness, float thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) w, (float) h, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        roundedRectStrokeGradientQuad(x, y, w, h, radius, 0.0f, thickness, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        roundedRectStrokeGradient(x, y, w, h, radius, 0.0f, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void roundedRectGlow(double x, double y, double w, double h,
                                float radius, float softness, float glow, int argb) {
        Effects2D.roundedRectGlow(self(), x, y, w, h, radius, softness, glow, argb);
    }

    public void roundedRectShadow(double x, double y, double w, double h,
                                  float radius, float softness, float spread, int argb) {
        Effects2D.roundedRectShadow(self(), x, y, w, h, radius, softness, spread, argb);
    }

    public void roundedRectSoftShadow(double x, double y, double w, double h,
                                      float radius, float blur, float innerAlpha, int argb) {
        Effects2D.roundedRectSoftShadow(self(), x, y, w, h, radius, blur, innerAlpha, argb);
    }

    public void radialGlowMasked(double x, double y, double w, double h,
                                 float radius, float softness,
                                 float glowRadius, float cx, float cy,
                                 int argb) {
        Effects2D.radialGlowMasked(self(), x, y, w, h, radius, softness, glowRadius, cx, cy, argb);
    }

    public void roundedRectGradientQuad(double x, double y, double w, double h,
                                        float radius, float softness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectMaskedQuad(x, y, w, h, x, y, w, h, radius, 0.0f, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectGradientQuad(double x, double y, double w, double h,
                                        float radius,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectGradientQuad(x, y, w, h, radius, 0.0f, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectMaskedQuad(double x, double y, double w, double h,
                                      double maskX, double maskY, double maskW, double maskH,
                                      float radius, float softness,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Rects2D.roundedRectMaskedQuad(self(), x, y, w, h, maskX, maskY, maskW, maskH, radius, softness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectStrokeGradientQuad(double x, double y, double w, double h,
                                              float radius, float softness, float thickness,
                                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Strokes2D.roundedRectStrokeGradientQuad(self(), x, y, w, h, radius, softness, thickness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectStrokeGradientQuad(double x, double y, double w, double h,
                                              float radius, float thickness,
                                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectStrokeGradientQuad(x, y, w, h, radius, 0.0f, thickness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    /**
     * Connects two rounded rectangles by their visible border, not by their centers.
     * The connector uses the centers only as the direction vector used to find each border anchor.
     */
    public void roundedRectConnector(double x1, double y1, double w1, double h1, double radius1,
                                     double x2, double y2, double w2, double h2, double radius2,
                                     double thickness, int argb) {
        Connectors2D.roundedRectConnector(self(), x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2, thickness, argb);
    }

    public void roundedRectConnectorGradient(double x1, double y1, double w1, double h1, double radius1,
                                             double x2, double y2, double w2, double h2, double radius2,
                                             double thickness, int startArgb, int endArgb) {
        Connectors2D.roundedRectConnectorGradient(self(), x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                thickness, startArgb, endArgb);
    }

    /**
     * Draws a Bezier node graph edge between two rounded rectangles, anchored on their borders.
     */
    public void roundedRectNodeGraphEdge(double x1, double y1, double w1, double h1, double radius1,
                                         double x2, double y2, double w2, double h2, double radius2,
                                         double thickness, int argb) {
        Connectors2D.roundedRectNodeGraphEdge(self(), x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2, thickness, argb);
    }

    public void roundedRectNodeGraphEdgeGradient(double x1, double y1, double w1, double h1, double radius1,
                                                 double x2, double y2, double w2, double h2, double radius2,
                                                 double thickness, int startArgb, int endArgb) {
        Connectors2D.roundedRectNodeGraphEdgeGradient(self(), x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                thickness, startArgb, endArgb);
    }

    /**
     * Draws an orthogonal connector between two rounded rectangles, anchored on their borders.
     */
    /**
     * Draws an orthogonal connector between two rounded rectangles, anchored on their borders.
     */
    public void roundedRectOrthogonalConnector(double x1, double y1, double w1, double h1, double radius1,
                                               double x2, double y2, double w2, double h2, double radius2,
                                               double midX, double thickness, int argb) {
        Connectors2D.roundedRectOrthogonalConnector(self(), x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                midX, thickness, argb);
    }

    public void roundedRectOrthogonalConnectorGradient(double x1, double y1, double w1, double h1, double radius1,
                                                       double x2, double y2, double w2, double h2, double radius2,
                                                       double midX, double thickness, int startArgb, int endArgb) {
        Connectors2D.roundedRectOrthogonalConnectorGradient(self(), x1, y1, w1, h1, radius1,
                x2, y2, w2, h2, radius2, midX, thickness, startArgb, endArgb);
    }

}
