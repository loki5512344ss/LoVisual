package dev.loki.lovisual.features.gui.clickgui.render;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiDrawHelpers;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextBatchItem;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextBatchKey;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextHeightKey;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextMeasureKey;

public final class ClickGuiTextBatch {

    private ClickGuiTextBatch() {
    }

    static float scaleForSize(float size) {
        return size / 18.0f;
    }

    public static float textWidth(TextRenderer tr, String text, float size) {

        if (tr == null || text == null || text.isEmpty()) return 0f;
        TextMeasureKey key = new TextMeasureKey(tr, Float.floatToIntBits(size), text);
        Float cached = ClickGuiRenderer.TEXT_WIDTH_CACHE.get(key);
        if (cached != null) return cached;
        float scale = scaleForSize(size);
        tr.begin(scale, true, false);
        try {
            float width = (float) tr.getWidth(text, false);
            ClickGuiRenderer.TEXT_WIDTH_CACHE.put(key, width);
            return width;
        } finally {
            tr.end();
        }
    }

    public static float textHeight(TextRenderer tr, float size) {

        if (tr == null) return 0f;
        TextHeightKey key = new TextHeightKey(tr, Float.floatToIntBits(size));
        Float cached = ClickGuiRenderer.TEXT_HEIGHT_CACHE.get(key);
        if (cached != null) return cached;
        float scale = scaleForSize(size);
        tr.begin(scale, true, false);
        try {
            float height = (float) tr.getHeight(false);
            ClickGuiRenderer.TEXT_HEIGHT_CACHE.put(key, height);
            return height;
        } finally {
            tr.end();
        }
    }

    static void beginTextBatch() {
        if (ClickGuiRenderer.textBatchActive) return;
        ClickGuiRenderer.textBatchActive = true;
        ClickGuiRenderer.TEXT_BATCH.clear();
    }

    static void endTextBatch() {
        if (!ClickGuiRenderer.textBatchActive) return;
        ClickGuiRenderer.textBatchActive = false;
        if (ClickGuiRenderer.TEXT_BATCH.isEmpty()) return;

        LinkedHashMap<TextBatchKey, ArrayList<TextBatchItem>> groups = new LinkedHashMap<>();
        for (TextBatchItem item : ClickGuiRenderer.TEXT_BATCH) {
            TextBatchKey key = new TextBatchKey(item.renderer(), Float.floatToIntBits(item.size()), item.shadow());
            ArrayList<TextBatchItem> list = groups.get(key);
            if (list == null) {
                list = new ArrayList<>();
                groups.put(key, list);
            }
            list.add(item);
        }

        for (var entry : groups.entrySet()) {
            TextBatchKey key = entry.getKey();
            TextRenderer tr = key.renderer();
            if (tr == null) continue;
            float size = Float.intBitsToFloat(key.sizeBits());
            float scale = scaleForSize(size);
            tr.begin(scale, false, false);
            for (TextBatchItem item : entry.getValue()) {
                tr.render(item.text(), item.x(), item.y(), new RenderColor(item.argb()), item.shadow());
            }
            tr.end();
        }

        ClickGuiRenderer.TEXT_BATCH.clear();
    }

    public static void drawText(TextRenderer tr, String text, float x, float y, float size, int argb, boolean shadow) {

        if (tr == null || text == null || text.isEmpty()) return;
        argb = ClickGuiDrawHelpers.applyAlpha(argb);
        if (((argb >>> 24) & 0xFF) <= 0) return;
        if (ClickGuiRenderer.textBatchActive) {
            ClickGuiRenderer.TEXT_BATCH.add(new TextBatchItem(tr, text, x, y, size, argb, shadow));
            return;
        }
        float scale = scaleForSize(size);
        tr.begin(scale, false, false);
        tr.render(text, x, y, new RenderColor(argb), shadow);
        tr.end();
    }

    static String fitText(TextRenderer tr, String text, float size, float maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0f) return text;
        String trimmed = text;
        float w = textWidth(tr, trimmed, size);
        if (w <= maxWidth) return trimmed;
        while (trimmed.length() > 1 && textWidth(tr, trimmed + "...", size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }

    static List<String> wrapText(TextRenderer tr, String text, float size, float maxWidth, int maxLines) {
        ArrayList<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        if (maxLines <= 1 || maxWidth <= 0f) {
            lines.add(fitText(tr, text, size, maxWidth));
            return lines;
        }

        String remaining = normalizeSpaces(text);
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (lines.size() == maxLines - 1) {
                lines.add(fitText(tr, remaining, size, maxWidth));
                break;
            }
            if (textWidth(tr, remaining, size) <= maxWidth) {
                lines.add(remaining);
                break;
            }

            int split = bestWrapIndex(tr, remaining, size, maxWidth);
            String line = remaining.substring(0, split).trim();
            if (line.isEmpty()) {
                lines.add(fitText(tr, remaining, size, maxWidth));
                break;
            }
            lines.add(line);
            remaining = remaining.substring(split).trim();
        }

        if (lines.isEmpty()) {
            lines.add(fitText(tr, text, size, maxWidth));
        }
        return lines;
    }

    private static int bestWrapIndex(TextRenderer tr, String text, float size, float maxWidth) {
        int bestSpace = -1;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) continue;
            String candidate = text.substring(0, i).trim();
            if (!candidate.isEmpty() && textWidth(tr, candidate, size) <= maxWidth) {
                bestSpace = i;
                continue;
            }
            if (bestSpace >= 0) break;
        }
        if (bestSpace >= 0) return bestSpace;

        String fitted = fitText(tr, text, size, maxWidth);
        if (fitted.endsWith("...")) {
            fitted = fitted.substring(0, Math.max(1, fitted.length() - 3));
        }
        return Math.max(1, Math.min(text.length(), fitted.length()));
    }

    private static String normalizeSpaces(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(trimmed.length());
        boolean lastSpace = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastSpace) {
                    sb.append(' ');
                    lastSpace = true;
                }
            } else {
                sb.append(c);
                lastSpace = false;
            }
        }
        return sb.toString();
    }
}
