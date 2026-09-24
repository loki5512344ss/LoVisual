package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime;

import java.util.LinkedHashMap;
import net.minecraft.util.Util;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine.UiScriptEngine;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine.UiScriptEngineProvider;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderResult;

/**
 * Execution engine behind {@link CachedUiScriptRuntime}: owns the per-key {@link State} cache, the
 * script engine handle and frame clock, and runs the JS-render / layout / prop-patch / bounds-patch
 * passes. {@code bake} rebuilds the tree when its signature changes; {@code updatePersistent} keeps a
 * persistent tree and patches props in place.
 */
final class UiScriptRuntimeCore {
    private static final UiScriptFrameStats FRAME_STATS = UiScriptFrameStats.GLOBAL;

    private final LinkedHashMap<String, State> states = new LinkedHashMap<>();
    private final CachedUiScriptRuntime.Reporter reporter;
    private UiScriptEngine scriptEngine = UiScriptEngineProvider.javet();
    private long frame;
    private long lastFrameMs;

    UiScriptRuntimeCore(CachedUiScriptRuntime.Reporter reporter) {
        this.reporter = reporter;
    }

    UiRuntime bake(UiScriptModuleHandle handle,
                   UiScriptModule module,
                   String key,
                   long treeSignature,
                   long dataSignature,
                   long layoutSignature,
                   float scriptWidth,
                   float scriptHeight,
                   TextRenderer textRenderer,
                   float layoutX,
                   float layoutY,
                   float layoutW,
                   float layoutH,
                   CachedUiScriptRuntime.PropsFactory propsFactory,
                   CachedUiScriptRuntime.RuntimePatchFactory patchFactory,
                   CachedUiScriptRuntime.BoundsPatchFactory boundsPatchFactory) {
        FRAME_STATS.bakeCalls++;
        if (handle == null || module == null || textRenderer == null || propsFactory == null) return null;
        if (handle.isRuntimeBlocked()) {
            FRAME_STATS.blockedCalls++;
            return null;
        }

        State state = state(key);
        state.runtime.diagnostics().counters().setPatchNanos(0L);
        state.runtime.diagnostics().counters().setPropPatchCount(0);
        state.runtime.diagnostics().counters().setBoundsPatchCount(0);
        if (!state.treeReady || state.treeSignature != treeSignature) {
            long now = Util.getMillis();
            double delta = lastFrameMs > 0L ? Math.max(0.0, (now - lastFrameMs) / 1000.0) : 0.0;
            lastFrameMs = now;

            UiScriptRenderResult result;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("js:" + runtimeLabel(handle, key))) {
                result = scriptEngine.render(
                        module,
                        new UiScriptRenderContext(
                                frame++,
                                now / 1000.0,
                                delta,
                                scriptWidth,
                                scriptHeight,
                                new UiProps(propsFactory.create())
                        )
                );
            }
            if (!result.success()) {
                FRAME_STATS.failedRenders++;
                reporter.reportRuntimeError(handle, result.error());
                return null;
            }
            FRAME_STATS.jsRenders++;
            state.runtime.setTree(result.root());
            state.treeSignature = treeSignature;
            state.dataSignature = dataSignature;
            state.layoutReady = false;
            state.treeReady = true;
        } else if (state.dataSignature != dataSignature) {
            if (patchFactory != null) {
                FRAME_STATS.propPatchPasses++;
                try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("patchProps:" + runtimeLabel(handle, key))) {
                    FRAME_STATS.propPatches += state.runtime.patchPropsByKey(patchFactory.create());
                }
            }
            state.dataSignature = dataSignature;
        }

        if (!state.layoutReady || state.layoutSignature != layoutSignature) {
            FRAME_STATS.layoutPasses++;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("layout:" + runtimeLabel(handle, key))) {
                state.runtime.layout(textRenderer, layoutX, layoutY, layoutW, layoutH);
            }
            state.layoutSignature = layoutSignature;
            state.layoutReady = true;
        }

        if (boundsPatchFactory != null) {
            FRAME_STATS.boundsPatchPasses++;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("patchBounds:" + runtimeLabel(handle, key))) {
                FRAME_STATS.boundsPatches += state.runtime.patchBoundsByKey(boundsPatchFactory.create());
            }
        }

        reporter.reportRenderState(handle, state.runtime, scriptWidth, scriptHeight);
        return state.runtime;
    }

    UiRuntime updatePersistent(UiScriptModuleHandle handle,
                               UiScriptModule module,
                               String key,
                               long treeSignature,
                               long layoutSignature,
                               float scriptWidth,
                               float scriptHeight,
                               TextRenderer textRenderer,
                               float layoutX,
                               float layoutY,
                               float layoutW,
                               float layoutH,
                               CachedUiScriptRuntime.PropsFactory templatePropsFactory,
                               CachedUiScriptRuntime.RuntimePatchFactory patchFactory,
                               CachedUiScriptRuntime.BoundsPatchFactory boundsPatchFactory) {
        if (handle == null || module == null || textRenderer == null || templatePropsFactory == null) return null;
        if (handle.isRuntimeBlocked()) {
            FRAME_STATS.blockedCalls++;
            return null;
        }

        State state = state(key);
        state.runtime.diagnostics().counters().setPatchNanos(0L);
        state.runtime.diagnostics().counters().setPropPatchCount(0);
        state.runtime.diagnostics().counters().setBoundsPatchCount(0);
        if (!state.treeReady || state.treeSignature != treeSignature) {
            FRAME_STATS.bakeCalls++;
            long now = Util.getMillis();
            double delta = lastFrameMs > 0L ? Math.max(0.0, (now - lastFrameMs) / 1000.0) : 0.0;
            lastFrameMs = now;

            UiScriptRenderResult result;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("js:" + runtimeLabel(handle, key))) {
                result = scriptEngine.render(
                        module,
                        new UiScriptRenderContext(
                                frame++,
                                now / 1000.0,
                                delta,
                                scriptWidth,
                                scriptHeight,
                                new UiProps(templatePropsFactory.create())
                        )
                );
            }
            if (!result.success()) {
                FRAME_STATS.failedRenders++;
                reporter.reportRuntimeError(handle, result.error());
                return null;
            }
            FRAME_STATS.jsRenders++;
            state.runtime.setTree(result.root());
            state.treeSignature = treeSignature;
            state.layoutReady = false;
            state.treeReady = true;
        }

        if (!state.layoutReady || state.layoutSignature != layoutSignature) {
            FRAME_STATS.layoutPasses++;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("layout:" + runtimeLabel(handle, key))) {
                state.runtime.layout(textRenderer, layoutX, layoutY, layoutW, layoutH);
            }
            state.layoutSignature = layoutSignature;
            state.layoutReady = true;
        }

        if (patchFactory != null) {
            FRAME_STATS.propPatchPasses++;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("patchProps:" + runtimeLabel(handle, key))) {
                FRAME_STATS.propPatches += state.runtime.patchPropsByKey(patchFactory.create());
            }
        }

        if (boundsPatchFactory != null) {
            FRAME_STATS.boundsPatchPasses++;
            try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiRuntime("patchBounds:" + runtimeLabel(handle, key))) {
                FRAME_STATS.boundsPatches += state.runtime.patchBoundsByKey(boundsPatchFactory.create());
            }
        }

        reporter.reportRenderState(handle, state.runtime, scriptWidth, scriptHeight);
        return state.runtime;
    }

    void reset() {
        try {
            scriptEngine.close();
        } catch (Exception ignored) {
        }
        scriptEngine = UiScriptEngineProvider.javet();
        states.clear();
        lastFrameMs = 0L;
    }

    private State state(String key) {
        String resolved = key != null && !key.isEmpty() ? key : "default";
        State existing = states.get(resolved);
        if (existing != null) return existing;
        State created = new State();
        states.put(resolved, created);
        return created;
    }

    private static String runtimeLabel(UiScriptModuleHandle handle, String key) {
        String id = handle != null && handle.getId() != null ? handle.getId().toString() : "unknown";
        String k = key != null && !key.isBlank() ? key : "default";
        return id + ":" + k;
    }

    private static final class State {
        private final UiRuntime runtime = new UiRuntime();
        private long treeSignature = Long.MIN_VALUE;
        private long dataSignature = Long.MIN_VALUE;
        private long layoutSignature = Long.MIN_VALUE;
        private boolean treeReady;
        private boolean layoutReady;
    }
}
