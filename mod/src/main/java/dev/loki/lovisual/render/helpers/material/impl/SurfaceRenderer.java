package dev.loki.lovisual.render.helpers.material;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.style.StyleSpec;
import dev.loki.lovisual.render.engine.renderer.style.StyleSpecs;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;

/**
 * Single entry point for drawing an element background plate in the resolved
 * surface style. An {@link SurfaceStyle#AUTO} override follows the global
 * selection from {@code StyleConfig} with the {@link StyleTransition} fade;
 * an explicit override draws that style immediately. Replaces the per-site
 * glass / blur / matte / outline branches scattered across HUD and ClickGUI.
 */
public enum SurfaceRenderer {
    ;

    /** Style an override resolves to right now (AUTO → animated global). */
    public static SurfaceStyle resolvedStyle(SurfaceStyle override) {
        return displayFor(override).style();
    }

    /** Draws a plate with a uniform corner radius. */
    public static void plate(Renderer2D renderer,
                             double x, double y, double w, double h,
                             float radius,
                             float scale, boolean large,
                             float blurAlpha, float glassAlpha,
                             int tintArgb, SurfaceStyle override) {
        plateCorners(renderer, x, y, w, h,
                radius, radius, radius, radius,
                scale, large, blurAlpha, glassAlpha, tintArgb, override);
    }

    /** Draws a plate with per-corner radii, dispatching on the resolved style. */
    public static void plateCorners(Renderer2D renderer,
                                    double x, double y, double w, double h,
                                    float radiusTL, float radiusTR,
                                    float radiusBR, float radiusBL,
                                    float scale, boolean large,
                                    float blurAlpha, float glassAlpha,
                                    int tintArgb, SurfaceStyle override) {
        if (renderer == null || w <= 0.0 || h <= 0.0) return;
        StyleTransition.Display display = displayFor(override);
        SurfaceStyle style = display.style();
        float fade = display.fade();
        float blur = AnimationUtility.clamp(blurAlpha, 0.0f, 1.0f) * fade;
        float glass = AnimationUtility.clamp(glassAlpha, 0.0f, 1.0f) * fade;
        float radius = (radiusTL + radiusTR + radiusBR + radiusBL) * 0.25f;

        switch (style) {
            case GLASS -> {
                if (blur <= 0.001f && glass <= 0.001f) return;
                StyleSpec spec = StyleSpecs.glass(large);
                float safeScale = Math.max(0.001f, scale);
                renderer.liquidGlassRectCorners(x, y, w, h,
                        radiusTL, radiusTR, radiusBR, radiusBL,
                        spec.softness() * safeScale,
                        tintArgb,
                        glass,
                        blur,
                        spec.fresnelPower(),
                        spec.fresnelAlpha(),
                        spec.baseAlpha(),
                        spec.fresnelMix(),
                        spec.distort() * glass,
                        spec.squircle());
            }
            case BLUR -> {
                if (blur <= 0.001f) return;
                StyleSpec spec = StyleSpecs.resolve(SurfaceStyle.BLUR);
                renderer.blurRect(x, y, w, h, radius,
                        spec.blurQuality(), 1.0f, blur, 0xFFFFFF);
            }
            case FLAT -> MatteHudStyle.drawPlate(renderer, x, y, w, h, radius, glass);
            case OUTLINE -> {
                if (glass <= 0.001f) return;
                StyleSpec spec = StyleSpecs.resolve(SurfaceStyle.OUTLINE);
                renderer.roundedRectStrokeCorners(x, y, w, h,
                        radiusTL, radiusTR, radiusBR, radiusBL,
                        spec.strokeWidth(), MatteHudStyle.strokeColor(glass));
            }
            case NONE, AUTO -> {
                // Nothing to draw; AUTO never reaches here (displayFor resolves it).
            }
        }
    }

    /**
     * Draws a bespoke glass material described by {@code spec}, or the matching
     * non-glass variant when the resolved style is not glass. Unlike {@link #plate},
     * {@code spec.distort()} is passed through as-is (call sites pre-multiply it),
     * and {@code spec.blurQuality()} > 0 requests an extra outer blur pass before
     * the glass (0 = the glass pass blurs by itself).
     */
    public static void plateSpec(Renderer2D renderer,
                                 double x, double y, double w, double h,
                                 float radiusTL, float radiusTR,
                                 float radiusBR, float radiusBL,
                                 StyleSpec spec,
                                 float scale,
                                 int tintArgb,
                                 float glassAlpha,
                                 float blurAlpha,
                                 SurfaceStyle override) {
        if (renderer == null || w <= 0.0 || h <= 0.0 || spec == null) return;
        StyleTransition.Display display = displayFor(override);
        float fade = display.fade();
        float glass = AnimationUtility.clamp(glassAlpha, 0.0f, 1.0f) * fade;
        float blur = AnimationUtility.clamp(blurAlpha, 0.0f, 1.0f) * fade;
        float radius = (radiusTL + radiusTR + radiusBR + radiusBL) * 0.25f;

        switch (display.style()) {
            case GLASS -> {
                if (spec.blurQuality() > 0.0f && blur > 0.001f) {
                    renderer.blurRect(x, y, w, h, radius,
                            spec.blurQuality(), 1.0f, blur, 0xFFFFFF);
                }
                if (glass <= 0.001f && blur <= 0.001f) return;
                renderer.liquidGlassRectCorners(x, y, w, h,
                        radiusTL, radiusTR, radiusBR, radiusBL,
                        Math.max(0.001f, spec.softness() * scale),
                        tintArgb,
                        glass,
                        blur,
                        spec.fresnelPower(),
                        spec.fresnelAlpha(),
                        spec.baseAlpha(),
                        spec.fresnelMix(),
                        spec.distort(),
                        spec.squircle());
            }
            case BLUR -> {
                float strength = blur > 0.001f ? blur : glass;
                if (strength <= 0.001f) return;
                float quality = spec.blurQuality() > 0.0f ? spec.blurQuality() : 1.0f;
                renderer.blurRect(x, y, w, h, radius, quality, 1.0f, strength, 0xFFFFFF);
            }
            case FLAT -> MatteHudStyle.drawPlate(renderer, x, y, w, h, radius, glass);
            case OUTLINE -> {
                if (glass <= 0.001f) return;
                renderer.roundedRectStrokeCorners(x, y, w, h,
                        radiusTL, radiusTR, radiusBR, radiusBL,
                        spec.strokeWidth(), MatteHudStyle.strokeColor(glass));
            }
            case NONE, AUTO -> {
                // Nothing to draw; AUTO never reaches here (displayFor resolves it).
            }
        }
    }

    private static StyleTransition.Display displayFor(SurfaceStyle override) {
        StyleTransition.Display base =
                (override == null || override == SurfaceStyle.AUTO)
                        ? StyleTransition.display()
                        : new StyleTransition.Display(override, 1.0f);
        // Optimize.no_glass kill-switch: fall back to a flat plate instead of
        // disabling the surface entirely (blur/glass resources are unavailable).
        if (OptimizeState.isNoGlass() && base.style() != SurfaceStyle.NONE) {
            return new StyleTransition.Display(SurfaceStyle.FLAT, base.fade());
        }
        return base;
    }
}
