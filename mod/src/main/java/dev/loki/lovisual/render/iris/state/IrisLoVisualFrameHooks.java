package dev.loki.lovisual.render.iris.state;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.render.iris.scene.IrisFinalizedSceneRenderer;

public enum IrisLoVisualFrameHooks {
    ;

    private static DeltaTracker currentTickCounter;
    private static boolean renderedAfterFinalization;
    private static boolean rendering;
    private static boolean loggedFrameHook;
    private static boolean loggedMissingFrame;
    private static boolean loggedFinalRender;

    public static void beginRenderLevel(DeltaTracker tickCounter) {
        currentTickCounter = tickCounter;
        renderedAfterFinalization = false;
        if (!loggedFrameHook) {
            loggedFrameHook = true;
            DebugLog.info("[IrisCompat] GameRenderer.renderLevel frame hook active");
        }
    }

    public static void endRenderLevel(DeltaTracker tickCounter) {
        if (currentTickCounter == tickCounter) {
            currentTickCounter = null;
        }
        renderedAfterFinalization = false;
        rendering = false;
    }

    public static boolean isRenderingAfterIrisFinalization() {
        return rendering;
    }

    public static void renderAfterIrisFinalization() {
        if (rendering || renderedAfterFinalization || currentTickCounter == null) {
            if (currentTickCounter == null && !loggedMissingFrame) {
                loggedMissingFrame = true;
                DebugLog.info("[IrisCompat] Iris final pass hook fired without active GameRenderer frame");
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !(mc.gameRenderer instanceof IrisFinalizedSceneRenderer renderer)) {
            return;
        }

        renderedAfterFinalization = true;
        rendering = true;
        if (!loggedFinalRender) {
            loggedFinalRender = true;
            DebugLog.info("[IrisCompat] rendering LoVisual after Iris final pass");
        }
        try {
            renderer.lovisual$renderAfterIrisFinalization(currentTickCounter);
        } finally {
            rendering = false;
        }
    }
}
