package dev.loki.lovisual.features.gui.hud.draggable.registry;

import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorX;
import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorY;
import dev.loki.lovisual.features.gui.hud.anchor.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragSession;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistrySnapSupport;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragSession;

/** Математика прилипания: якоря-кандидаты, sticky snap, якоря после drag. */
public final class RegistrySnapSupport {

    private RegistrySnapSupport() {
    }

    public static RegistryState.WidgetAnchorCandidate findWidgetAnchorCandidate(DraggableHudElement dragging, int screenW, int screenH) {
        float bestDist = Float.MAX_VALUE;
        RegistryState.WidgetAnchorCandidate best = null;
        for (DraggableHudElement target : RegistryState.WIDGETS) {
            if (target == dragging) continue;
            if (!RegistryWidgetStore.isWidgetVisibleForLinkOverlay(target)) continue;
            if (RegistryWidgetStore.isDescendantOf(target, dragging)) continue;
            if (target.getWidth() <= 0 || target.getHeight() <= 0) continue;

            float dxOverlap = overlap(dragging.getX(), dragging.getX() + dragging.getWidth(),
                    target.getX(), target.getX() + target.getWidth());
            float dyOverlap = overlap(dragging.getY(), dragging.getY() + dragging.getHeight(),
                    target.getY(), target.getY() + target.getHeight());

            float distTop = Math.abs((dragging.getY() + dragging.getHeight()) - (target.getY() - RegistryState.PARENT_GAP));
            if (dxOverlap > 0 && distTop <= RegistryState.PARENT_SNAP_DIST && distTop < bestDist) {
                float offsetX = parentAxisOffset(dragging.getX() - target.getX());
                float desiredY = target.getY() - RegistryState.PARENT_GAP - dragging.getHeight();
                float desiredX = target.getX() + offsetX;
                if (!RegistryDragSession.fitsLinkedBoundsAt(dragging, desiredX, desiredY, screenW, screenH)) continue;
                float offsetY = 0f;
                bestDist = distTop;
                best = new RegistryState.WidgetAnchorCandidate(target, WidgetAnchorSide.TOP, offsetX, offsetY);
            }

            float distBottom = Math.abs(dragging.getY() - (target.getY() + target.getHeight() + RegistryState.PARENT_GAP));
            if (dxOverlap > 0 && distBottom <= RegistryState.PARENT_SNAP_DIST && distBottom < bestDist) {
                float offsetX = parentAxisOffset(dragging.getX() - target.getX());
                float desiredY = target.getY() + target.getHeight() + RegistryState.PARENT_GAP;
                float desiredX = target.getX() + offsetX;
                if (!RegistryDragSession.fitsLinkedBoundsAt(dragging, desiredX, desiredY, screenW, screenH)) continue;
                float offsetY = 0f;
                bestDist = distBottom;
                best = new RegistryState.WidgetAnchorCandidate(target, WidgetAnchorSide.BOTTOM, offsetX, offsetY);
            }

            float distLeft = Math.abs((dragging.getX() + dragging.getWidth()) - (target.getX() - RegistryState.PARENT_GAP));
            if (dyOverlap > 0 && distLeft <= RegistryState.PARENT_SNAP_DIST && distLeft < bestDist) {
                float offsetY = parentAxisOffset(dragging.getY() - target.getY());
                float desiredX = target.getX() - RegistryState.PARENT_GAP - dragging.getWidth();
                float desiredY = target.getY() + offsetY;
                if (!RegistryDragSession.fitsLinkedBoundsAt(dragging, desiredX, desiredY, screenW, screenH)) continue;
                float offsetX = 0f;
                bestDist = distLeft;
                best = new RegistryState.WidgetAnchorCandidate(target, WidgetAnchorSide.LEFT, offsetX, offsetY);
            }

            float distRight = Math.abs(dragging.getX() - (target.getX() + target.getWidth() + RegistryState.PARENT_GAP));
            if (dyOverlap > 0 && distRight <= RegistryState.PARENT_SNAP_DIST && distRight < bestDist) {
                float offsetY = parentAxisOffset(dragging.getY() - target.getY());
                float desiredX = target.getX() + target.getWidth() + RegistryState.PARENT_GAP;
                float desiredY = target.getY() + offsetY;
                if (!RegistryDragSession.fitsLinkedBoundsAt(dragging, desiredX, desiredY, screenW, screenH)) continue;
                float offsetX = 0f;
                bestDist = distRight;
                best = new RegistryState.WidgetAnchorCandidate(target, WidgetAnchorSide.RIGHT, offsetX, offsetY);
            }
        }
        return best;
    }

