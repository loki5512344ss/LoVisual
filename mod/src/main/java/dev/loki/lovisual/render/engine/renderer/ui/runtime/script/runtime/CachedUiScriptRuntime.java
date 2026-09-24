package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime;

import java.util.Map;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;

/**
 * Caches script output as runtime trees.
 *
 * <p>JS execution, object conversion, style resolving, reconciliation, and layout
 * are skipped while the caller-provided signatures are unchanged. The render pass
 * still runs every frame because it writes the already baked tree to the current
 * draw context.</p>
 *
 * <p>This is the public API surface; the JS/layout/patch execution lives in
 * {@code impl.UiScriptRuntimeCore}, hashing in {@code impl.UiScriptSignature} and telemetry in
 * {@code impl.UiScriptFrameStats}.</p>
 */
public final class CachedUiScriptRuntime {
    private final UiScriptRuntimeCore core;

    public CachedUiScriptRuntime() {
        this(Reporter.NOOP);
    }

    public CachedUiScriptRuntime(Reporter reporter) {
        this.core = new UiScriptRuntimeCore(reporter != null ? reporter : Reporter.NOOP);
    }

    public static void resetFrameStats() {
        UiScriptFrameStats.GLOBAL.reset();
    }

    public static FrameStatsSnapshot frameStatsSnapshot() {
        return UiScriptFrameStats.GLOBAL.snapshot();
    }

    public static long signature(Map<String, ?> values) {
        return UiScriptSignature.signature(values);
    }

    public static long mix(long hash, boolean value) {
        return UiScriptSignature.mix(hash, value);
    }

    public static long mix(long hash, int value) {
        return UiScriptSignature.mix(hash, value);
    }

    public static long mix(long hash, float value) {
        return UiScriptSignature.mix(hash, value);
    }

    public static long mix(long hash, String value) {
        return UiScriptSignature.mix(hash, value);
    }

    public static long mixObject(long hash, Object value) {
        return UiScriptSignature.mixObject(hash, value);
    }

    public UiRuntime bake(UiScriptModuleHandle handle,
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
                          PropsFactory propsFactory) {
        return core.bake(handle, module, key, treeSignature, 0L, layoutSignature, scriptWidth, scriptHeight,
                textRenderer, layoutX, layoutY, layoutW, layoutH, propsFactory, null, null);
    }

    public UiRuntime bake(UiScriptModuleHandle handle,
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
                          PropsFactory propsFactory,
                          RuntimePatchFactory patchFactory) {
        return core.bake(handle, module, key, treeSignature, dataSignature, layoutSignature, scriptWidth, scriptHeight,
                textRenderer, layoutX, layoutY, layoutW, layoutH, propsFactory, patchFactory, null);
    }

    public UiRuntime bake(UiScriptModuleHandle handle,
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
                          PropsFactory propsFactory,
                          RuntimePatchFactory patchFactory,
                          BoundsPatchFactory boundsPatchFactory) {
        return core.bake(handle, module, key, treeSignature, dataSignature, layoutSignature, scriptWidth, scriptHeight,
                textRenderer, layoutX, layoutY, layoutW, layoutH, propsFactory, patchFactory, boundsPatchFactory);
    }

    public UiRuntime updatePersistent(UiScriptModuleHandle handle,
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
                                      PropsFactory templatePropsFactory,
                                      RuntimePatchFactory patchFactory) {
        return updatePersistent(handle, module, key, treeSignature, layoutSignature, scriptWidth, scriptHeight,
                textRenderer, layoutX, layoutY, layoutW, layoutH, templatePropsFactory, patchFactory, null);
    }

    public UiRuntime updatePersistent(UiScriptModuleHandle handle,
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
                                      PropsFactory templatePropsFactory,
                                      RuntimePatchFactory patchFactory,
                                      BoundsPatchFactory boundsPatchFactory) {
        return core.updatePersistent(handle, module, key, treeSignature, layoutSignature, scriptWidth, scriptHeight,
                textRenderer, layoutX, layoutY, layoutW, layoutH, templatePropsFactory, patchFactory, boundsPatchFactory);
    }

    public void reset() {
        core.reset();
    }

    public interface Reporter {
        Reporter NOOP = new Reporter() {
            @Override
            public void reportRuntimeError(UiScriptModuleHandle handle, UiScriptRuntimeError error) {
            }

            @Override
            public void reportRenderState(UiScriptModuleHandle handle, UiRuntime runtime, float width, float height) {
            }
        };

        void reportRuntimeError(UiScriptModuleHandle handle, UiScriptRuntimeError error);

        void reportRenderState(UiScriptModuleHandle handle, UiRuntime runtime, float width, float height);
    }

    public interface PropsFactory {
        Map<String, ?> create();
    }

    public interface RuntimePatchFactory {
        Map<String, ? extends Map<String, ?>> create();
    }

    public interface BoundsPatchFactory {
        Map<String, UiBounds> create();
    }

    public record FrameStatsSnapshot(int bakeCalls,
                                     int jsRenders,
                                     int propPatchPasses,
                                     int propPatches,
                                     int boundsPatchPasses,
                                     int boundsPatches,
                                     int layoutPasses,
                                     int blockedCalls,
                                     int failedRenders) {
    }
}
