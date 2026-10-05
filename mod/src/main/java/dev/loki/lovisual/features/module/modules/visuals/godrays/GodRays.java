package dev.loki.lovisual.features.module.modules.visuals.godrays;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.godrays.GodRaysUniforms;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Sun shafts: a radial blur of the bright sky towards the sun, added on top of the scene. Skipped while a
 * shaderpack renders; the sun position comes from the sky state captured by GodRaysSkyMixin.
 */
@ModuleInfo(
        id = "godrays",
        displayName = "God Rays",
        aliases = {"godrays", "sunshafts"},
        category = ModuleCategory.VISUALS
)
public final class GodRays extends Module implements PostProcessPass {
    private static final float SAMPLE_WEIGHT = 1.35f;
    private static final float MIN_FADE = 0.002f;
    private static final int LITE_SAMPLES = 12;

    private final NumberValue<Float> strength = num("godRaysStrength", "strength", 110.0f, 10.0f, 400.0f);
    private final NumberValue<Float> reach = num("godRaysReach", "reach", 65.0f, 20.0f, 100.0f);
    private final NumberValue<Integer> samples = num("godRaysSamples", "samples", 28, 8, 64);
    private final NumberValue<Float> falloff = num("godRaysFalloff", "falloff", 96.0f, 80.0f, 100.0f);
    private final RGBColorValue rayColor = colorNoAlpha("godRaysColor", "color", "#FFE2A8");
    private final BooleanValue sunsetBoost = bool("godRaysSunsetBoost", "sunset_boost", true);

    private final Matrix4f viewProjection = new Matrix4f();
    private boolean failed;

    {
        PostProcessManager.register(this);
    }

    @Override
    public void onEnable() {
        failed = false;
    }

    @Override
    public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return isEnabled() && !failed && GodRaysSun.isValid() && mc.player != null && mc.level != null
                && !IrisRuntime.isShaderpackRendererActive();
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public Phase getPhase() {
        return Phase.PRE_HAND;
    }

    @Override
    public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) {
        return false;
    }

    @Override
    public boolean render(PostProcessContext context, GpuTextureView src, GpuTextureView dst) {
        if (!isActive() || context == null || src == null || dst == null || context.mainDepth() == null) return false;

        Matrix4f projection = LoVisualWorldMatrices.renderProjectionMatrix();
        if (projection == null) projection = RenderState.worldProjection;
        viewProjection.set(projection).mul(new Matrix4f().rotation(new Quaternionf(RenderState.cameraRotation).conjugate()));

        GodRaysSun.Screen sun = GodRaysSun.project(GodRaysSun.angle(), viewProjection);
        if (sun == null) return false;
        float fade = GodRaysSun.fade(sun, Minecraft.getInstance().level.getRainLevel(1.0f), sunsetBoost.get());
        if (fade <= MIN_FADE) return false;

        int glow = GodRaysSun.sunrise();
        int ray = rayColor.getArgb();
        int glowAlpha = glow >>> 24;
        GodRaysUniforms.update(sun.u(), sun.v(), reach.get() * 0.01f,
                OptimizeState.isLitePost() ? LITE_SAMPLES : samples.get(), SAMPLE_WEIGHT,
                strength.get() * 0.01f, falloff.get() * 0.01f,
                GodRaysSun.tintChannel(ray >> 16 & 0xFF, glow >> 16 & 0xFF, glowAlpha),
                GodRaysSun.tintChannel(ray >> 8 & 0xFF, glow >> 8 & 0xFF, glowAlpha),
                GodRaysSun.tintChannel(ray & 0xFF, glow & 0xFF, glowAlpha),
                fade, context.width(), context.height());
        try {
            FullScreenRenderer.ensureInit();
            FullScreenRenderer.begin("LoVisual GodRays Pass")
                    .attachment(dst)
                    .pipeline(LoVisualRenderPipelines.GOD_RAYS)
                    .uniform("GodRays", GodRaysUniforms.get())
                    .sampler("u_Texture", src, PostProcessManager.getSampler())
                    .sampler("u_MainDepth", context.mainDepth(), PostProcessManager.getSampler())
                    .end();
            return true;
        } catch (Throwable t) {
            failed = true;
            DebugLog.error("[LoVisual] GodRays pass failed, disabled until re-enabled", t);
            return false;
        }
    }
}
