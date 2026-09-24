package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.glyph.custom.TextGlyphFallback;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.text.format.TextRenderUtil.Part;
import net.minecraft.network.chat.Component;

import java.util.List;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardTextHelper.*;

final class ScoreboardTextRenderer {

    private ScoreboardTextRenderer() {}

    static void renderStyledText(Renderer2D shapeRenderer, TextRenderer fallback, float scale,
                                 float x, float y, Component text, int defaultColor, boolean animated,
                                 HudTextEffects.Effect effect, int effectSpeed, float timeSec, float phase) {
        if (fallback == null || text == null) return;
        float cursorX = x;
        int partIndex = 0;
        float lineHeight = rendererHeight(fallback, scale);
        for (Part part : dev.loki.lovisual.util.text.format.TextRenderUtil.flattenStyled(text, defaultColor)) {
            TextRenderer renderer = styledRenderer(fallback, part);
            String renderText = part.obfuscated() ? obfuscate(part.text(), timeSec + phase, partIndex) : part.text();
            float partW = renderTextRuns(shapeRenderer, renderer, scale, cursorX, y, renderText, part.color(),
                    animated, effect, effectSpeed, timeSec + phase + partIndex * 0.12f);
            renderDecorations(shapeRenderer, cursorX, y, partW, lineHeight, part);
            cursorX += partW;
            partIndex++;
        }
    }

    static void renderStyledLinesCentered(Renderer2D shapeRenderer, TextRenderer fallback, float scale,
                                          float boxX, float boxWidth, float y, List<List<Part>> lines,
                                          float lineGap, boolean animated, HudTextEffects.Effect effect,
                                          int effectSpeed, float timeSec) {
        if (fallback == null || lines == null || lines.isEmpty()) return;
        float lineHeight = rendererHeight(fallback, scale);
        float cursorY = y;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<Part> line = lines.get(lineIndex);
            float lineWidth = 0.0f;
            int measurePart = 0;
            for (Part part : line) lineWidth += partWidth(fallback, part, scale, timeSec + lineIndex * 0.18f, measurePart++);
            float cursorX = boxX + Math.max(0.0f, (boxWidth - lineWidth) * 0.5f);
            int partIndex = 0;
            for (Part part : line) {
                TextRenderer renderer = styledRenderer(fallback, part);
                String renderText = part.obfuscated() ? obfuscate(part.text(), timeSec + lineIndex * 0.18f, partIndex) : part.text();
                float partW = renderTextRuns(shapeRenderer, renderer, scale, cursorX, cursorY, renderText, part.color(),
                        animated, effect, effectSpeed, timeSec + lineIndex * 0.18f + partIndex * 0.12f);
                renderDecorations(shapeRenderer, cursorX, cursorY, partW, lineHeight, part);
                cursorX += partW;
                partIndex++;
            }
            cursorY += lineHeight + lineGap;
        }
    }

    static float renderTextRuns(Renderer2D shapeRenderer, TextRenderer preferred, float scale, float x, float y,
                                String text, int color, boolean animated, HudTextEffects.Effect effect,
                                int effectSpeed, float timeSec) {
        if (preferred == null || text == null || text.isEmpty()) return 0.0f;
        TextRenderer current = null;
        StringBuilder run = new StringBuilder();
        float cursorX = x;
        float runX = x;
        int glyphIndex = 0;
        float svgSize = svgGlyphSize(preferred, scale);
        float svgY = y + (rendererHeight(preferred, scale) - svgSize) * 0.5f;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (TextGlyphFallback.shouldUseVanillaSvg(preferred, cp)) {
                if (current != null && !run.isEmpty()) {
                    cursorX = flushTextRun(current, run.toString(), scale, runX, y, color, animated, effect, effectSpeed, timeSec + glyphIndex * 0.12f);
                    run.setLength(0);
                }
                current = null;
                String svgName = TextGlyphFallback.vanillaSvgName(cp);
                if (shapeRenderer != null && svgName != null) {
                    shapeRenderer.svg(svgName, cursorX, svgY, svgSize, svgSize,
                            SvgRenderOptions.fromFile().withAlpha(((color >>> 24) & 0xFF) / 255.0f));
                }
                cursorX += svgSize;
                runX = cursorX;
                i += Character.charCount(cp);
                glyphIndex++;
                continue;
            }
            TextRenderer next = rendererForGlyph(preferred, cp);
            if (current != null && next != current) {
                cursorX = flushTextRun(current, run.toString(), scale, runX, y, color, animated, effect, effectSpeed, timeSec + glyphIndex * 0.12f);
                run.setLength(0);
                runX = cursorX;
            }
            if (next != current) current = next;
            run.appendCodePoint(cp);
            i += Character.charCount(cp);
            glyphIndex++;
        }
        if (current != null && !run.isEmpty()) {
            cursorX = flushTextRun(current, run.toString(), scale, runX, y, color, animated, effect, effectSpeed, timeSec + glyphIndex * 0.12f);
        }
        return cursorX - x;
    }

    static float flushTextRun(TextRenderer renderer, String text, float scale, float x, float y, int color,
                              boolean animated, HudTextEffects.Effect effect, int effectSpeed, float timeSec) {
        if (renderer == null || text == null || text.isEmpty()) return x;
        renderer.begin(scale, false, false);
        boolean rendered = animated && HudTextEffects.render(renderer, text, x, y, color, effect, effectSpeed, timeSec, false);
        if (!rendered) renderer.render(text, x, y, new RenderColor(color), false);
        float width = (float) renderer.getWidth(text, false);
        renderer.end();
        return x + width;
    }

    static void renderDecorations(Renderer2D renderer, float x, float y, float w, float lineHeight, Part part) {
        if (renderer == null || part == null || w <= 0.0f) return;
        if (!part.underline() && !part.strikethrough()) return;
        int color = part.color();
        float h = Math.max(0.65f, lineHeight * 0.055f);
        if (part.underline()) renderer.quad(x, y + lineHeight * 0.88f, w, h, color);
        if (part.strikethrough()) renderer.quad(x, y + lineHeight * 0.54f, w, h, color);
    }
}
