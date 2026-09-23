/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedTriangulatorHudPanel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.CachedThrow;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.SolveResult;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorView;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model.DisplayState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.LinkedHashMap;
import java.util.List;
import dev.loki.lovisual.features.gui.hud.draggable.impl.Triangulator;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.render.TriangulatorMeasure;

/**
 * Assembles the scripted Triangulator panel: header button hitboxes, row
 * data and the final {@link ScriptedTriangulatorHudPanel.Panel} instance.
 */
public final class TriangulatorPainter {
    private static final Identifier HEADER_ICON =
            Identifier.fromNamespaceAndPath("lovisual", "textures/hud/elements/coords.png");
    private static final float HEADER_HEIGHT = 15.5f;
    private static final float CLEAR_BUTTON_RIGHT = 8.0f;

    /** Screen-space hitboxes of the header clear/copy buttons. */
    public record HeaderButtons(float clearX,
                                float clearY,
                                float clearSize,
                                float copyX,
                                float copyY,
                                float copySize) {
        public static final HeaderButtons NONE = new HeaderButtons(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    private TriangulatorPainter() {
    }

    public static HeaderButtons placeButtons(TriangulatorMeasure.Metrics metrics,
                                             float drawX,
                                             float drawY,
                                             float drawWidth,
                                             float drawHeight,
                                             float drawBaseScale,
                                             boolean showClearButton,
                                             boolean showCopyButton) {
        float buttonCursorRight = CLEAR_BUTTON_RIGHT * drawBaseScale;
        float clearX = 0.0f;
        float clearY = 0.0f;
        float clearSize = 0.0f;
        float copyX = 0.0f;
        float copyY = 0.0f;
        float copySize = 0.0f;
        if (showClearButton) {
            clearSize = TriangulatorMeasure.CLEAR_BUTTON_SIZE * drawBaseScale;
            clearX = drawX + drawWidth - buttonCursorRight - clearSize;
            clearY = drawY + (((HEADER_HEIGHT * drawBaseScale) - clearSize) * 0.5f);
            buttonCursorRight += clearSize + (TriangulatorMeasure.HEADER_BUTTON_GAP * drawBaseScale);
        }
        if (showCopyButton) {
            copySize = TriangulatorMeasure.CLEAR_BUTTON_SIZE * drawBaseScale;
            copyX = drawX + drawWidth - buttonCursorRight - copySize;
            copyY = drawY + (((HEADER_HEIGHT * drawBaseScale) - copySize) * 0.5f);
        }
        return new HeaderButtons(clearX, clearY, clearSize, copyX, copyY, copySize);
    }

    public static int headerIconColor(int baseColor, HudTextEffects.Effect effect, int effectSpeed) {
        if (effect == HudTextEffects.Effect.NONE) {
            return baseColor;
        }
        return HudTextEffects.animatedColor(
                baseColor,
                effect,
                effectSpeed,
                (float) (Util.getMillis() / 1000.0),
                0.0f
        );
    }

    public static List<LinkedHashMap<String, Object>> buildRows(List<CachedThrow> displayThrows,
                                                                SolveResult displayResult,
                                                                TriangulatorView view,
                                                                double autoResetDistance) {
        List<LinkedHashMap<String, Object>> panelRows = ScriptedTriangulatorHudPanel.rows();
        for (int i = 0; i < displayThrows.size(); i++) {
            CachedThrow cachedThrow = displayThrows.get(i);
            int markerColor = view.markerColor(cachedThrow, displayResult, i, displayThrows.size(), autoResetDistance);
            panelRows.add(ScriptedTriangulatorHudPanel.row(
                    "eye:" + cachedThrow.serial(),
                    "Eye #" + (i + 1),
                    TriangulatorSolver.formatThrowPoint(cachedThrow),
                    view.text(),
                    markerColor,
                    markerColor,
                    HudRenderUtil.scaleAlpha(view.muted(), 0.45f),
                    1.0f
            ));
        }
        return panelRows;
    }

    public static ScriptedTriangulatorHudPanel.Panel panel(TriangulatorView view,
                                                           TriangulatorMeasure.Metrics metrics,
                                                           float drawX,
                                                           float drawY,
                                                           float drawWidth,
                                                           float drawHeight,
                                                           float drawScale,
                                                           float drawBaseScale,
                                                           float drawFontScale,
                                                           int eyeCount,
                                                           boolean blur,
                                                           float blurStrength,
                                                           int headerIconColor,
                                                           boolean showClearButton,
                                                           boolean showCopyButton,
                                                           DisplayState state,
                                                           List<LinkedHashMap<String, Object>> rows) {
        return new ScriptedTriangulatorHudPanel.Panel(
                view.palette(),
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                drawScale,
                drawBaseScale,
                drawFontScale,
                metrics.headerTextH() * drawScale,
                metrics.rowTextH() * drawScale,
                metrics.activeLabelWidth() * drawScale,
                metrics.activeCountWidth() * drawScale,
                eyeCount,
                blur,
                blurStrength,
                ScriptedTriangulatorHudPanel.idString(HEADER_ICON),
                headerIconColor,
                showClearButton,
                view.danger(),
                showCopyButton,
                view.counter(),
                state.statusText(),
                state.primaryLine(),
                state.secondaryLine(),
                state.confidenceText(),
                state.statusColor(),
                rows
        );
    }
}
