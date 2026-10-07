package dev.loki.lovisual.features.gui.hud.draggable.interaction;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.quick.HudQuickSettings;
import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorX;
import dev.loki.lovisual.features.gui.hud.anchor.HudAnchorY;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragSession;

/**
 * Обработка ввода drag-and-drop виджетов при открытом чате / drag-редакторе.
 */
public final class RegistryDragController {

    private RegistryDragController() {
    }

    public static void handleDragging(Minecraft mc, int screenW, int screenH, float uiScale) {
        boolean chatOpen = ClientScreen.current() instanceof ChatScreen;
        boolean dragEditorOpen = RegistryState.forceVisible && ClientScreen.current() != null;
        boolean allowDragging = chatOpen || dragEditorOpen;
        float fbX = (float) mc.mouseHandler.xpos();
        float fbY = (float) mc.mouseHandler.ypos();
        float mx = HudScale.toVirtual(fbX, uiScale);
        float my = HudScale.toVirtual(fbY, uiScale);

        boolean left = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        boolean middle = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (!allowDragging) {
            if (RegistryState.dragging != null) {
                //DebugLog.config("[HUD] Drag cancelled outside chat for %s", RegistryState.dragging.getId());
            }
            if (left && !RegistryState.prevLeft) {
                // DebugLog.config("[HUD] Mouse down ignored (chat not open) at (%.1f, %.1f)", mx, my);
            }
            if (!left && RegistryState.prevLeft) {
                // DebugLog.config("[HUD] Mouse up ignored (chat not open) at (%.1f, %.1f)", mx, my);
            }
            RegistryState.dragging = null;
            RegistryState.draggingWithLeft = false;
            HudQuickSettings.close();
            RegistryDragSession.clearSnapPreview();
            if (RegistryState.hudCursorActive) {
                SystemCursor.reset();
                RegistryState.hudCursorActive = false;
            }
            RegistryState.prevLeft = left;
            RegistryState.prevMiddle = middle;
            RegistryState.prevRight = right;
            return;
        }

        if (handleQuickSettings(mx, my, left, right, screenW, screenH)) {
            RegistryState.prevLeft = left;
            RegistryState.prevMiddle = middle;
            RegistryState.prevRight = right;
            return;
        }

        if (RegistryState.dragging != null && !RegistryWidgetStore.isWidgetVisibleForEditor(RegistryState.dragging)) {
            RegistryState.dragging = null;
            RegistryState.draggingWithLeft = false;
            RegistryDragSession.clearSnapPreview();
        }

        if (left && !RegistryState.prevLeft) {
            //DebugLog.config("[HUD] Mouse down in chat at (%.1f, %.1f)", mx, my);
            for (DraggableHudElement w : RegistryState.WIDGETS) {
                /*DebugLog.config("[HUD] Widget %s bounds x=%.1f y=%.1f w=%.1f h=%.1f contains=%s",
                        w.getId(), w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.contains(mx, my));*/
            }
            DraggableHudElement clickTarget = null;
            long bestClickOrder = Long.MIN_VALUE;
            for (DraggableHudElement w : RegistryState.WIDGETS) {
                if (!RegistryWidgetStore.isWidgetVisibleForEditor(w)) continue;
                if (!w.contains(mx, my)) continue;
                if (!w.isMouseOverInteractive(mx, my)) continue;
                long order = w.getRuntimeRenderOrder();
                if (order > bestClickOrder) {
                    bestClickOrder = order;
                    clickTarget = w;
                }
            }
            if (clickTarget != null) {
                if (clickTarget.onMouseClicked(mx, my, GLFW.GLFW_MOUSE_BUTTON_1)) {
                    RegistryState.dragging = null;
                    RegistryState.draggingWithLeft = false;
                    RegistryDragSession.clearSnapPreview();
                    RegistryState.prevLeft = left;
                    RegistryState.prevMiddle = middle;
                    return;
                }
                // interactive hitbox: do not start drag on left click
                RegistryState.prevLeft = left;
                RegistryState.prevMiddle = middle;
                return;
            } else {
                // start drag with left click if no interactive hitbox
                DraggableHudElement candidate = RegistryWidgetStore.pickDragCandidate(mx, my);
                if (candidate != null) {
                    RegistryState.dragging = candidate;
                    RegistryState.draggingWithLeft = true;
                    RegistryState.dragOffsetX = mx - candidate.getX();
                    RegistryState.dragOffsetY = my - candidate.getY();
                    if (RegistryWidgetStore.isWidgetAnchorModifierDown()) {
                        RegistryWidgetStore.detachChildren(candidate);
                    }
                    RegistryState.dragging.clearParentAnchor();
                    RegistryState.dragging.setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, RegistryState.dragging.getX(), RegistryState.dragging.getY());
                    SystemCursor.set(SystemCursor.CursorType.MOVE);
                    RegistryState.hudCursorActive = true;
                }
            }
        }

