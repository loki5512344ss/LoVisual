package dev.loki.lovisual.render.iris.runtime;

import net.fabricmc.loader.api.FabricLoader;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.render.iris.compat.IrisCompatibilityFeature;
import dev.loki.lovisual.render.iris.compat.IrisCompatibilityProfile;
import dev.loki.lovisual.render.iris.runtime.IrisRuntimeBridge;
import dev.loki.lovisual.render.iris.runtime.IrisRuntimeSnapshot;

public enum IrisRuntime {
    ;
    private static volatile boolean loggedApiFailure;

    public static IrisRuntimeSnapshot snapshot() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return IrisRuntimeSnapshot.UNLOADED;
        }

        try {
            return IrisRuntimeBridge.snapshot();
        } catch (LinkageError | RuntimeException t) {
            if (!loggedApiFailure) {
                loggedApiFailure = true;
                DebugLog.warn("[IrisCompat] Iris runtime probe failed: " + t);
            }
            return loadedApiUnavailable(t.getClass().getSimpleName());
        }
    }

    public static boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("iris");
    }

    public static boolean isShaderpackRendererActive() {
        IrisRuntimeSnapshot snapshot = snapshot();
        return snapshot.modLoaded() && snapshot.apiAvailable() && snapshot.shadersEnabled() && snapshot.shaderpackInUse();
    }

    public static boolean isRenderingShadowPass() {
        IrisRuntimeSnapshot snapshot = snapshot();
        return snapshot.modLoaded() && snapshot.apiAvailable() && snapshot.renderingShadowPass();
    }

    public static void registerLoVisualPipelines() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return;
        }

        try {
            IrisRuntimeBridge.registerLoVisualPipelines();
        } catch (LinkageError | RuntimeException t) {
            if (!loggedApiFailure) {
                loggedApiFailure = true;
                DebugLog.warn("[IrisCompat] LoVisual pipeline registration failed: " + t);
            }
        }
    }

    public static boolean supports(IrisCompatibilityFeature feature) {
        return snapshot().profile().supports(feature);
    }

    private static IrisRuntimeSnapshot loadedApiUnavailable(String status) {
        return new IrisRuntimeSnapshot(
                true,
                false,
                false,
                false,
                false,
                "",
                IrisCompatibilityProfile.NONE,
                status == null || status.isBlank() ? "api unavailable" : status
        );
    }
}