    public static float parentAxisOffset(float offset) {
        return Math.abs(offset) <= RegistryState.PARENT_ALIGN_SNAP_DIST ? 0f : offset;
    }

    public static float overlap(float a0, float a1, float b0, float b1) {
        float left = Math.max(a0, b0);
        float right = Math.min(a1, b1);
        return right - left;
    }

    public static RegistryState.StickySnap findStickySnap(DraggableHudElement dragging, int screenW, int screenH) {
        float bestX = 0f;
        float bestY = 0f;
        float bestXDist = Float.MAX_VALUE;
        float bestYDist = Float.MAX_VALUE;
        boolean hasX = false;
        boolean hasY = false;

        boolean rightSide = dragging.getX() > screenW * 0.5f;
        for (DraggableHudElement target : RegistryState.WIDGETS) {
            if (target == dragging) continue;
            if (!target.isEnabled()) continue;
            if (target.getWidth() <= 0 || target.getHeight() <= 0) continue;

            if (rightSide) {
                float targetEdge = target.getX() + target.getWidth();
                float dragEdge = dragging.getX() + dragging.getWidth();
                float dist = Math.abs(targetEdge - dragEdge);
                if (dist <= RegistryState.PARENT_SNAP_DIST && dist < bestXDist) {
                    bestXDist = dist;
                    bestX = targetEdge - dragging.getWidth();
                    hasX = true;
                }
            } else {
                float dist = Math.abs(target.getX() - dragging.getX());
                if (dist <= RegistryState.PARENT_SNAP_DIST && dist < bestXDist) {
                    bestXDist = dist;
                    bestX = target.getX();
                    hasX = true;
                }
            }

            float distY = Math.abs(target.getY() - dragging.getY());
            if (distY <= RegistryState.PARENT_SNAP_DIST && distY < bestYDist) {
                bestYDist = distY;
                bestY = target.getY();
                hasY = true;
            }
        }

        float distTop = Math.abs(dragging.getY());
        if (distTop <= RegistryState.PARENT_SNAP_DIST && distTop < bestYDist) {
            bestY = 0f;
            hasY = true;
        }

        return new RegistryState.StickySnap(hasX, hasY, bestX, bestY);
    }

    public static void updateDragAnchors(DraggableHudElement w, int screenW, int screenH, boolean allowWidgetAnchoring) {
        if (w.getWidth() <= 0 || w.getHeight() <= 0) {
            w.clearParentAnchor();
            w.setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, w.getX(), w.getY());
            return;
        }

        if (!allowWidgetAnchoring) {
            w.clearParentAnchor();
            w.setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, w.getX(), w.getY());
            return;
        }

        if (w.supportsWidgetAnchoring()) {
            RegistryState.WidgetAnchorCandidate candidate = findWidgetAnchorCandidate(w, screenW, screenH);
            if (candidate != null) {
                w.setParentAnchor(candidate.parent().getId(), candidate.side(), candidate.offsetX(), candidate.offsetY());
                return;
            }
        }
        w.clearParentAnchor();

        float x = w.getX();
        float y = w.getY();

        HudAnchorX ax = HudAnchorX.FREE;
        float offX = x;
        float leftDist = Math.abs(x);
        float rightDist = Math.abs((x + w.getWidth()) - screenW);
        float centerDistX = Math.abs((x + w.getWidth() * 0.5f) - screenW * 0.5f);
        if (leftDist <= RegistryState.SNAP_DIST) {
            ax = HudAnchorX.LEFT;
            offX = x;
        } else if (rightDist <= RegistryState.SNAP_DIST) {
            ax = HudAnchorX.RIGHT;
            offX = rightDist;
        } else if (centerDistX <= RegistryState.SNAP_DIST) {
            ax = HudAnchorX.CENTER;
            offX = (x + w.getWidth() * 0.5f) - screenW * 0.5f;
        }

        HudAnchorY ay = HudAnchorY.FREE;
        float offY = y;
        float topDist = Math.abs(y);
        float bottomDist = Math.abs((y + w.getHeight()) - screenH);
        float centerDistY = Math.abs((y + w.getHeight() * 0.5f) - screenH * 0.5f);
        if (topDist <= RegistryState.SNAP_DIST) {
            ay = HudAnchorY.TOP;
            offY = y;
        } else if (bottomDist <= RegistryState.SNAP_DIST) {
            ay = HudAnchorY.BOTTOM;
            offY = bottomDist;
        } else if (centerDistY <= RegistryState.SNAP_DIST) {
            ay = HudAnchorY.CENTER;
            offY = (y + w.getHeight() * 0.5f) - screenH * 0.5f;
        }

        w.setAnchors(ax, ay, offX, offY);
    }
}
