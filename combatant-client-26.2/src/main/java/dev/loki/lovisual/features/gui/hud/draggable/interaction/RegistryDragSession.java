/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.interaction;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.anchor.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistrySnapSupport;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import net.minecraft.client.Minecraft;

import java.util.HashSet;

/** Жизненный цикл перетаскивания: завершение, перемещение, snap-превью, курсор. */
public final class RegistryDragSession {

    private RegistryDragSession() {
    }

    public static void stopDragging(int screenW, int screenH) {
        if (RegistryState.dragging == null) return;
        if (!RegistryWidgetStore.isWidgetAnchorModifierDown()) {
            applyStickySnap(RegistryState.dragging, screenW, screenH);
        }
        RegistryState.dragging.clearParentAnchor();
        Minecraft mc = Minecraft.getInstance();
        boolean allowWidgetAnchoring = mc == null || !RegistryWidgetStore.isWidgetAnchorModifierDown();
        RegistrySnapSupport.updateDragAnchors(RegistryState.dragging, screenW, screenH, allowWidgetAnchoring);
        RegistryState.dragging.layoutValue().set(RegistryState.dragging.snapshotLayout());
        RegistryState.dragging.saveConfig();
        RegistryState.dragging = null;
        RegistryState.draggingWithLeft = false;
        clearSnapPreview();
        if (RegistryState.hudCursorActive) {
            SystemCursor.reset();
            RegistryState.hudCursorActive = false;
        }
    }

    public static void cancelDragging() {
        RegistryState.dragging = null;
        RegistryState.draggingWithLeft = false;
        clearSnapPreview();
        if (RegistryState.hudCursorActive) {
            SystemCursor.reset();
            RegistryState.hudCursorActive = false;
        }
        RegistryState.prevLeft = false;
        RegistryState.prevMiddle = false;
    }

    public static void moveDraggedWidget(DraggableHudElement w, float mx, float my, int screenW, int screenH) {
        float newX = mx - RegistryState.dragOffsetX;
        float newY = my - RegistryState.dragOffsetY;
        RegistryState.Bounds groupBounds = collectLinkedBounds(w, screenW, screenH);
        float dx = newX - w.getX();
        float dy = newY - w.getY();

        if (groupBounds != null && groupBounds.width() <= screenW && groupBounds.height() <= screenH) {
            dx = RegistryAnchorLayout.clamp(dx, -groupBounds.minX(), screenW - groupBounds.maxX());
            dy = RegistryAnchorLayout.clamp(dy, -groupBounds.minY(), screenH - groupBounds.maxY());
            w.moveTo(w.getX() + dx, w.getY() + dy);
            RegistryAnchorLayout.applyDescendantAnchors(w, screenW, screenH, new HashSet<>());
            return;
        }

        w.moveTo(RegistryAnchorLayout.clampX(newX, w, screenW), RegistryAnchorLayout.clampY(newY, w, screenH));
        RegistryAnchorLayout.applyDescendantAnchors(w, screenW, screenH, new HashSet<>());
    }

    public static void updateSnapPreview(DraggableHudElement w, int screenW, int screenH) {
        if (w == null || w.getWidth() <= 0 || w.getHeight() <= 0) {
            clearSnapPreview();
            return;
        }

        RegistryState.StickySnap snap = RegistrySnapSupport.findStickySnap(w, screenW, screenH);
        if (!snap.hasX() && !snap.hasY()) {
            clearSnapPreview();
            return;
        }

        RegistryState.snapPreviewActive = true;
        RegistryState.snapPreviewHasX = snap.hasX();
        RegistryState.snapPreviewHasY = snap.hasY();
        RegistryState.snapPreviewLineX = snap.x();
        RegistryState.snapPreviewLineY = snap.y();
    }

