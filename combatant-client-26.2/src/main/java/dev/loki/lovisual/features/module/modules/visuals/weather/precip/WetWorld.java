/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.weather.precip;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.world.liquid.WetSurfaceUniforms;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Мочит все подходящие (смотрящие вверх) поверхности мира по факту реальной погоды сервера —
 * в отличие от {@link Rain}, не зависит от партиклов дождя и её собственного тумблера
 * {@code wet_ground}: работает как отдельный лёгкий "always-on if raining" эффект. Использует
 * тот же SSR-шейдер (wet_surface.frag) и тот же {@link WetSurfaceUniforms}, что и Rain — если
 * включены оба модуля одновременно, эффект отражений применится дважды подряд (стек), поэтому
 * рекомендуется держать выключенным {@code Rain.wet_ground}, когда включён WetWorld.
 */
@ModuleInfo(
        id = "wet_world",
        displayName = "Wet World",
        category = ModuleCategory.VISUALS,
        description = "Мокрые поверхности мира с отражениями по факту дождя на сервере"
)
public final class WetWorld extends Module implements PostProcessPass {

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f invView = new Matrix4f();
    private boolean depthSupported = true;
    private float clock;
    private long lastFrameNanos;

    {
        PostProcessManager.register(this);
    }

    private final EnumValue<Quality> quality = enumMode("quality", Quality.HIGH, Quality.values());
    private final NumberValue<Float> reflectionStrength = num("reflection_strength", 0.75f, 0.1f, 1.5f);
    private final NumberValue<Float> darkening = num("darkening", 0.30f, 0.0f, 0.8f);
    private final BooleanValue ripples = bool("ripples", true);
    private final NumberValue<Float> rippleSpeed = visibleWhen(num("ripple_speed", 1.0f, 0.1f, 3.0f), ripples::get);
    private final BooleanValue rainOnly = bool("rain_only", true);

    @Override
    public void onEnable() {
        depthSupported = true;
        lastFrameNanos = 0L;
    }

    private int reflectionSteps() {
        return switch (quality.get()) {
            case LOW -> 16;
            case MEDIUM -> 28;
            case HIGH -> 44;
            case ULTRA -> 64;
        };
    }

    private int binarySteps() {
        return switch (quality.get()) {
            case LOW -> 2;
            case MEDIUM -> 3;
            case HIGH -> 5;
            case ULTRA -> 7;
        };
    }

    private float maxDistance() {
        return switch (quality.get()) {
            case LOW -> 24.0f;
            case MEDIUM -> 32.0f;
            case HIGH -> 48.0f;
            case ULTRA -> 64.0f;
        };
    }

    private float rainGradient(Minecraft mc) {
        try {
            return mc.level.getRainLevel(1.0f);
        } catch (Throwable ignored) {
            return mc.level.isRaining() ? 1.0f : 0.0f;
        }
    }

    @Override
    public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || !depthSupported || mc.player == null || mc.level == null) return false;
        return !rainOnly.get() || rainGradient(mc) > 0.01f;
    }

    @Override
    public int getPriority() {
        return 2;
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
        if (!isActive() || context == null || src == null || dst == null) return false;

        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) lastFrameNanos = now;
        clock += (float) Mth.clamp((now - lastFrameNanos) / 1_000_000_000.0, 0.0, 0.05);
        lastFrameNanos = now;

        GpuTextureView mainDepth = context.mainDepth();
        boolean hasDepth = mainDepth != null;
        if (!hasDepth) mainDepth = src;

        Matrix4f proj = LoVisualWorldMatrices.renderProjectionMatrix();
        if (proj == null) proj = RenderState.worldProjection;
        projection.set(proj);
        invView.set(new Matrix4f().rotation(RenderState.cameraRotation));

        float rain = rainGradient(mc);
        float effectiveStrength = reflectionStrength.get() * (rainOnly.get() ? Mth.clamp(rain, 0.0f, 1.0f) : 1.0f);

        WetSurfaceUniforms.update(
                projection,
                invView,
                (float) RenderState.cameraPos.x, (float) RenderState.cameraPos.y, (float) RenderState.cameraPos.z,
                context.width() > 0 ? 1.0f / context.width() : 0.0f,
                context.height() > 0 ? 1.0f / context.height() : 0.0f,
                clock,
                hasDepth,
                1.0f,
                0.95f,
                effectiveStrength,
                ripples.get() ? 1.0f : 0.0f,
                maxDistance(),
                reflectionSteps(),
                Mth.clamp(0.6f + rain * 0.4f, 0.6f, 1.0f),
                ripples.get(),
                binarySteps(),
                darkening.get()
        );

        try {
            FullScreenRenderer.ensureInit();
            FullScreenRenderer.begin("LoVisual WetWorld Pass")
                    .attachment(dst)
                    .pipeline(LoVisualRenderPipelines.WET_SURFACE)
                    .uniform("WetSurface", WetSurfaceUniforms.get())
                    .sampler("u_Texture", src, PostProcessManager.getSampler())
                    .sampler("u_MainDepth", mainDepth, PostProcessManager.getSampler())
                    .end();
        } catch (Throwable t) {
            depthSupported = false;
            return false;
        }

        return true;
    }

    public enum Quality implements EnumValue.IdProvider {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        ULTRA("ultra");

        private final String id;

        Quality(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
