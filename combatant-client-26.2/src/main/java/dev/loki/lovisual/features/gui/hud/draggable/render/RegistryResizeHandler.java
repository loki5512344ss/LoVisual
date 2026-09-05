/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.render;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.HudAnchorX;
import dev.loki.lovisual.features.gui.hud.HudAnchorY;
import dev.loki.lovisual.features.gui.hud.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;

/**
 * Реакция раскладки на изменение логического размера экрана.
 */
public final class RegistryResizeHandler {

    private RegistryResizeHandler() {
    }

    public static void handleScreenResize(int screenW, int screenH) {
        if (screenW <= 0 || screenH <= 0) return;
        if (RegistryState.lastScreenW <= 0 || RegistryState.lastScreenH <= 0) {
            RegistryState.lastScreenW = screenW;
            RegistryState.lastScreenH = screenH;
            return;
        }
        if (RegistryState.lastScreenW == screenW && RegistryState.lastScreenH == screenH) return;

        int oldW = RegistryState.lastScreenW;
        int oldH = RegistryState.lastScreenH;
        RegistryState.lastScreenW = screenW;
        RegistryState.lastScreenH = screenH;

        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (!w.isPositionInitialized()) continue;
            if (w.getParentId() != null && w.getParentSide() != WidgetAnchorSide.NONE) {
                // Parent-anchored widgets will be handled when anchors are applied.
                continue;
            }
            if (w.getWidth() <= 0 || w.getHeight() <= 0) continue;

            boolean changed = false;
            if (w.getAnchorX() == HudAnchorX.FREE && w.getAnchorY() == HudAnchorY.FREE) {
                changed |= inferScreenAnchors(w, oldW, oldH);
            }

            RegistryAnchorLayout.applyScreenAnchor(w, screenW, screenH);
            changed |= clampToScreenIfFree(w, screenW, screenH);

            if (changed) {
                w.layoutValue().set(w.snapshotLayout());
                w.saveConfig();
            }
        }
    }

    private static boolean inferScreenAnchors(DraggableHudElement w, int screenW, int screenH) {
        float x = w.getX();
        float y = w.getY();
        boolean changed = false;

        HudAnchorX ax = w.getAnchorX();
        float offX = w.getAnchorOffsetX();
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

        HudAnchorY ay = w.getAnchorY();
        float offY = w.getAnchorOffsetY();
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

        if (ax != w.getAnchorX() || ay != w.getAnchorY()
                || offX != w.getAnchorOffsetX() || offY != w.getAnchorOffsetY()) {
            w.setAnchors(ax, ay, offX, offY);
            changed = true;
        }
        return changed;
    }

    private static boolean clampToScreenIfFree(DraggableHudElement w, int screenW, int screenH) {
        boolean changed = false;
        float newX = w.getX();
        float newY = w.getY();
        if (w.getAnchorX() == HudAnchorX.FREE) {
            float maxX = Math.max(0f, screenW - w.getWidth());
            newX = Math.max(0f, Math.min(newX, maxX));
        }
        if (w.getAnchorY() == HudAnchorY.FREE) {
            float maxY = Math.max(0f, screenH - w.getHeight());
            newY = Math.max(0f, Math.min(newY, maxY));
        }
        if (newX != w.getX() || newY != w.getY()) {
            w.moveTo(newX, newY);
            if (w.getAnchorX() == HudAnchorX.FREE || w.getAnchorY() == HudAnchorY.FREE) {
                w.setAnchors(w.getAnchorX(), w.getAnchorY(),
                        w.getAnchorX() == HudAnchorX.FREE ? newX : w.getAnchorOffsetX(),
                        w.getAnchorY() == HudAnchorY.FREE ? newY : w.getAnchorOffsetY());
            }
            changed = true;
        }
        return changed;
    }
}
