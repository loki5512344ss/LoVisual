package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.render;

import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.CachedThrow;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;
import dev.loki.lovisual.features.gui.hud.draggable.impl.Triangulator;

/**
 * Text measurement pass that computes the animated panel width/height
 * targets and shared header metrics for the Triangulator HUD.
 */
public final class TriangulatorMeasure {
    public static final float CLEAR_BUTTON_SIZE = 9.0f;
    public static final float HEADER_BUTTON_GAP = 3.0f;
    private static final float MIN_WIDTH = 126.0f;
    private static final float ROW_STEP = 11.0f;
    private static final float CONTENT_START_Y = 23.0f;
    private static final float SUMMARY_HEIGHT = 30.5f;
    private static final float TITLE_TEXT_X = 22.0f;
    private static final float COUNT_LABEL_OFFSET = 28.0f;

    public record Metrics(float headerTextH,
                          float rowTextH,
                          float maxWidth,
                          float activeLabelWidth,
                          float activeCountWidth,
                          float targetHeight) {
    }

    private TriangulatorMeasure() {
    }

    public static Metrics measure(TextRenderer headerTextRenderer,
                                  TextRenderer rowTextRenderer,
                                  float baseScale,
                                  float fontScale,
                                  List<CachedThrow> displayThrows,
                                  String statusText,
                                  String primaryLine,
                                  String secondaryLine,
                                  boolean showClearButton,
                                  boolean showCopyButton) {
        headerTextRenderer.begin(fontScale, true, false);
        float headerTextH = (float) headerTextRenderer.getHeight(false);
        float titleWidth = (float) headerTextRenderer.getWidth("Triangulator", false);
        headerTextRenderer.end();

        rowTextRenderer.begin(fontScale, true, false);
        float rowTextH = (float) rowTextRenderer.getHeight(false);
        float maxWidth = Math.max(MIN_WIDTH * baseScale,
                (float) rowTextRenderer.getWidth(statusText, false) + 52.0f * baseScale);
        maxWidth = Math.max(maxWidth, (float) rowTextRenderer.getWidth(primaryLine, false) + 20.0f * baseScale);
        rowTextRenderer.end();

        rowTextRenderer.begin(fontScale * 0.92f, false, false);
        maxWidth = Math.max(maxWidth, (float) rowTextRenderer.getWidth(secondaryLine, false) + 20.0f * baseScale);
        String activeCountText = Integer.toString(displayThrows.size());
        float activeLabelWidth = (float) rowTextRenderer.getWidth("Eyes:", false);
        float activeCountWidth = (float) rowTextRenderer.getWidth(activeCountText, false);
        rowTextRenderer.end();

        for (int i = 0; i < displayThrows.size(); i++) {
            CachedThrow cachedThrow = displayThrows.get(i);
            rowTextRenderer.begin(fontScale, true, false);
            float rowWidth = (float) rowTextRenderer.getWidth("Eye #" + (i + 1), false)
                    + (float) rowTextRenderer.getWidth(TriangulatorSolver.formatThrowPoint(cachedThrow), false)
                    + 36.0f * baseScale;
            rowTextRenderer.end();
            maxWidth = Math.max(maxWidth, rowWidth);
        }

        int headerButtons = (showClearButton ? 1 : 0) + (showCopyButton ? 1 : 0);
        float headerButtonsWidth = headerButtons == 0 ? 0.0f
                : ((CLEAR_BUTTON_SIZE * headerButtons) + (HEADER_BUTTON_GAP * Math.max(0, headerButtons - 1)) + 4.0f) * baseScale;
        float headerWidth = (TITLE_TEXT_X * baseScale) + titleWidth
                + activeLabelWidth + activeCountWidth
                + headerButtonsWidth
                + ((COUNT_LABEL_OFFSET + 16.0f) * baseScale);
        maxWidth = Math.max(maxWidth, headerWidth);

        float rowsHeight = displayThrows.isEmpty() ? 0.0f : displayThrows.size() * (ROW_STEP * baseScale);
        float dividerHeight = displayThrows.isEmpty() ? 0.0f : 5.0f * baseScale;
        float targetHeight = (CONTENT_START_Y * baseScale) + (SUMMARY_HEIGHT * baseScale) + dividerHeight + rowsHeight;
        return new Metrics(headerTextH, rowTextH, maxWidth, activeLabelWidth, activeCountWidth, targetHeight);
    }
}
