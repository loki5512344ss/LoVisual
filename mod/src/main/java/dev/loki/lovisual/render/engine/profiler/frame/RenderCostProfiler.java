package dev.loki.lovisual.render.engine.profiler.frame;

import java.util.List;
import dev.loki.lovisual.render.engine.profiler.bridge.DevProfilerBridge;

public enum RenderCostProfiler {
    ;
    private static final boolean DEV = DevProfilerBridge.available("RenderCostProfiler");

    public static void beginFrame(long frameId) {
        if (!DEV) return;
        DevProfilerBridge.invoke("RenderCostProfiler", "beginFrame", new Class<?>[]{long.class}, frameId);
    }

    public static void endFrame() {
        if (!DEV) return;
        DevProfilerBridge.invoke("RenderCostProfiler", "endFrame", new Class<?>[0]);
    }

    public static Snapshot snapshot() {
        return Snapshot.EMPTY;
    }

    public static boolean isEnabled() {
        if (!DEV) return false;
        return DevProfilerBridge.bool("RenderCostProfiler", "isEnabled", false, new Class<?>[0]);
    }

    /**
     * Single static-field read for call sites that build concatenated labels
     * ("graph:" + phase, "3d:module:" + name) every frame — the string work is
     * free when no dev profiler jar is attached.
     */
    public static boolean profiling() {
        return DEV;
    }

    public static boolean isConfigured() {
        if (!DEV) return false;
        return DevProfilerBridge.bool("RenderCostProfiler", "isConfigured", false, new Class<?>[0]);
    }

    public static Scope scope(String domain, String name) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "scope",
                new Class<?>[]{String.class, String.class}, domain, name));
    }

    public static Scope phase(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "phase", new Class<?>[]{String.class}, label));
    }

    public static Scope uiNode(Object label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "uiNode", new Class<?>[]{Object.class}, label));
    }

    public static Scope uiRuntime(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "uiRuntime", new Class<?>[]{String.class}, label));
    }

    public static Scope uiEffect(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "uiEffect", new Class<?>[]{String.class}, label));
    }

    public static Scope postPass(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "postPass", new Class<?>[]{String.class}, label));
    }

    /** Two-part label: concatenation happens only when a dev profiler is attached. */
    public static Scope postPass(String prefix, String suffix) {
        return DEV ? postPass(prefix + suffix) : Scope.NOOP;
    }

    public static Scope rhiDraw(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "rhiDraw", new Class<?>[]{String.class}, label));
    }

    public static Scope itemRender(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "itemRender", new Class<?>[]{String.class}, label));
    }

    public static Scope worldEffect(String label) {
        if (!DEV) return Scope.NOOP;
        return new Scope(DevProfilerBridge.closeable("RenderCostProfiler", "worldEffect", new Class<?>[]{String.class}, label));
    }

    public static List<String> debugLines(String title, String domainPrefix, int limit, double minMs) {
        if (!DEV) return List.of();
        return DevProfilerBridge.lines("RenderCostProfiler", "debugLines",
                new Class<?>[]{String.class, String.class, int.class, double.class},
                title, domainPrefix, limit, minMs);
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

    public record Snapshot(long frameId, List<Entry> entries) {
        public static final Snapshot EMPTY = new Snapshot(Long.MIN_VALUE, List.of());
    }

    public record Entry(String domain,
                        String owner,
                        String name,
                        long calls,
                        long totalNs,
                        long maxNs,
                        long drawCalls,
                        long meshUploads,
                        long fullscreenPasses,
                        long fastCopies,
                        long shaderCopies,
                        long uploadedBytes,
                        long uniformWrites,
                        long uniformBytes) {
    }
}
