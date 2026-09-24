package dev.loki.lovisual.render.engine.renderer.ui.blur.kawase;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.blur.FrameBlurCacheEntry;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Исполнение Kawase-пассов (down/up-цепочка) и наполнение кэша UiBlurResources.
 * Пишет результат в переданное {@link SharedKawaseBlur}-состояние.
 */
final class KawaseBlurPreparer {
    private KawaseBlurPreparer() {
    }

    /**
     * Снимок текущей позиции в цепочке пассов.
     */
    private record PassState(GpuTextureView view, GpuSampler sampler, int w, int h, int drawCalls) {
    }

    static int prepare(SharedKawaseBlur blur,
                       Minecraft mc,
                       @Nullable GpuTextureView sourceView,
                       @Nullable GpuSampler sourceSampler,
                       float screenW,
                       float screenH,
                       float uiScale,
                       Renderer2D.BlurQuality blurQuality,
                       float offsetPx) {
        if (LoVisualRenderSystem.rhi().shapeClip().isActive()) {
            blur.adoptCached(sourceView, sourceSampler, screenW, screenH, uiScale, blurQuality, offsetPx);
            return 0;
        }
        return prepareInternal(blur, mc, sourceView, sourceSampler, screenW, screenH, uiScale, blurQuality, offsetPx);
    }

    static void prewarm(Minecraft mc,
                        @Nullable GpuTextureView sourceView,
                        @Nullable GpuSampler sourceSampler,
                        float screenW,
                        float screenH,
                        float uiScale) {
        if (mc == null || sourceView == null || sourceSampler == null) return;
        prepareInternal(new SharedKawaseBlur(), mc, sourceView, sourceSampler, screenW, screenH, uiScale,
                Renderer2D.DEFAULT_LIQUID_GLASS_BLUR_QUALITY, Renderer2D.LIQUID_GLASS_KAWASE_OFFSET_PX);
    }

    private static int prepareInternal(SharedKawaseBlur blur,
                                       Minecraft mc,
                                       @Nullable GpuTextureView sourceView,
                                       @Nullable GpuSampler sourceSampler,
                                       float screenW,
                                       float screenH,
                                       float uiScale,
                                       Renderer2D.BlurQuality blurQuality,
                                       float offsetPx) {
        if (sourceView == null || sourceSampler == null) {
            return 0;
        }
        Renderer2D.BlurQuality quality = blurQuality != null ? blurQuality : Renderer2D.DEFAULT_BLUR_QUALITY;
        int iterations = Math.max(1, quality.iterations);
        float passOffset = Float.isFinite(offsetPx) ? Math.max(0.0f, offsetPx) : Renderer2D.DEFAULT_KAWASE_OFFSET_PX;

        if (blur.blurredView != null && blur.blurredSampler != null
                && blur.sourceView == sourceView
                && blur.sourceSampler == sourceSampler
                && blur.quality == quality
                && Float.compare(blur.offsetPx, passOffset) == 0) {
            return 0;
        }

        long frameId = UiBlurResources.currentFrameId();
        RenderPhase phase = UiBlurResources.currentPhase();
        FrameBlurCacheEntry cachedBlur = UiBlurResources.findReusable(frameId, phase, sourceView, sourceSampler,
                screenW, screenH, uiScale, quality, passOffset);
        if (cachedBlur != null) {
            blur.sourceView = sourceView;
            blur.sourceSampler = sourceSampler;
            blur.blurredView = cachedBlur.blurredView;
            blur.blurredSampler = cachedBlur.blurredSampler;
            blur.quality = quality;
            blur.offsetPx = passOffset;
            return 0;
        }

        blur.reset();

        GpuSampler blurSampler = PostProcessManager.getSampler();
        if (blurSampler == null) {
            return 0;
        }

        PassState pass = new PassState(sourceView, sourceSampler,
                Math.max(1, Math.round(screenW)), Math.max(1, Math.round(screenH)), 0);
        for (int level = 0; level < iterations; level++) {
            pass = runLevel(mc, true, level, pass, blurSampler, passOffset);
            if (pass == null || pass.view() == null) return 0;
        }
        for (int level = iterations - 2; level >= 0; level--) {
            pass = runLevel(mc, false, level, pass, blurSampler, passOffset);
            if (pass == null || pass.view() == null) return 0;
        }

        blur.sourceView = sourceView;
        blur.sourceSampler = sourceSampler;
        blur.blurredView = pass.view();
        blur.blurredSampler = blurSampler;
        blur.quality = quality;
        blur.offsetPx = passOffset;
        UiBlurResources.remember(frameId, phase, sourceView, sourceSampler, pass.view(), blurSampler,
                screenW, screenH, uiScale, quality, passOffset);
        return pass.drawCalls();
    }

    /**
     * Один пасс; при неудаче цепочки возвращает null (ранний выход без результата).
     */
    private static @Nullable PassState runLevel(Minecraft mc, boolean down, int level, PassState in,
                                                GpuSampler blurSampler, float passOffset) {
        TextureTarget target = down
                ? UiBlurResources.ensureKawaseDown(mc, level)
                : UiBlurResources.ensureKawaseUp(mc, level);
        if (target == null
                || !KawasePass.draw(target, in.view(), in.sampler(), in.w(), in.h(), passOffset, !down)) {
            return null;
        }
        return new PassState(target.getColorTextureView(), blurSampler,
                Math.max(1, target.width), Math.max(1, target.height), in.drawCalls() + 1);
    }
}
