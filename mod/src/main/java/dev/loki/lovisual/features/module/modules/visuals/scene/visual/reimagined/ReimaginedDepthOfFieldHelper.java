package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.depth.WorldSceneDepth;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.blur.DepthOfFieldDepthSource;
import dev.loki.lovisual.render.engine.postprocess.blur.DepthOfFieldQuality;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.post.DepthOfFieldUniforms;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.render.iris.scene.IrisSceneDepth;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

final class ReimaginedDepthOfFieldHelper implements PostProcessPass {
    private final EnumValue<DepthOfFieldDepthSource> depthSource;
    private final NumberValue<Float> farStart;
    private final NumberValue<Float> farTransition;
    private final NumberValue<Float> strength;
    private final NumberValue<Float> maxRadius;
    private final EnumValue<DepthOfFieldQuality> quality;
    private final BooleanValue debugCoc;
    private final Matrix4f projection = new Matrix4f();
    private boolean depthSamplerSupported = true;
    ReimaginedDepthOfFieldHelper(EnumValue<DepthOfFieldDepthSource> depthSource, NumberValue<Float> farStart, NumberValue<Float> farTransition, NumberValue<Float> strength, NumberValue<Float> maxRadius, EnumValue<DepthOfFieldQuality> quality, BooleanValue debugCoc) {
        this.depthSource = depthSource; this.farStart = farStart; this.farTransition = farTransition; this.strength = strength; this.maxRadius = maxRadius; this.quality = quality; this.debugCoc = debugCoc;
    }
    @Override public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        ReimaginedVisual module = Modules.get(ReimaginedVisual.class);
        boolean moduleActive = module != null && module.isEnabled() && module.isEffectSelected("depth_of_field");
        return moduleActive && mc != null && mc.player != null && mc.level != null && !IrisRuntime.isShaderpackRendererActive() && depthSamplerSupported && depthSource.get() != DepthOfFieldDepthSource.OFF;
    }
    boolean needsWorldSceneDepthCapture() { return isActive() && depthSource.get() == DepthOfFieldDepthSource.WORLD_SCENE; }
    boolean needsPreTranslucentDepthCapture() { return isActive() && depthSource.get() == DepthOfFieldDepthSource.PRE_TRANSLUCENT; }
    boolean needsResolvedMainDepthCapture() { return isActive() && depthSource.get() == DepthOfFieldDepthSource.MAIN; }
    @Override public int getPriority() { return 3; }
    @Override public Phase getPhase() { return Phase.PRE_HAND; }
    @Override public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) { return false; }
    @Override public boolean render(PostProcessContext context, GpuTextureView src, GpuTextureView dst) {
        if (!isActive() || context == null || src == null || dst == null) return false;
        ReimaginedDepthBindings depth = resolveDepthBindings(context);
        if (!depth.hasAnyDepth()) {
            if (!debugCoc.get()) return false;
            depth = new ReimaginedDepthBindings(null, null, null, null, null, null);
        }
        buildProjection();
        int taps = quality.get().taps();
        if (OptimizeState.isLitePost()) {
            taps = DepthOfFieldQuality.LOW.taps();
        }
        DepthOfFieldUniforms.update(projection, context.width(), context.height(), 0.0f, 0.0f, farStart.get(), farTransition.get(), strength.get(), maxRadius.get(), taps, 0.85f, debugCoc.get(), depth.hasMain(), depth.hasTranslucent(), depth.hasItemEntity(), depth.hasParticles(), depth.hasWeather(), depth.hasClouds());
        try {
            FullScreenRenderer.ensureInit();
            FullScreenRenderer.begin("LoVisual DepthOfField Pass").attachment(dst).pipeline(LoVisualRenderPipelines.DEPTH_OF_FIELD).uniform("DepthOfField", DepthOfFieldUniforms.get()).sampler("u_Texture", src, PostProcessManager.getSampler()).sampler("u_MainDepth", depth.mainOr(src), PostProcessManager.getSampler()).sampler("u_TranslucentDepth", depth.translucentOr(src), PostProcessManager.getSampler()).sampler("u_ItemEntityDepth", depth.itemEntityOr(src), PostProcessManager.getSampler()).sampler("u_ParticlesDepth", depth.particlesOr(src), PostProcessManager.getSampler()).sampler("u_WeatherDepth", depth.weatherOr(src), PostProcessManager.getSampler()).sampler("u_CloudsDepth", depth.cloudsOr(src), PostProcessManager.getSampler()).end();
        } catch (Throwable t) {
            depthSamplerSupported = false;
            DebugLog.warn("[LoVisual] DepthOfField depth sampler path failed; disabling DoF depth sampling for this session: " + t);
            return false;
        }
        return true;
    }
    private void buildProjection() {
        Matrix4f proj = LoVisualWorldMatrices.renderProjectionMatrix();
        if (proj == null) proj = RenderState.worldProjection;
        projection.set(proj);
    }
    void resetSamplerSupport() { depthSamplerSupported = true; }
    private ReimaginedDepthBindings resolveDepthBindings(PostProcessContext context) {
        DepthOfFieldDepthSource s = depthSource.get();
        if (s == DepthOfFieldDepthSource.WORLD_SCENE) return resolveWorldSceneDepth();
        if (s == DepthOfFieldDepthSource.MAIN) return new ReimaginedDepthBindings(context.mainDepth(), null, null, null, null, null);
        if (s == DepthOfFieldDepthSource.PRE_TRANSLUCENT) return new ReimaginedDepthBindings(context.preTranslucentDepth(), null, null, null, null, null);
        return new ReimaginedDepthBindings(null, null, null, null, null, null);
    }
    private ReimaginedDepthBindings resolveWorldSceneDepth() {
        if (IrisSceneDepth.isValid()) return new ReimaginedDepthBindings(IrisSceneDepth.mainDepthView(), IrisSceneDepth.preTranslucentDepthView(), IrisSceneDepth.preHandDepthView(), null, null, null);
        if (!WorldSceneDepth.isValid() || !WorldSceneDepth.hasMain()) return new ReimaginedDepthBindings(null, null, null, null, null, null);
        return new ReimaginedDepthBindings(WorldSceneDepth.mainDepthView(), WorldSceneDepth.translucentDepthView(), WorldSceneDepth.itemEntityDepthView(), WorldSceneDepth.particlesDepthView(), WorldSceneDepth.weatherDepthView(), null);
    }
}
