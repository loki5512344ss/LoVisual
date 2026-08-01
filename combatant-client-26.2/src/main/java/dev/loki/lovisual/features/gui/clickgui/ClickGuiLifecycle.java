package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.module.ModuleManager;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.SystemCursor;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

final class ClickGuiLifecycle {

    private ClickGuiLifecycle() {
    }

    static void init() {
        if (ClickGuiRenderer.init) return;
        ClickGuiRenderer.init = true;
        ClickGuiRenderer.activeTabId = ClickGuiRenderer.MODULES_TAB_ID;
        for (ClickGuiTabEntry tab : ClickGuiTabState.currentTabs()) {
            if (ClickGuiRenderer.MODULES_TAB_ID.equals(tab.id())) {
                tab.section().onSelected();
            } else {
                tab.section().onDeselected();
            }
        }
    }

    static void beginOpenAnimation() {
        init();
        ClickGuiRenderer.closingForExit = false;
        ClickGuiRenderer.finishingClose = false;
        ClickGuiRenderer.lastLifecycleAnimNs = System.nanoTime();
        ClickGuiTabState.getActiveSection().onSelected();
    }

    static void beginCloseAnimation() {
        if (ClickGuiRenderer.finishingClose) return;
        init();
        ClickGuiRenderer.closingForExit = true;
        ClickGuiRenderer.lastLifecycleAnimNs = System.nanoTime();
        ClickGuiSearch.deactivate();
        ClickGuiEditorManager.closeTextEditor(false);
        ClickGuiEditorManager.clearInlineText();
        ClickGuiScreenManager.clearPicker();
        for (ClickGuiTabEntry tab : ClickGuiTabState.currentTabs()) {
            tab.section().onDeselected();
        }
        detachScreenForExitOverlay();
        releaseCursorForGameplay();
    }

    private static void detachScreenForExitOverlay() {
        if (Minecraft.getInstance() == null) return;
        if (ClientScreen.current() instanceof ClickGuiScreen cs) {
            ClickGuiRenderer.mainScreen = cs;
        }
        if (ClientScreen.current() instanceof ClickGuiScreen
                || ClientScreen.current() instanceof ClickGuiPickerScreen
                || ClientScreen.current() instanceof ClickGuiEditorScreen) {
            ClientScreen.show(Minecraft.getInstance(), null);
        }
    }

    static boolean isLifecycleActive() {
        return ModuleManager.isEnabled("clickgui") || ClickGuiRenderer.closingForExit || ClickGuiRenderer.lifecycleAnim > 0.001f;
    }

    static boolean isClosingForExit() {
        return ClickGuiRenderer.closingForExit;
    }

    static boolean isInputReady() {
        return ModuleManager.isEnabled("clickgui") && !ClickGuiRenderer.closingForExit && ClickGuiRenderer.lifecycleAnim >= ClickGuiRenderer.INPUT_READY_PROGRESS;
    }

    static void finishCloseImmediately() {
        ClickGuiRenderer.closingForExit = false;
        ClickGuiRenderer.lifecycleAnim = 0.0f;
        onClose();
        ClickGuiScreenManager.clearScreens();
    }

    static void onClose() {
        if (!ClickGuiRenderer.cursorShown) return;
        long handle = Minecraft.getInstance().getWindow().handle();

        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        SystemCursor.invalidate();
        ClickGuiRenderer.cursorShown = false;
        DraggableHudElementRegistry.setForceVisible(false);
        ClickGuiSearch.deactivate();
        ClickGuiEditorManager.closeTextEditor(false);
        ClickGuiEditorManager.clearInlineText();
        ClickGuiScreenManager.clearPicker();

        ClickGuiScreenManager.requestConfigSave();
    }

    static void releaseCursorForGameplay() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().getWindow() == null) return;
        if (ClientScreen.current() == null) {
            try {
                Minecraft.getInstance().mouseHandler.grabMouse();
                SystemCursor.invalidate();
            } catch (Throwable ignored) {
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                SystemCursor.invalidate();
            }
        } else {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            SystemCursor.invalidate();
        }
        ClickGuiRenderer.cursorShown = false;
        DraggableHudElementRegistry.setForceVisible(false);
    }

    static void ensureCursorShown() {
        if (ClickGuiRenderer.cursorShown) return;
        if (Minecraft.getInstance() == null || Minecraft.getInstance().getWindow() == null) return;
        GLFW.glfwSetInputMode(
                Minecraft.getInstance().getWindow().handle(),
                GLFW.GLFW_CURSOR,
                GLFW.GLFW_CURSOR_NORMAL
        );
        SystemCursor.invalidate();
        ClickGuiRenderer.cursorShown = true;
    }

    static void updateLifecycleAnim(long now, boolean opening) {
        float dt = (now - ClickGuiRenderer.lastLifecycleAnimNs) / 1_000_000_000f;
        ClickGuiRenderer.lastLifecycleAnimNs = now;
        if (dt < 0f) dt = 0f;
        float target = opening ? 1.0f : 0.0f;
        float speed = opening ? ClickGuiRenderer.LIFECYCLE_OPEN_SPEED : ClickGuiRenderer.LIFECYCLE_CLOSE_SPEED;
        ClickGuiRenderer.lifecycleAnim = AnimationUtility.approach(ClickGuiRenderer.lifecycleAnim, target, dt, speed);
        ClickGuiRenderer.lifecycleAnim = AnimationUtility.snap(ClickGuiRenderer.lifecycleAnim, target, 0.002f);
    }

    static boolean sectionsClosed() {
        return !ClickGuiTabState.getActiveSection().isVisible();
    }

    static void completeCloseAnimation() {
        if (ClickGuiRenderer.finishingClose) return;
        ClickGuiRenderer.finishingClose = true;
        ClickGuiRenderer.closingForExit = false;
        onClose();
        if (Minecraft.getInstance() != null && (ClientScreen.current() instanceof ClickGuiScreen
                || ClientScreen.current() instanceof ClickGuiPickerScreen
                || ClientScreen.current() instanceof ClickGuiEditorScreen)) {
            ClientScreen.show(Minecraft.getInstance(), null);
        }
        ClickGuiScreenManager.clearScreens();
        ClickGuiRenderer.finishingClose = false;
    }
}
