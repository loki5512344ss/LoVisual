package dev.loki.lovisual.render.engine.renderer.facade;

import dev.loki.lovisual.render.engine.renderer.facade.shape.Renderer2DShapes;
import dev.loki.lovisual.render.engine.renderer.renderer2d.fx.Glass2D;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.*;

/** Liquid-glass effects of the Renderer2D facade. */
public class Renderer2DGlass extends Renderer2DShapes {

    protected Renderer2DGlass(boolean textured) {
        super(textured);
    }

    public enum LiquidGlassPreset {
        LIGHT(7.0f, -5.50f, 0.70f, 0.42f, 0.40f, 0.030f),
        BALANCED(11.0f, -7.50f, 0.78f, 0.52f, 0.42f, 0.040f),
        HEAVY(15.0f, -9.00f, 0.86f, 0.62f, 0.48f, 0.052f),
        HUD_SMALL(11.0f, -12.00f, 0.84f, 0.78f, 0.22f, 0.055f),
        HUD_LARGE(13.5f, -14.00f, 0.88f, 0.72f, 0.20f, 0.060f),
        HEALTH_BAR(3.25f, -18.00f, 0.52f, 0.96f, 0.055f, 0.0030f);

        private final float thicknessPx;
        private final float fresnelPower;
        private final float fresnelAlpha;
        private final float baseAlpha;
        private final float fresnelMix;
        private final float distortPx;

        LiquidGlassPreset(float thicknessPx,
                          float fresnelPower,
                          float fresnelAlpha,
                          float baseAlpha,
                          float fresnelMix,
                          float distortPx) {
            this.thicknessPx = thicknessPx;
            this.fresnelPower = fresnelPower;
            this.fresnelAlpha = fresnelAlpha;
            this.baseAlpha = baseAlpha;
            this.fresnelMix = fresnelMix;
            this.distortPx = distortPx;
        }
    }

    public void liquidGlassCircle(double cx, double cy, double radius,
                                  float softness,
                                  int tintArgb,
                                  float globalAlpha,
                                  float fresnelPower,
                                  float fresnelAlpha,
                                  float baseAlpha,
                                  float fresnelMix,
                                  float distortPx,
                                  float squirclePower) {
        if (radius <= 0.0) return;
        double size = radius * 2.0;
        liquidGlassRect(cx - radius, cy - radius, size, size,
                (float) radius, softness,
                tintArgb,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius, float softness,
                                int tintArgb,
                                float globalAlpha,
                                float fresnelPower,
                                float fresnelAlpha,
                                float baseAlpha,
                                float fresnelMix,
                                float distortPx,
                                float squirclePower) {
        liquidGlassRect(x, y, w, h,
                radius, softness,
                tintArgb,
                globalAlpha,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius, float softness,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                float fresnelPower,
                                float fresnelAlpha,
                                float baseAlpha,
                                float fresnelMix,
                                float distortPx,
                                float squirclePower) {
        liquidGlassRectCorners(x, y, w, h,
                radius, radius, radius, radius,
                softness,
                tintArgb,
                glassAlpha,
                blurAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                LiquidGlassPreset preset) {
        liquidGlassRect(x, y, w, h,
                radius,
                tintArgb,
                glassAlpha,
                blurAlpha,
                preset,
                1.0f,
                0.0f);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float globalAlpha,
                                LiquidGlassPreset preset,
                                float thicknessScale,
                                float squirclePower) {
        liquidGlassRect(x, y, w, h, radius, tintArgb, globalAlpha, globalAlpha, preset, thicknessScale, squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                LiquidGlassPreset preset,
                                float thicknessScale,
                                float squirclePower) {
        LiquidGlassPreset p = preset != null ? preset : LiquidGlassPreset.BALANCED;
        float materialAlpha = clamp01(glassAlpha);
        float blurStrength = clamp01(blurAlpha);
        float scale = Math.max(0.001f, thicknessScale);
        liquidGlassRect(x, y, w, h,
                radius, p.thicknessPx * scale,
                tintArgb,
                materialAlpha,
                blurStrength,
                p.fresnelPower,
                p.fresnelAlpha,
                p.baseAlpha,
                p.fresnelMix,
                p.distortPx * materialAlpha,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int tintArgb,
                                       float glassAlpha,
                                       float blurAlpha,
                                       float fresnelPower,
                                       float fresnelAlpha,
                                       float baseAlpha,
                                       float fresnelMix,
                                       float distortPx,
                                       float squirclePower) {
        Glass2D.liquidGlassRectCorners(self(), x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, softness,
                tintArgb, glassAlpha, blurAlpha, fresnelPower, fresnelAlpha, baseAlpha, fresnelMix, distortPx, squirclePower);
    }

}