        if (middle && !RegistryState.prevMiddle) {
            // start drag
            DraggableHudElement candidate = RegistryWidgetStore.pickDragCandidate(mx, my);
            if (candidate != null) {
                RegistryState.dragging = candidate;
                RegistryState.draggingWithLeft = false;
                RegistryState.dragOffsetX = mx - candidate.getX();
                RegistryState.dragOffsetY = my - candidate.getY();
                if (RegistryWidgetStore.isWidgetAnchorModifierDown()) {
                    RegistryWidgetStore.detachChildren(candidate);
                }
                RegistryState.dragging.clearParentAnchor();
                RegistryState.dragging.setAnchors(HudAnchorX.FREE, HudAnchorY.FREE, RegistryState.dragging.getX(), RegistryState.dragging.getY());
                //DebugLog.config("[HUD] Drag start for %s at (%.1f, %.1f)", RegistryState.dragging.getId(), RegistryState.dragging.getX(), RegistryState.dragging.getY());
                SystemCursor.set(SystemCursor.CursorType.MOVE);
                RegistryState.hudCursorActive = true;
            }
            if (RegistryState.dragging == null) {
                //DebugLog.config("[HUD] No widget found under cursor at (%.1f, %.1f); widgets=%d", mx, my, RegistryState.WIDGETS.size());
            }
        } else if (!middle && RegistryState.prevMiddle && RegistryState.dragging != null && !RegistryState.draggingWithLeft) {
            RegistryDragSession.stopDragging(screenW, screenH);
        } else if (!middle && RegistryState.prevMiddle) {
            //DebugLog.config("[HUD] Mouse up with no active drag at (%.1f, %.1f)", mx, my);
        }

        if (RegistryState.draggingWithLeft && !left && RegistryState.prevLeft && RegistryState.dragging != null) {
            RegistryDragSession.stopDragging(screenW, screenH);
        }

        if (((RegistryState.draggingWithLeft && left) || (!RegistryState.draggingWithLeft && middle)) && RegistryState.dragging != null) {
            RegistryDragSession.moveDraggedWidget(RegistryState.dragging, mx, my, screenW, screenH);
            if (!RegistryWidgetStore.isWidgetAnchorModifierDown()) {
                RegistryDragSession.updateSnapPreview(RegistryState.dragging, screenW, screenH);
            } else {
                RegistryDragSession.clearSnapPreview();
            }
            //DebugLog.config("[HUD] Dragging %s -> (%.1f, %.1f)", RegistryState.dragging.getId(), RegistryState.dragging.getX(), RegistryState.dragging.getY());
            SystemCursor.set(SystemCursor.CursorType.MOVE);
            RegistryState.hudCursorActive = true;
        } else if (RegistryState.dragging == null) {
            RegistryDragSession.clearSnapPreview();
            RegistryDragSession.requestHoverCursor(mx, my);
        }

        RegistryState.prevLeft = left;
        RegistryState.prevMiddle = middle;
    }

    /** Right click on a widget opens its quick settings; true while the popup swallows this frame's input. */
    private static boolean handleQuickSettings(float mx, float my, boolean left, boolean right, int screenW, int screenH) {
        if (right && !RegistryState.prevRight) {
            DraggableHudElement target = topWidgetAt(mx, my);
            if (target != null && RegistryState.dragging == null && HudQuickSettings.open(target, mx, my, screenW, screenH)) return true;
            HudQuickSettings.close();
            return false;
        }
        if (!HudQuickSettings.isOpen()) return false;
        if (left && !RegistryState.prevLeft) {
            if (!HudQuickSettings.contains(mx, my)) {
                HudQuickSettings.close();
                return false;
            }
            HudQuickSettings.click(mx, my);
            return true;
        }
        if (left) HudQuickSettings.drag(mx);
        else HudQuickSettings.release();
        return left && HudQuickSettings.contains(mx, my);
    }

    private static DraggableHudElement topWidgetAt(float mx, float my) {
        DraggableHudElement best = null;
        long bestOrder = Long.MIN_VALUE;
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (!RegistryWidgetStore.isWidgetVisibleForEditor(w) || !w.contains(mx, my)) continue;
            if (w.getRuntimeRenderOrder() > bestOrder) {
                bestOrder = w.getRuntimeRenderOrder();
                best = w;
            }
        }
        return best;
    }
}
