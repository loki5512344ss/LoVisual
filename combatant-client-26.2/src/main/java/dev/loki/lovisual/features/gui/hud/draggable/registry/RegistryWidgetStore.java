/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.registry;

import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.gui.hud.*;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.module.HudPhase;
import dev.loki.lovisual.util.input.KeyManager;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Хранение и поиск виджетов реестра + предикаты видимости для редактора.
 */
public final class RegistryWidgetStore {

    private RegistryWidgetStore() {
    }

    public static void register(DraggableHudElement widget) {
        if (!RegistryState.WIDGETS.contains(widget)) {
            widget.postInit();
            RegistryState.WIDGETS.add(widget);
            DebugLog.config("[HUD] Registered widget: %s", widget.getId());
        }
    }

    public static List<DraggableHudElement> getWidgets() {
        HudElements.ensureLoaded();
        return Collections.unmodifiableList(RegistryState.WIDGETS);
    }

    public static DraggableHudElement getById(String id) {
        HudElements.ensureLoaded();
        return findById(id);
    }

    public static boolean isEnabled(String id) {
        if (!Lifecycle.canRunHud()) return false;
        DraggableHudElement w = findById(id);
        return w != null && w.isEnabled();
    }

    public static <T extends DraggableHudElement> T get(Class<T> type) {
        if (type == null) return null;
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (type.isInstance(w)) {
                return type.cast(w);
            }
        }
        return null;
    }

    public static boolean isEnabled(Class<? extends DraggableHudElement> type) {
        if (!Lifecycle.canRunHud()) return false;
        DraggableHudElement w = get(type);
        return w != null && w.isEnabled();
    }

    public static void tickAll() {
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            w.onTick();
        }
    }

    public static DraggableHudElement findById(String id) {
        if (id == null) return null;
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (id.equals(w.getId())) return w;
        }
        return null;
    }

    public static void noteRendered(DraggableHudElement w) {
        if (w == null) return;
        w.markRendered(++RegistryState.renderOrderSeq);
    }

    public static boolean isWidgetVisibleForEditor(DraggableHudElement w) {
        if (w == null) return false;

        boolean singleEditorPreview = RegistryState.editorWidgetId != null;
        if (singleEditorPreview && (RegistryState.editorWidgetId.isEmpty() || !RegistryState.editorWidgetId.equals(w.getId()))) {
            return false;
        }

        boolean bypassEnabled = singleEditorPreview && RegistryState.forceVisible;
        if (!bypassEnabled && !w.isEnabled()) {
            return false;
        }

        if (!w.usesEngineRenderer()) return false;
        if (w.getRenderSpace() != HudRenderSpace.UNSCALED_LOGICAL) return false;
        return w.getRuntimeRenderOrder() > 0L;
    }

    public static boolean isRenderableCandidate(DraggableHudElement w, HudPhase phase) {
        if (w == null) return false;

        boolean singleEditorPreview = RegistryState.editorWidgetId != null;
        if (singleEditorPreview && (RegistryState.editorWidgetId.isEmpty() || !RegistryState.editorWidgetId.equals(w.getId()))) {
            return false;
        }

        boolean bypassEnabled = singleEditorPreview && RegistryState.forceVisible;
        if (!bypassEnabled && !w.isEnabled()) {
            return false;
        }

        if (!w.usesEngineRenderer()) return false;
        if (w.getHudPhase() != phase) return false;
        return w.getRenderSpace() == HudRenderSpace.UNSCALED_LOGICAL;
    }

    public static boolean isWidgetVisibleForLinkOverlay(DraggableHudElement w) {
        if (w == null) return false;

        boolean singleEditorPreview = RegistryState.editorWidgetId != null;
        if (singleEditorPreview && (RegistryState.editorWidgetId.isEmpty() || !RegistryState.editorWidgetId.equals(w.getId()))) {
            return false;
        }

        boolean bypassEnabled = singleEditorPreview && RegistryState.forceVisible;
        if (!bypassEnabled && !w.isEnabled()) {
            return false;
        }

        return w.usesEngineRenderer() && w.getRenderSpace() == HudRenderSpace.UNSCALED_LOGICAL;
    }

    public static boolean isDescendantOf(DraggableHudElement possibleDescendant, DraggableHudElement ancestor) {
        if (possibleDescendant == null || ancestor == null) return false;
        String ancestorId = ancestor.getId();
        Set<String> visited = new HashSet<>();
        DraggableHudElement current = possibleDescendant;
        while (current != null && current.getParentId() != null && current.getParentSide() != WidgetAnchorSide.NONE) {
            if (!visited.add(current.getId())) return false;
            if (ancestorId.equals(current.getParentId())) return true;
            current = findById(current.getParentId());
        }
        return false;
    }

    public static void detachChildren(DraggableHudElement parent) {
        String parentId = parent.getId();
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (w == parent) continue;
            if (parentId.equals(w.getParentId()) && w.getParentSide() != WidgetAnchorSide.NONE) {
                w.clearParentAnchor();
                w.setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, w.getX(), w.getY());
                w.layoutValue().set(w.snapshotLayout());
                w.saveConfig();
            }
        }
    }

    public static boolean isWidgetAnchorModifierDown() {
        return KeyManager.isComboHeldAllowScreen("LEFT_ALT")
                || KeyManager.isComboHeldAllowScreen("RIGHT_ALT");
    }

    public static DraggableHudElement pickDragCandidate(float mx, float my) {
        DraggableHudElement candidate = null;
        long bestOrder = Long.MIN_VALUE;
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (!isWidgetVisibleForEditor(w)) continue;
            if (!w.isDraggable() || !w.contains(mx, my)) continue;
            long order = w.getRuntimeRenderOrder();
            if (order > bestOrder) {
                bestOrder = order;
                candidate = w;
            }
        }
        if (candidate == null) {
            for (int i = RegistryState.WIDGETS.size() - 1; i >= 0; i--) {
                DraggableHudElement w = RegistryState.WIDGETS.get(i);
                if (!isWidgetVisibleForEditor(w)) continue;
                if (w.contains(mx, my) && w.isDraggable()) {
                    candidate = w;
                    break;
                }
            }
        }
        return candidate;
    }
}
