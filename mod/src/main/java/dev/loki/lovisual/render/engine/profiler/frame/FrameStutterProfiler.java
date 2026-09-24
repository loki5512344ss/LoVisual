package dev.loki.lovisual.render.engine.profiler.frame;

import dev.loki.lovisual.render.engine.rhi.stats.RhiStatsSnapshot;
import dev.loki.lovisual.render.engine.rhi.resource.manager.RenderResourceStatsSnapshot;
import dev.loki.lovisual.render.engine.rhi.uniform.stats.UniformAllocatorStatsSnapshot;
import dev.loki.lovisual.render.engine.profiler.bridge.DevProfilerBridge;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerSettings;

public enum FrameStutterProfiler {
    ;
    private static final boolean DEV = DevProfilerBridge.available("FrameStutterProfiler");

    public static void configure(ProfilerSettings.OutputMode mode, double thresholdMs, double sampleMs) {
        if (!DEV) return;
        DevProfilerBridge.invoke("FrameStutterProfiler", "configure",
                new Class<?>[]{ProfilerSettings.OutputMode.class, double.class, double.class},
                mode, thresholdMs, sampleMs);
    }

    public static String statusLine() {
        if (!DEV) return "Profiler stutter: dev build required";
        return DevProfilerBridge.string("FrameStutterProfiler", "statusLine", "Profiler stutter: dev build required", new Class<?>[0]);
    }

    public static boolean isEnabled() {
        if (!DEV) return false;
        return DevProfilerBridge.bool("FrameStutterProfiler", "isEnabled", false, new Class<?>[0]);
    }

    public static void onFramePresented(RhiStatsSnapshot rhi,
                                        UniformAllocatorStatsSnapshot uniforms,
                                        RenderResourceStatsSnapshot resources) {
        if (!DEV) return;
        DevProfilerBridge.invoke("FrameStutterProfiler", "onFramePresented",
                new Class<?>[]{RhiStatsSnapshot.class, UniformAllocatorStatsSnapshot.class, RenderResourceStatsSnapshot.class},
                rhi, uniforms, resources);
    }
}
