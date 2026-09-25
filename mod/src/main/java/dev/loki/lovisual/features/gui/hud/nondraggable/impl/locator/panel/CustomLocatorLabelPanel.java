package dev.loki.lovisual.features.gui.hud.nondraggable.impl.locator.panel;

import dev.loki.lovisual.render.engine.math.ColorMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.style.StyleSpec;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;
import dev.loki.lovisual.render.helpers.material.SurfaceRenderer;
import net.minecraft.util.Mth;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar.*;
import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel.*;
import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBarModel;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomXpBarRenderer;

public final class CustomLocatorLabelPanel {

    private CustomLocatorLabelPanel() {
    }

    public static void drawPanel(Renderer2D r2d, CustomBar bm, float x, float y, float w, float h,

                          float radius, float scale, float alpha, boolean hasTarget) {
        if (r2d == null || alpha <= 0.001f || w <= 0f || h <= 0f) return;

        float effectiveAlpha = Mth.clamp(alpha * (bm != null ? bm.getHudOverlayAlphaFactor() : 1.0f), 0.0f, 1.0f);
        float materialAlpha = Mth.clamp(0.78f + effectiveAlpha * 0.22f, 0.0f, 1.0f);
        float blurAlpha = effectiveAlpha;

        StyleSpec spec = new StyleSpec(
                SurfaceStyle.GLASS,
                15.5f,
                -26.0f,
                0.96f,
                0.98f,
                0.46f,
                0.172f * materialAlpha,
                2.65f,
                0xFFFFFFFF,
                0.6f,
                0.0f);
        SurfaceRenderer.plateSpec(r2d,
                x, y, w, h, radius, radius, radius, radius,
                spec, Math.max(0.001f, scale), 0xFFFFFFFF, materialAlpha, blurAlpha,
                SurfaceStyle.AUTO);

        r2d.roundedRect(
                x,
                y,
                w,
                h,
                radius,
                scale,
                CustomXpBarRenderer.applyBarAlpha(ColorMath.scaleAlpha(theme().windowBg(), alpha * 0.22f), bm)
        );
        r2d.roundedRectStrokeGradient(
                x,
                y,
                w,
                h,
                radius,
                Math.max(0.45f, 0.55f * scale),
                CustomXpBarRenderer.applyBarAlpha(ColorMath.scaleAlpha(theme().textPrimary(), alpha * 0.18f), bm),
                CustomXpBarRenderer.applyBarAlpha(ColorMath.scaleAlpha(theme().strokeSoft(), alpha * 0.20f), bm),
                118f
        );
    }

    public static void drawRow(Renderer2D r2d, CustomBar bm, CustomBarModel.LocatorLabel label,

                        float x, float y, float w, float h, float radius, float scale, float alpha) {
        if (r2d == null || label == null || alpha <= 0.001f || w <= 0f || h <= 0f) return;

        int rowFill = label.target()
                ? CustomXpBarRenderer.applyBarAlpha(ColorMath.scaleAlpha(theme().textPrimary(), alpha * 0.105f), bm)
                : CustomXpBarRenderer.applyBarAlpha(ColorMath.scaleAlpha(theme().surfaceHover(), alpha * 0.055f), bm);
        r2d.roundedRect(
                x,
                y,
                w,
                h,
                radius,
                scale,
                rowFill
        );

        if (label.target()) {
            float markerW = LOCATOR_LABEL_ACCENT_W * scale;
            float markerH = Math.max(1.0f * scale, h - 4.2f * scale);
            r2d.roundedRect(
                    x + 1.8f * scale,
                    y + (h - markerH) * 0.5f,
                    markerW,
                    markerH,
                    markerW * 0.5f,
                    0f,
                    CustomXpBarRenderer.applyBarAlpha(ColorMath.scaleAlpha(theme().textPrimary(), alpha * 0.72f), bm)
            );
        }
    }
}
