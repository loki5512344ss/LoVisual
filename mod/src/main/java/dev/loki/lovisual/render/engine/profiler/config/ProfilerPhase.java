/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.profiler.config;
import dev.loki.lovisual.render.engine.profiler.bridge.DevProfilerBridge;

public enum ProfilerPhase {
    ;
    private static final boolean DEV = DevProfilerBridge.available("ProfilerPhase");

    public static Scope scope(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("ProfilerPhase", "scope", new Class<?>[]{String.class}, label));
    }

    /** Two-part label: concatenation happens only when a dev profiler is attached. */
    public static Scope scope(String prefix, String suffix) {
        return DEV ? scope(prefix + suffix) : Scope.NOOP;
    }

    public static void begin(String label) {
        if (!DEV) return;
        DevProfilerBridge.invoke("ProfilerPhase", "begin", new Class<?>[]{String.class}, label);
    }

    public static void end(String label) {
        if (!DEV) return;
        DevProfilerBridge.invoke("ProfilerPhase", "end", new Class<?>[]{String.class}, label);
    }

    public static String current() {
        if (!DEV) return "unknown";
        return DevProfilerBridge.string("ProfilerPhase", "current", "unknown", new Class<?>[0]);
    }

    public static void clearCurrent() {
        if (!DEV) return;
        DevProfilerBridge.invoke("ProfilerPhase", "clearCurrent", new Class<?>[0]);
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null);
        private final AutoCloseable delegate;

        private Scope(AutoCloseable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            DevProfilerBridge.close(delegate);
        }
    }
}
