package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Word wrapping for the hover description plaque, independent of any renderer. */
public final class DescriptionLines {
    private static final String ELLIPSIS = "...";

    private DescriptionLines() {
    }

    /**
     * Greedy word wrap; a single word wider than {@code maxWidth} gets its own line, and when more
     * than {@code maxLines} are needed the last line is cut with an ellipsis.
     */
    public static List<String> wrap(String text, double maxWidth, ToDoubleFunction<String> measure, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || maxLines < 1) return lines;
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && measure.applyAsDouble(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                candidate = word;
            }
            line.setLength(0);
            line.append(candidate);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines.size() <= maxLines ? lines : truncate(lines, maxLines, maxWidth, measure);
    }

    private static List<String> truncate(List<String> lines, int maxLines, double maxWidth, ToDoubleFunction<String> measure) {
        List<String> out = new ArrayList<>(lines.subList(0, maxLines));
        String last = out.get(maxLines - 1);
        while (last.length() > 1 && measure.applyAsDouble(last + ELLIPSIS) > maxWidth) {
            last = last.substring(0, last.length() - 1);
        }
        out.set(maxLines - 1, last + ELLIPSIS);
        return out;
    }
}
