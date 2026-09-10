/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.registry;

import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorX;
import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorY;
import dev.loki.lovisual.features.gui.hud.anchor.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableLayoutValue;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableLayoutValue;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;

import java.util.*;

/** Применение якорей и инициализация сохранённых позиций виджетов. */
public final class RegistryAnchorLayout {

    private RegistryAnchorLayout() {
    }

    public static void ensurePositionInitialized(DraggableHudElement w, int screenW, int screenH) {
        if (w.isPositionInitialized()) return;
        DraggableLayoutValue.State saved = w.layoutValue().get();
        if (saved != null) {
            DraggableLayoutValue.State resolved = saved;
            if (!saved.logical()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getWindow() != null) {
                    int fbw = mc.getWindow().getWidth();
                    int fbh = mc.getWindow().getHeight();
                    float uiScale = HudScale.scale(fbw, fbh);
                    float nx = HudScale.toVirtual(saved.x(), uiScale);
                    float ny = HudScale.toVirtual(saved.y(), uiScale);
                    float npx = HudScale.toVirtual(saved.parentOffsetX(), uiScale);
                    float npy = HudScale.toVirtual(saved.parentOffsetY(), uiScale);
                    resolved = new DraggableLayoutValue.State(
                            nx,
                            ny,
                            saved.anchorX(),
                            saved.anchorY(),
                            saved.parentId(),
                            saved.parentSide(),
                            npx,
                            npy,
                            true
                    );
                    w.layoutValue().set(resolved);
                    w.saveConfig();
                }
            }
            float safeW = w.getWidth() > 0f ? w.getWidth() : 20f;
            float safeH = w.getHeight() > 0f ? w.getHeight() : 20f;
            float clampedX = saved.anchorX() == HudAnchorX.FREE
                    ? Math.max(0, Math.min(resolved.x(), screenW - safeW))
                    : resolved.x();
            float clampedY = saved.anchorY() == HudAnchorY.FREE
                    ? Math.max(0, Math.min(resolved.y(), screenH - safeH))
                    : resolved.y();
            if (saved.anchorX() == HudAnchorX.FREE && saved.anchorY() == HudAnchorY.FREE
                    && (clampedX != resolved.x() || clampedY != resolved.y())) {
                DebugLog.config("[HUD] Saved position clamped for %s: (%.1f, %.1f) -> (%.1f, %.1f)",
                        w.getId(), resolved.x(), resolved.y(), clampedX, clampedY);
            } else {
                DebugLog.config("[HUD] Loaded saved position for %s: (%.1f, %.1f)", w.getId(), resolved.x(), resolved.y());
            }
            w.moveTo(clampedX, clampedY);
            w.setAnchors(resolved.anchorX(), resolved.anchorY(), clampedX, clampedY);
            if (resolved.parentId() != null && resolved.parentSide() != WidgetAnchorSide.NONE) {
                w.setParentAnchor(resolved.parentId(), resolved.parentSide(), resolved.parentOffsetX(), resolved.parentOffsetY());
            } else {
                w.clearParentAnchor();
            }
        } else {
            w.applyDefaultPosition(screenW, screenH);
            w.setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, w.getX(), w.getY());
            w.clearParentAnchor();
            DebugLog.config("[HUD] Applied default position for %s: (%.1f, %.1f)", w.getId(), w.getX(), w.getY());
        }
        w.markPositionInitialized();
    }

    public static void applyAnchors(DraggableHudElement w, int screenW, int screenH) {
        applyAnchors(w, screenW, screenH, new HashSet<>());
    }

    public static void applyAnchors(DraggableHudElement w, int screenW, int screenH, Set<String> visiting) {
        if (w.getParentId() == null || w.getParentSide() == WidgetAnchorSide.NONE) {
            applyScreenAnchor(w, screenW, screenH);
            return;
        }
        DraggableHudElement parent = RegistryWidgetStore.findById(w.getParentId());
        if (parent == null) {
            w.clearParentAnchor();
            applyScreenAnchor(w, screenW, screenH);
            return;
        }
        if (!visiting.add(w.getId())) {
            w.clearParentAnchor();
            applyScreenAnchor(w, screenW, screenH);
            return;
        }
        if (parent != RegistryState.dragging) {
            applyAnchors(parent, screenW, screenH, visiting);
        }
        if (RegistryWidgetStore.isDescendantOf(parent, w)) {
            w.clearParentAnchor();
            applyScreenAnchor(w, screenW, screenH);
        } else {
            applyWidgetAnchor(w, parent, screenW, screenH);
        }
        visiting.remove(w.getId());
    }

    public static void applyScreenAnchor(DraggableHudElement w, int screenW, int screenH) {
        float x = w.getAnchorOffsetX();
        float y = w.getAnchorOffsetY();
        switch (w.getAnchorX()) {
            case LEFT -> x = w.getAnchorOffsetX();
            case CENTER -> x = screenW * 0.5f - w.getWidth() * 0.5f + w.getAnchorOffsetX();
            case RIGHT -> x = screenW - w.getWidth() - w.getAnchorOffsetX();
            case FREE -> x = w.getAnchorOffsetX();
        }
        switch (w.getAnchorY()) {
            case TOP -> y = w.getAnchorOffsetY();
            case CENTER -> y = screenH * 0.5f - w.getHeight() * 0.5f + w.getAnchorOffsetY();
            case BOTTOM -> y = screenH - w.getHeight() - w.getAnchorOffsetY();
            case FREE -> y = w.getAnchorOffsetY();
        }
        w.moveTo(clampX(x, w, screenW), clampY(y, w, screenH));
    }

    public static void applyWidgetAnchor(DraggableHudElement w, DraggableHudElement parent, int screenW, int screenH) {
        float x = w.getX();
        float y = w.getY();
        switch (w.getParentSide()) {
            case TOP -> {
                x = parent.getX() + w.getParentOffsetX();
                y = parent.getY() - w.getHeight() - RegistryState.PARENT_GAP + w.getParentOffsetY();
            }
            case BOTTOM -> {
                x = parent.getX() + w.getParentOffsetX();
                y = parent.getY() + parent.getHeight() + RegistryState.PARENT_GAP + w.getParentOffsetY();
            }
            case LEFT -> {
                x = parent.getX() - w.getWidth() - RegistryState.PARENT_GAP + w.getParentOffsetX();
                y = parent.getY() + w.getParentOffsetY();
            }
            case RIGHT -> {
                x = parent.getX() + parent.getWidth() + RegistryState.PARENT_GAP + w.getParentOffsetX();
                y = parent.getY() + w.getParentOffsetY();
            }
            case NONE -> {
            }
        }
        w.moveTo(clampX(x, w, screenW), clampY(y, w, screenH));
    }

    public static void applyDragAnchorLock(DraggableHudElement w, int screenW, int screenH) {
        if (w.getParentId() != null && w.getParentSide() != WidgetAnchorSide.NONE) {
            DraggableHudElement parent = RegistryWidgetStore.findById(w.getParentId());
            if (parent != null) {
                applyWidgetAnchor(w, parent, screenW, screenH);
            }
            return;
        }
        if (w.getAnchorX() != HudAnchorX.FREE || w.getAnchorY() != HudAnchorY.FREE) {
            applyScreenAnchor(w, screenW, screenH);
        }
    }

    public static void applyDescendantAnchors(DraggableHudElement parent, int screenW, int screenH, Set<String> visited) {
        if (parent == null || !visited.add(parent.getId())) return;
        String parentId = parent.getId();
        for (DraggableHudElement child : RegistryState.WIDGETS) {
            if (child == parent) continue;
            if (!parentId.equals(child.getParentId()) || child.getParentSide() == WidgetAnchorSide.NONE) continue;
            applyWidgetAnchor(child, parent, screenW, screenH);
            applyDescendantAnchors(child, screenW, screenH, visited);
        }
    }

    public static float clampX(float x, DraggableHudElement w, int screenW) {
        float maxX = Math.max(0f, screenW - Math.max(0f, w.getWidth()));
        return clamp(x, 0f, maxX);
    }

    public static float clampY(float y, DraggableHudElement w, int screenH) {
        float maxY = Math.max(0f, screenH - Math.max(0f, w.getHeight()));
        return clamp(y, 0f, maxY);
    }

    public static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(value, max));
    }
}
