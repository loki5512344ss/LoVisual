package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.sections.settings.SettingsTabRuntime;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.util.wav.ClickGuiSounds;

final class ClickGuiInputHandler {

    private ClickGuiInputHandler() {
    }

    static void onMouseMove(double x, double y) {
        updateMouse(x, y, false);
        if (ClickGuiRenderer.picker != null) {
            ClickGuiRenderer.picker.handleMouseMove(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        }
        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
            ClickGuiRenderer.protocolHeuristicsEditor.handleMouseMove(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        }
        if (ClickGuiRenderer.textEditor != null) {
            ClickGuiRenderer.textEditor.handleMouseMove(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        }
    }

    static void onMouseButton(double x, double y, int button, boolean pressed) {
        updateMouse(x, y, false);
        handleMouseButton(button, pressed);
    }

    static void onMouseScroll(double x, double y, double delta) {
        updateMouse(x, y, false);
        handleMouseScroll(delta);
    }

    static void onMouseMoveScaled(double x, double y) {
        updateMouse(x, y, true);
        if (ClickGuiRenderer.picker != null) {
            ClickGuiRenderer.picker.handleMouseMove(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        }
        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
            ClickGuiRenderer.protocolHeuristicsEditor.handleMouseMove(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        }
        if (ClickGuiRenderer.textEditor != null) {
            ClickGuiRenderer.textEditor.handleMouseMove(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        }
    }

    static void onMouseButtonScaled(double x, double y, int button, boolean pressed) {
        updateMouse(x, y, true);
        handleMouseButton(button, pressed);
    }

    static void onMouseScrollScaled(double x, double y, double delta) {
        updateMouse(x, y, true);
        handleMouseScroll(delta);
    }

    static void onEditorMouseMoveScaled(double x, double y) {
        updateMouse(x, y, true);
    }

    static void onEditorMouseButtonScaled(double x, double y, int button, boolean pressed) {
        updateMouse(x, y, true);
        if (ClickGuiRenderer.waitingForKey) {
            if (pressed) ClickGuiKeyBindManager.addPendingMouseButton(button);
            return;
        }
        if (ClickGuiRenderer.textEditor != null) {
            if (pressed) {
                ClickGuiRenderer.textEditor.handleMouseDown(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            } else {
                ClickGuiRenderer.textEditor.handleMouseUp(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            }
            return;
        }
        if (ClickGuiTabState.isSettingsTabActive()) {
            SettingsTabRuntime.handleEditorMouseButton(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button, pressed);
        }
    }

    static void onEditorMouseScrollScaled(double x, double y, double delta) {
        updateMouse(x, y, true);
        if (delta != 0.0) {
            ClickGuiSounds.scroll();
        }
        if (ClickGuiRenderer.textEditor != null) {
            ClickGuiRenderer.textEditor.scroll(delta);
            return;
        }
        if (ClickGuiTabState.isSettingsTabActive()) {
            SettingsTabRuntime.handleEditorMouseScroll(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, delta);
        }
    }

    private static void handleMouseButton(int button, boolean pressed) {
        if (!ClickGuiLifecycle.isInputReady()) return;
        if (ClickGuiRenderer.waitingForKey) {
            if (pressed) ClickGuiKeyBindManager.addPendingMouseButton(button);
            return;
        }

        if (ClickGuiRenderer.textEditor != null) {
            if (pressed) {
                ClickGuiRenderer.textEditor.handleMouseDown(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            } else {
                ClickGuiRenderer.textEditor.handleMouseUp(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            }
            return;
        }

        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
            if (pressed) {
                ClickGuiRenderer.protocolHeuristicsEditor.handleMouseDown(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            } else {
                ClickGuiRenderer.protocolHeuristicsEditor.handleMouseUp(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            }
            return;
        }

        if (pressed && ClickGuiTabBar.handleTabClick(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY)) {
            return;
        }

        if (ClickGuiRenderer.picker != null) {
            if (pressed) {
                ClickGuiRenderer.picker.handleMouseDown(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            } else {
                ClickGuiRenderer.picker.handleMouseUp(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            }
            return;
        }

        if (!pressed) {
            ClickGuiTabState.getActiveSection().mouseReleased(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
            return;
        }

        ClickGuiTabState.getActiveSection().mousePressed(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, button);
    }

    private static void handleMouseScroll(double delta) {
        if (!ClickGuiLifecycle.isInputReady()) return;
        if (delta != 0.0) {
            ClickGuiSounds.scroll();
        }

        if (ClickGuiRenderer.textEditor != null) {
            ClickGuiRenderer.textEditor.scroll(delta);
            return;
        }

        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
            ClickGuiRenderer.protocolHeuristicsEditor.scroll(delta);
            return;
        }

        if (ClickGuiRenderer.picker != null) {
            ClickGuiRenderer.picker.scroll(delta);
            return;
        }

        ClickGuiTabState.getActiveSection().mouseScrolled(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY, delta);
    }

    private static void updateMouse(double x, double y, boolean fromScaledCoords) {
        if (ClickGuiRenderer.MC == null || ClickGuiRenderer.MC.getWindow() == null) {
            ClickGuiRenderer.mouseX = (float) x;
            ClickGuiRenderer.mouseY = (float) y;
            return;
        }
        if (fromScaledCoords) {
            double scaledW = ClickGuiRenderer.MC.getWindow().getGuiScaledWidth();
            double scaledH = ClickGuiRenderer.MC.getWindow().getGuiScaledHeight();
            double fbW = ClickGuiRenderer.MC.getWindow().getWidth();
            double fbH = ClickGuiRenderer.MC.getWindow().getHeight();
            double sx = scaledW > 0 ? fbW / scaledW : ClickGuiRenderer.MC.getWindow().getGuiScale();
            double sy = scaledH > 0 ? fbH / scaledH : ClickGuiRenderer.MC.getWindow().getGuiScale();
            double fbX = x * sx;
            double fbY = y * sy;
            float uiScale = HudScale.scale((int) fbW, (int) fbH);
            ClickGuiRenderer.mouseX = (float) (fbX / uiScale);
            ClickGuiRenderer.mouseY = (float) (fbY / uiScale);
            return;
        }
        double scale = ClickGuiRenderer.MC.getWindow().getGuiScale();
        double fbW = ClickGuiRenderer.MC.getWindow().getWidth();
        double fbH = ClickGuiRenderer.MC.getWindow().getHeight();
        double fbX = x * scale;
        double fbY = y * scale;
        float uiScale = HudScale.scale((int) fbW, (int) fbH);
        ClickGuiRenderer.mouseX = (float) (fbX / uiScale);
        ClickGuiRenderer.mouseY = (float) (fbY / uiScale);
    }
}
