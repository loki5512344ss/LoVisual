package dev.loki.lovisual.render.engine.text.glyph;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.glyph.custom.HorizontalFade;
import dev.loki.lovisual.render.engine.text.render.TextRenderSystem;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

/**
 * Text-render helpers backing {@link CustomTextRenderer}: the shadow/main
 * two-pass glyph sequence, single-text drawing, glyph-mesh submission and the
 * gradient / horizontal-fade runs. Tiers 1:1 with the former inline methods.
 */
final class GradientTexts {
    private GradientTexts() {
    }

    /**
     * Runs the shadow pass then the main pass, temporarily folding the mesh
     * alpha (or the colour alpha) into {@code SHADOW_COLOR.a}.
     */
    static double shadowPass(CustomTextRenderer r, RenderColor color, boolean alphaFromColor,
                             CustomTextRenderer.GlyphRun shadowGlyphs, CustomTextRenderer.GlyphRun mainGlyphs) {
        int preShadowA = CustomTextRenderer.SHADOW_COLOR.a;
        CustomTextRenderer.SHADOW_COLOR.a =
                (int) ((alphaFromColor ? color.a / 255.0 : r.mesh.alpha) * preShadowA);
        double ignored = shadowGlyphs.run();
        double width = mainGlyphs.run();
        CustomTextRenderer.SHADOW_COLOR.a = preShadowA;
        return width;
    }

    /** Draws one text at (x, y); when {@code shadow}, a SHADOW_COLOR copy is offset behind it. */
    static double drawText(CustomTextRenderer r, String text, double x, double y,
                           RenderColor color, boolean shadow) {
        double offset = r.fontScale * r.scale / 1.5;
        if (shadow) {
            return shadowPass(r, color, true,
                    () -> r.font.render(r.mesh, text, x + offset, y + offset, CustomTextRenderer.SHADOW_COLOR, r.scale / 1.5),
                    () -> r.font.render(r.mesh, text, x, y, color, r.scale / 1.5));
        }
        return r.font.render(r.mesh, text, x, y, color, r.scale / 1.5);
    }

    /** Submits the built glyph mesh through {@link TextRenderSystem}, picking the pipeline for the run. */
    static void submitMesh(CustomTextRenderer r) {
        boolean msdf = r.font.isMsdf();
        RenderPipeline pipeline = r.liquidGlassText
                ? (msdf ? LoVisualRenderPipelines.UI_TEXT_LIQUID_GLASS_MSDF : LoVisualRenderPipelines.UI_TEXT_LIQUID_GLASS)
                : (msdf ? LoVisualRenderPipelines.UI_TEXT_MSDF : LoVisualRenderPipelines.UI_TEXT);
        TextRenderSystem.submitGlyphMesh(
                r.liquidGlassText ? "LoVisual UI Liquid Glass Text" : "LoVisual UI Text",
                r.font,
                r.mesh,
                pipeline,
                TextPlacementMode.UI
        );
    }

    static double gradient(CustomTextRenderer r, String text, double x, double y,
                           Font.GlyphGradient gradient, boolean shadow) {
        return r.autoRun(() -> {
            r.ensureMeshStarted();
            if (shadow) {
                return r.shadowPass(null, false,
                        shadowGlyphs(r, text, x, y),
                        () -> r.font.renderGradient(r.mesh, text, x, y, r.scale / 1.5, gradient));
            }
            return r.font.renderGradient(r.mesh, text, x, y, r.scale / 1.5, gradient);
        });
    }

    static double quadGradient(CustomTextRenderer r, String text, double x, double y,
                               Font.GlyphQuadGradient gradient, boolean shadow) {
        return r.autoRun(() -> {
            r.ensureMeshStarted();
            if (shadow) {
                return r.shadowPass(null, false,
                        shadowGlyphs(r, text, x, y),
                        () -> r.font.renderQuadGradient(r.mesh, text, x, y, r.scale / 1.5, gradient));
            }
            return r.font.renderQuadGradient(r.mesh, text, x, y, r.scale / 1.5, gradient);
        });
    }

    static double horizontalFade(CustomTextRenderer r, String text, double x, double y, RenderColor color,
                                 double clipLeft, double clipRight, double fadeLeft, double fadeRight,
                                 boolean shadow) {
        // Optimize.lean_hud: plain solid text — skips the per-glyph String allocations and
        // gradient lambdas of the fade run (marquee edge fade is the visual price).
        if (OptimizeState.isLeanHud()) {
            return r.autoRun(() -> {
                r.ensureMeshStarted();
                return drawText(r, text, x, y, color, shadow);
            });
        }
        return r.autoRun(() -> {
            r.ensureMeshStarted();
            double glyphScale = r.scale / 1.5;
            Font.GlyphGradient mainGradient =
                    HorizontalFade.gradient(r.font, color, clipLeft, clipRight, fadeLeft, fadeRight, glyphScale);
            if (shadow) {
                Font.GlyphGradient shadowGradient =
                        HorizontalFade.gradient(r.font, CustomTextRenderer.SHADOW_COLOR, clipLeft, clipRight, fadeLeft, fadeRight, glyphScale);
                return r.shadowPass(color, false,
                        () -> r.font.renderGradient(r.mesh, text, x + r.fontScale * glyphScale, y + r.fontScale * glyphScale, glyphScale, shadowGradient),
                        () -> r.font.renderGradient(r.mesh, text, x, y, glyphScale, mainGradient));
            }
            return r.font.renderGradient(r.mesh, text, x, y, glyphScale, mainGradient);
        });
    }

    static double horizontalFadeClipped(CustomTextRenderer r, String text, double x, double y, RenderColor color,
                                        double clipLeft, double clipRight, double fadeLeft, double fadeRight,
                                        boolean shadow) {
        boolean wasBuilding = r.building;
        if (!wasBuilding) r.begin();
        boolean clipped = false;
        try {
            double clipWidth = Math.max(0.0, clipRight - clipLeft);
            double clipHeight = Math.max(0.0, r.getHeight(shadow));
            clipped = ScissorFunction.pushRaw((float) clipLeft, (float) y, (float) clipWidth, (float) clipHeight);
            return horizontalFade(r, text, x, y, color, clipLeft, clipRight, fadeLeft, fadeRight, shadow);
        } finally {
            if (clipped) {
                ScissorFunction.pop();
            }
            if (!wasBuilding) {
                r.end();
            }
        }
    }

    private static CustomTextRenderer.GlyphRun shadowGlyphs(CustomTextRenderer r, String text, double x, double y) {
        double offset = r.fontScale * r.scale / 1.5;
        double glyphScale = r.scale / 1.5;
        return () -> r.font.render(r.mesh, text, x + offset, y + offset, CustomTextRenderer.SHADOW_COLOR, glyphScale);
    }
}
