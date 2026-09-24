package dev.loki.lovisual.render.iris.runtime;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import dev.loki.lovisual.render.LoVisualEntityRenderTypes;
import dev.loki.lovisual.render.iris.compat.IrisCompatibilityProfile;
import dev.loki.lovisual.render.iris.compat.IrisCompatibilityProfiles;
import dev.loki.lovisual.render.iris.runtime.IrisRuntimeSnapshot;

public enum IrisRuntimeBridge {
    ;

    static IrisRuntimeSnapshot snapshot() {
        IrisApi api = IrisApi.getInstance();
        String packName = currentPackName();
        boolean shaderpackInUse = probe(api::isShaderPackInUse, packName != null && !packName.isBlank());
        boolean shadersEnabled = shadersEnabled(api, shaderpackInUse);
        boolean renderingShadowPass = probe(api::isRenderingShadowPass, false);
        IrisCompatibilityProfile profile = IrisCompatibilityProfiles.resolve(packName, shaderpackInUse);

        return new IrisRuntimeSnapshot(
                true,
                true,
                shadersEnabled,
                shaderpackInUse,
                renderingShadowPass,
                packName,
                profile,
                "ok"
        );
    }

    static void registerLoVisualPipelines() {
        IrisApi api = IrisApi.getInstance();
        java.util.List<RenderPipeline> pipelines = LoVisualEntityRenderTypes.irisMappedPipelines();
        for (RenderPipeline pipeline : pipelines) {
            assign(api, pipeline, IrisProgram.ENTITIES_TRANSLUCENT);
        }
    }

    private static void assign(IrisApi api, RenderPipeline pipeline, IrisProgram program) {
        if (api == null || pipeline == null || program == null) return;
        try {
            api.assignPipeline(pipeline, program);
        } catch (IllegalStateException ignored) {
            // Iris keeps pipeline assignments globally; resource reloads can call this more than once.
        }
    }

    private static boolean shadersEnabled(IrisApi api, boolean shaderpackInUse) {
        try {
            if (api.getConfig() != null) {
                return api.getConfig().areShadersEnabled();
            }
        } catch (LinkageError | RuntimeException ignored) {
            // Iris can expose a not-yet-initialized config during early LoVisual module/config loading.
        }
        return shaderpackInUse;
    }

    private static String currentPackName() {
        try {
            String value = Iris.getCurrentPackName();
            return value == null ? "" : value;
        } catch (LinkageError | RuntimeException ignored) {
            return "";
        }
    }

    private static boolean probe(BooleanProbe probe, boolean fallback) {
        try {
            return probe.getAsBoolean();
        } catch (LinkageError | RuntimeException ignored) {
            return fallback;
        }
    }

    @FunctionalInterface
    private interface BooleanProbe {
        boolean getAsBoolean();
    }
}
