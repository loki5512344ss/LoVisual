/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.registry;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.anchor.WidgetAnchorSide;
import dev.loki.lovisual.features.theme.Themes;

import java.util.ArrayList;
import java.util.List;

/**
 * Общее состояние реестра HUD-виджетов: константы, поля drag-and-drop
 * и структуры данных раскладки.
 */
public final class RegistryState {

    private RegistryState() {
    }

    public static final List<DraggableHudElement> WIDGETS = new ArrayList<>();
    public static final float SNAP_DIST = 8f;
    public static final float PARENT_SNAP_DIST = 10f;
    public static final float PARENT_ALIGN_SNAP_DIST = 5f;
    public static final float PARENT_GAP = 6f;
    public static final int SNAP_PREVIEW_LINE = 0xFFFFFFFF;
    public static final float DRAG_COORD_TEXT_SCALE = 0.85f;
    public static final float DRAG_COORD_MARGIN = 4f;
    public static final float LINK_WIRE_THICKNESS = 1.05f;
    public static final float LINK_PREVIEW_THICKNESS = 1.55f;

    public static DraggableHudElement dragging;
    public static float dragOffsetX;
    public static float dragOffsetY;
    public static boolean draggingWithLeft;
    public static boolean prevLeft;
    public static boolean prevMiddle;
    public static long renderOrderSeq;
    public static boolean forceVisible;
    public static boolean snapPreviewActive;
    public static boolean snapPreviewHasX;
    public static boolean snapPreviewHasY;
    public static float snapPreviewLineX;
    public static float snapPreviewLineY;
    public static String editorWidgetId;
    public static boolean hudCursorActive;
    public static int lastScreenW = -1;
    public static int lastScreenH = -1;
    public static Themes.Theme cachedLinkTheme;
    public static LinkWireColors cachedLinkColors;

    public record StickySnap(boolean hasX, boolean hasY, float x, float y) {
    }

    public record WidgetAnchorCandidate(DraggableHudElement parent, WidgetAnchorSide side, float offsetX,
                                        float offsetY) {
    }

    public record LinkWireColors(int wireStart, int wireEnd, int previewStart, int previewEnd) {
    }

    public record Bounds(float minX, float minY, float maxX, float maxY) {

        public static Bounds of(DraggableHudElement w) {
            return new Bounds(w.getX(), w.getY(), w.getX() + w.getWidth(), w.getY() + w.getHeight());
        }

        public static Bounds union(Bounds a, Bounds b) {
            if (a == null) return b;
            if (b == null) return a;
            return new Bounds(
                    Math.min(a.minX, b.minX),
                    Math.min(a.minY, b.minY),
                    Math.max(a.maxX, b.maxX),
                    Math.max(a.maxY, b.maxY)
            );
        }

        public float width() {
            return maxX - minX;
        }

        public float height() {
            return maxY - minY;
        }
    }
}
