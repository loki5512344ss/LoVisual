/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.profiler.render;

import java.util.List;
import dev.loki.lovisual.render.engine.profiler.bridge.DevProfilerBridge;

public enum RenderProfiler2D {
    ;
    private static final boolean DEV = DevProfilerBridge.available("RenderProfiler2D");

    public static boolean isEnabled() {
        if (!DEV) return false;
        return DevProfilerBridge.bool("RenderProfiler2D", "isEnabled", false, new Class<?>[0]);
    }

    public static void beginFrame(String name) {
        if (!DEV) return;
        DevProfilerBridge.invoke("RenderProfiler2D", "beginFrame", new Class<?>[]{String.class}, name);
    }

    public static boolean beginFrameIfNeeded(String name) {
        if (!DEV) return false;
        return DevProfilerBridge.bool("RenderProfiler2D", "beginFrameIfNeeded", false, new Class<?>[]{String.class}, name);
    }

    public static void endFrame() {
        if (!DEV) return;
        DevProfilerBridge.invoke("RenderProfiler2D", "endFrame", new Class<?>[0]);
    }

    public static Section section(String name) {
        if (!DEV) return Section.NOOP;
        return new Section(DevProfilerBridge.closeable("RenderProfiler2D", "section", new Class<?>[]{String.class}, name));
    }

    /** Two-part label: concatenation happens only when a dev profiler is attached. */
    public static Section section(String prefix, String suffix) {
        return DEV ? section(prefix + suffix) : Section.NOOP;
    }

    public static List<String> getDebugLines() {
        if (!DEV) return List.of();
        return DevProfilerBridge.lines("RenderProfiler2D", "getDebugLines", new Class<?>[0]);
    }

    public static List<String> getDebugLines(int topLimit, int depthLimit, double minMs) {
        if (!DEV) return List.of();
        return DevProfilerBridge.lines("RenderProfiler2D", "getDebugLines",
                new Class<?>[]{int.class, int.class, double.class}, topLimit, depthLimit, minMs);
    }

    public static final class Section implements AutoCloseable {
        private static final Section NOOP = new Section(null);
        private final AutoCloseable delegate;

        private Section(AutoCloseable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            DevProfilerBridge.close(delegate);
        }
    }
}
