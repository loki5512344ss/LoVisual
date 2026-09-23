/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.profiler.frame;

import java.util.List;
import dev.loki.lovisual.render.engine.profiler.bridge.DevProfilerBridge;

public enum GlSyncTracker {
    ;
    private static final boolean DEV = DevProfilerBridge.available("GlSyncTracker");

    public static void onFence(long sync) {
        if (!DEV) return;
        DevProfilerBridge.invoke("GlSyncTracker", "onFence", new Class<?>[]{long.class}, sync);
    }

    public static void onWaitStart(long sync) {
        if (!DEV) return;
        DevProfilerBridge.invoke("GlSyncTracker", "onWaitStart", new Class<?>[]{long.class}, sync);
    }

    public static void onWaitEnd() {
        if (!DEV) return;
        DevProfilerBridge.invoke("GlSyncTracker", "onWaitEnd", new Class<?>[0]);
    }

    public static void emitTracyFrame() {
        if (!DEV) return;
        DevProfilerBridge.invoke("GlSyncTracker", "emitTracyFrame", new Class<?>[0]);
    }

    public static List<String> drainLines() {
        if (!DEV) return List.of();
        return DevProfilerBridge.lines("GlSyncTracker", "drainLines", new Class<?>[0]);
    }

    public static void reset() {
        if (!DEV) return;
        DevProfilerBridge.invoke("GlSyncTracker", "reset", new Class<?>[0]);
    }
}
