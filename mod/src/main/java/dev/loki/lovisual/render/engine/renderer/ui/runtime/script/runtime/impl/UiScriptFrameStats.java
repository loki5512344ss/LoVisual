package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime;

/**
 * Mutable per-frame counters for {@link CachedUiScriptRuntime} (debug telemetry only). Fields are
 * package-private so the runtime engine can bump them inline; {@link #snapshot()} freezes them into
 * the public {@link CachedUiScriptRuntime.FrameStatsSnapshot}.
 */
final class UiScriptFrameStats {
    static final UiScriptFrameStats GLOBAL = new UiScriptFrameStats();

    int bakeCalls;
    int jsRenders;
    int propPatchPasses;
    int propPatches;
    int boundsPatchPasses;
    int boundsPatches;
    int layoutPasses;
    int blockedCalls;
    int failedRenders;

    private UiScriptFrameStats() {
    }

    void reset() {
        bakeCalls = 0;
        jsRenders = 0;
        propPatchPasses = 0;
        propPatches = 0;
        boundsPatchPasses = 0;
        boundsPatches = 0;
        layoutPasses = 0;
        blockedCalls = 0;
        failedRenders = 0;
    }

    CachedUiScriptRuntime.FrameStatsSnapshot snapshot() {
        return new CachedUiScriptRuntime.FrameStatsSnapshot(
                bakeCalls,
                jsRenders,
                propPatchPasses,
                propPatches,
                boundsPatchPasses,
                boundsPatches,
                layoutPasses,
                blockedCalls,
                failedRenders
        );
    }
}
