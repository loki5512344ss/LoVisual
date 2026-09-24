package dev.loki.lovisual.render.engine.profiler.render;

import dev.loki.lovisual.util.logging.DebugLog;

public enum ProfilerLog {
    ;

    static void info(String message, Object... args) {
        DebugLog.renderThread("[Profiler] " + message, args);
    }

    static void warn(String message, Object... args) {
        DebugLog.warn("[Profiler] " + message, args);
    }
}
