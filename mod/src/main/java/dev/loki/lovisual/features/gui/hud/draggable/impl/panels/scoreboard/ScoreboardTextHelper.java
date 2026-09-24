package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.glyph.custom.TextGlyphFallback;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.text.format.TextRenderUtil;
import dev.loki.lovisual.util.text.format.TextRenderUtil.Part;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ScoreboardTextHelper {

    private ScoreboardTextHelper() {}

    static float measureStyledText(TextRenderer fallback, Component text, int defaultColor, float scale) {
        if (fallback == null || text == null) return 0.0f;
        float width = 0.0f;
        for (Part part : TextRenderUtil.flattenStyled(text, defaultColor)) {
            width += partWidth(fallback, part, scale, 0.0f, 0);
        }
        return width;
    }

    static List<List<Part>> splitStyledLines(Component text, int defaultColor) {
        List<List<Part>> lines = new ArrayList<>();
        List<Part> current = new ArrayList<>();
        lines.add(current);
        for (Part part : TextRenderUtil.flattenStyled(text, defaultColor)) {
            String value = part.text();
            int start = 0;
            while (true) {
                int nl = value.indexOf('\n', start);
                String segment = nl >= 0 ? value.substring(start, nl) : value.substring(start);
                if (!segment.isEmpty()) current.add(new Part(segment, part.color(), part.bold(),
                        part.italic(), part.underline(), part.strikethrough(), part.obfuscated()));
                if (nl < 0) break;
                current = new ArrayList<>();
                lines.add(current);
                start = nl + 1;
            }
        }
        return lines;
    }

    static float measureStyledLineWidth(TextRenderer fallback, List<List<Part>> lines, float scale) {
        if (fallback == null || lines == null || lines.isEmpty()) return 0.0f;
        float widest = 0.0f;
        for (List<Part> line : lines) {
            float width = 0.0f;
            int idx = 0;
            for (Part part : line) width += partWidth(fallback, part, scale, 0.0f, idx++);
            widest = Math.max(widest, width);
        }
        return widest;
    }

    static float rendererHeight(TextRenderer renderer, float scale) {
        if (renderer == null) return 0.0f;
        renderer.begin(scale, false, false);
        float h = (float) renderer.getHeight(false);
        renderer.end();
        return h;
    }

    static TextRenderer styledRenderer(TextRenderer fallback, Part part) {
        if (part == null) return fallback;
        FontInfo.Type type;
        if (part.bold() && part.italic()) type = FontInfo.Type.BoldItalic;
        else if (part.bold()) type = FontInfo.Type.Bold;
        else if (part.italic()) type = FontInfo.Type.Italic;
        else type = FontInfo.Type.Regular;
        TextRenderer r = Fonts.renderer("Iosevka", type, fallback);
        return r != null ? r : fallback;
    }

    static TextRenderer rendererForGlyph(TextRenderer preferred, int codePoint) {
        return TextGlyphFallback.rendererForGlyph(preferred, codePoint);
    }

    static float svgGlyphSize(TextRenderer renderer, float scale) {
        return Math.max(1.0f, rendererHeight(renderer, scale)) * 0.92f;
    }

    static float partWidth(TextRenderer fallback, Part part, float scale, float timeSec, int salt) {
        if (fallback == null || part == null || part.text().isEmpty()) return 0.0f;
        TextRenderer renderer = styledRenderer(fallback, part);
        String text = part.obfuscated() ? obfuscate(part.text(), timeSec, salt) : part.text();
        return measureTextRuns(renderer, scale, text);
    }

    static float measureTextRuns(TextRenderer preferred, float scale, String text) {
        if (preferred == null || text == null || text.isEmpty()) return 0.0f;
        TextRenderer current = null;
        float width = 0.0f;
        float svgAdvance = svgGlyphSize(preferred, scale);
        try {
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (TextGlyphFallback.shouldUseVanillaSvg(preferred, cp)) {
                    if (current != null) { current.end(); current = null; }
                    width += svgAdvance;
                    i += Character.charCount(cp);
                    continue;
                }
                String glyph = new String(Character.toChars(cp));
                TextRenderer next = rendererForGlyph(preferred, cp);
                if (next != current) {
                    if (current != null) current.end();
                    current = next;
                    current.begin(scale, false, false);
                }
                width += (float) current.getWidth(glyph, false);
                i += Character.charCount(cp);
            }
            return width;
        } finally {
            if (current != null) current.end();
        }
    }

    static String obfuscate(String text, float timeSec, int salt) {
        if (text == null || text.isEmpty()) return "";
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int seed = Math.abs((int) (timeSec * 18.0f) * 31 + salt * 131);
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) out.append(ch);
            else out.append(alphabet.charAt(Math.floorMod(seed + i * 17 + ch, alphabet.length())));
        }
        return out.toString();
    }
}