    public static void applyStickySnap(DraggableHudElement w, int screenW, int screenH) {
        if (w == null || w.getWidth() <= 0 || w.getHeight() <= 0) return;

        RegistryState.StickySnap snap = RegistrySnapSupport.findStickySnap(w, screenW, screenH);
        float newX = w.getX();
        float newY = w.getY();
        if (snap.hasX()) newX = snap.x();
        if (snap.hasY()) newY = snap.y();
        w.moveTo(RegistryAnchorLayout.clampX(newX, w, screenW), RegistryAnchorLayout.clampY(newY, w, screenH));
    }

    public static void clearSnapPreview() {
        RegistryState.snapPreviewActive = false;
        RegistryState.snapPreviewHasX = false;
        RegistryState.snapPreviewHasY = false;
    }

    public static void requestHoverCursor(float mx, float my) {
        DraggableHudElement interactive = findTopVisibleWidget(mx, my, true);
        if (interactive != null) {
            SystemCursor.set(SystemCursor.CursorType.HAND);
            RegistryState.hudCursorActive = true;
            return;
        }

        DraggableHudElement draggable = findTopVisibleWidget(mx, my, false);
        if (draggable != null) {
            SystemCursor.set(SystemCursor.CursorType.MOVE);
            RegistryState.hudCursorActive = true;
        }
    }

    public static DraggableHudElement findTopVisibleWidget(float mx, float my, boolean interactiveOnly) {
        DraggableHudElement best = null;
        long bestOrder = Long.MIN_VALUE;
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (!RegistryWidgetStore.isWidgetVisibleForEditor(w)) continue;
            if (!w.contains(mx, my)) continue;

            if (interactiveOnly) {
                if (!w.isMouseOverInteractive(mx, my)) continue;
            } else if (!w.isDraggable()) {
                continue;
            }

            long order = w.getRuntimeRenderOrder();
            if (order > bestOrder) {
                bestOrder = order;
                best = w;
            }
        }

        if (best != null) return best;
        for (int i = RegistryState.WIDGETS.size() - 1; i >= 0; i--) {
            DraggableHudElement w = RegistryState.WIDGETS.get(i);
            if (!RegistryWidgetStore.isWidgetVisibleForEditor(w)) continue;
            if (!w.contains(mx, my)) continue;

            if (interactiveOnly) {
                if (!w.isMouseOverInteractive(mx, my)) continue;
            } else if (!w.isDraggable()) {
                continue;
            }

            return w;
        }

        return null;
    }

    public static RegistryState.Bounds collectLinkedBounds(DraggableHudElement root, int screenW, int screenH) {
        if (root == null) return null;
        return collectLinkedBounds(root, screenW, screenH, new HashSet<>());
    }

    public static RegistryState.Bounds collectLinkedBounds(DraggableHudElement root, int screenW, int screenH,
                                                           java.util.Set<String> visited) {
        if (root == null || !visited.add(root.getId())) return null;
        RegistryState.Bounds bounds = RegistryState.Bounds.of(root);
        String parentId = root.getId();
        for (DraggableHudElement child : RegistryState.WIDGETS) {
            if (child == root) continue;
            if (!parentId.equals(child.getParentId()) || child.getParentSide() == WidgetAnchorSide.NONE) continue;
            RegistryAnchorLayout.applyWidgetAnchor(child, root, screenW, screenH);
            bounds = RegistryState.Bounds.union(bounds, collectLinkedBounds(child, screenW, screenH, visited));
        }
        return bounds;
    }

    public static boolean fitsLinkedBoundsAt(DraggableHudElement root,
                                             float desiredRootX,
                                             float desiredRootY,
                                             int screenW,
                                             int screenH) {
        RegistryState.Bounds bounds = collectLinkedBounds(root, screenW, screenH);
        if (bounds == null) return false;
        float dx = desiredRootX - root.getX();
        float dy = desiredRootY - root.getY();
        return bounds.minX() + dx >= 0f
                && bounds.minY() + dy >= 0f
                && bounds.maxX() + dx <= screenW
                && bounds.maxY() + dy <= screenH;
    }
}
