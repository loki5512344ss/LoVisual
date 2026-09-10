/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.profiler.frame;

import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.rhi.stats.RhiStatsSnapshot;
import dev.loki.lovisual.render.engine.rhi.uniform.stats.UniformAllocatorStatsSnapshot;
import dev.loki.lovisual.render.engine.profiler.bridge.DevProfilerBridge;

public enum RenderFrameProfiler {
    ;
    private static final boolean DEV = DevProfilerBridge.available("RenderFrameProfiler");

    public static void beginFrame(long frameId) {
        if (!DEV) return;
        DevProfilerBridge.invoke("RenderFrameProfiler", "beginFrame", new Class<?>[]{long.class}, frameId);
    }

    public static PhaseScope phase(RenderPhase phase, String label) {
        if (!DEV) return PhaseScope.NOOP;
        return new PhaseScope(DevProfilerBridge.closeable("RenderFrameProfiler", "phase",
                new Class<?>[]{RenderPhase.class, String.class}, phase, label));
    }

    public static void endFrame(RhiStatsSnapshot rhi, UniformAllocatorStatsSnapshot uniforms) {
        if (!DEV) return;
        DevProfilerBridge.invoke("RenderFrameProfiler", "endFrame",
                new Class<?>[]{RhiStatsSnapshot.class, UniformAllocatorStatsSnapshot.class}, rhi, uniforms);
    }

    public static final class PhaseScope implements AutoCloseable {
        private static final PhaseScope NOOP = new PhaseScope(null);
        private final AutoCloseable delegate;

        private PhaseScope(AutoCloseable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            DevProfilerBridge.close(delegate);
        }
    }
}
