package dev.loki.lovisual.render.engine.text.glyph;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.glyph.custom.CustomFontSet;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

public class CustomTextRenderer implements TextRenderer {
    public static final RenderColor SHADOW_COLOR = new RenderColor(60, 60, 60, 180);
    public final FontFace fontFace;
    final MeshBuilder mesh = new MeshBuilder(LoVisualRenderPipelines.UI_TEXT);
    final CustomFontSet fontSet;
    GlyphFont font;
    boolean building;
    boolean meshStarted;
    boolean liquidGlassText;
    double fontScale = 1;
    double scale = 1;

    public CustomTextRenderer(FontFace fontFace) {
        this.fontFace = fontFace;
        this.fontSet = new CustomFontSet(fontFace);
    }

    /**
     * Выбранный глиф-шрифт и производные масштабы; glyphScale/shadowOffset —
     * производные акцессоры, формат старого рекорда сохранён.
     */
    public record GlyphSelection(GlyphFont font, double fontScale, double scale) {
        public double glyphScale() {
            return scale / 1.5;
        }

        public double shadowOffset() {
            return fontScale * glyphScale();
        }
    }

    @Override
    public void setAlpha(double a) {
        mesh.alpha = a;
    }

    @Override
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");
        this.building = true;
        this.meshStarted = false;
        applySelection(fontSet.select(scale, big));
    }

    public GlyphSelection prepareGlyphFont(double requestedScale, boolean big) {
        GlyphSelection selection = fontSet.prepare(requestedScale, big);
        applySelection(selection);
        return selection;
    }

    private void applySelection(GlyphSelection selection) {
        this.font = selection.font();
        this.fontScale = selection.fontScale();
        this.scale = selection.scale();
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text == null || text.isEmpty()) return 0;
        GlyphFont activeFont = activeFont();
        return (activeFont.getWidth(text, length) + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double getHeight(boolean shadow) {
        GlyphFont activeFont = activeFont();
        return (activeFont.height() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        GlyphFont activeFont = building && font != null ? font : fontSet.base();
        return activeFont != null && activeFont.hasGlyph(codePoint);
    }

    private GlyphFont activeFont() {
        return building ? font : fontSet.base();
    }

    interface GlyphRun {
        double run();
    }

    double autoRun(GlyphRun body) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();
        try {
            return body.run();
        } finally {
            if (!wasBuilding) end();
        }
    }

    double shadowPass(RenderColor color, boolean alphaFromColor, GlyphRun shadowGlyphs, GlyphRun mainGlyphs) {
        return GradientTexts.shadowPass(this, color, alphaFromColor, shadowGlyphs, mainGlyphs);
    }

    @Override
    public double render(String text, double x, double y, RenderColor color, boolean shadow) {
        return autoRun(() -> {
            ensureMeshStarted();
            return GradientTexts.drawText(this, text, x, y, color, shadow);
        });
    }

    @Override
    public double renderLiquidGlass(String text, double x, double y, RenderColor color, boolean shadow) {
        // The glyph mesh belongs to a single pipeline. Mixing regular and
        // liquid-glass glyphs inside one begin/end block would submit earlier
        // glyphs through the wrong shader, so callers should use a separate
        // text run for this effect.
        if (building && meshStarted && !liquidGlassText) {
            return render(text, x, y, color, shadow);
        }
        return autoRun(() -> {
            liquidGlassText = true;
            ensureMeshStarted();
            return GradientTexts.drawText(this, text, x, y, color, shadow);
        });
    }

    public double renderGradient(String text, double x, double y, Font.GlyphGradient gradient, boolean shadow) {
        return GradientTexts.gradient(this, text, x, y, gradient, shadow);
    }

    @Override
    public double renderQuadGradient(String text, double x, double y, Font.GlyphQuadGradient gradient, boolean shadow) {
        return GradientTexts.quadGradient(this, text, x, y, gradient, shadow);
    }

    public double renderHorizontalFade(String text, double x, double y, RenderColor color,
                                       double clipLeft, double clipRight, double fadeLeft, double fadeRight,
                                       boolean shadow) {
        return GradientTexts.horizontalFade(this, text, x, y, color, clipLeft, clipRight, fadeLeft, fadeRight, shadow);
    }

    public double renderHorizontalFadeClipped(String text, double x, double y, RenderColor color,
                                              double clipLeft, double clipRight, double fadeLeft, double fadeRight,
                                              boolean shadow) {
        return GradientTexts.horizontalFadeClipped(this, text, x, y, color, clipLeft, clipRight, fadeLeft, fadeRight, shadow);
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (meshStarted) {
            mesh.end();
            if (!font.isReady() && fontSet.rebuild()) {
                applySelection(fontSet.select(scale * fontScale, false));
            }
            if (font.isReady()) {
                GradientTexts.submitMesh(this);
            }
        }

        building = false;
        meshStarted = false;
        liquidGlassText = false;
        scale = 1;
    }

    void ensureMeshStarted() {
        if (meshStarted) {
            return;
        }
        // Do not flush Renderer2D here. UI text is enqueued as an ordered Renderer2D batch entry
        // by TextRenderSystem.submitGlyphMesh(...). Only adjacent compatible text runs may merge;
        // text order relative to shapes/items is preserved. Non-UI/world text still submits immediately.
        if (!mesh.isBuilding()) {
            mesh.begin();
        }
        meshStarted = true;
    }

    public void destroy() {
        mesh.close();
        fontSet.close();
    }
}
