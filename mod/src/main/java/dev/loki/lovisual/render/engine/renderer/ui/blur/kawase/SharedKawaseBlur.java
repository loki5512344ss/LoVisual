/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.blur.kawase;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.blur.FrameBlurCacheEntry;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Общее состояние разделяемого Kawase-блюра на кадр/фазу: один источник — один
 * размытый результат, переиспользуемый всеми batch'ами с одинаковыми параметрами.
 * Пассы исполняет {@link KawaseBlurPreparer}.
 */
public final class SharedKawaseBlur {
    @Nullable GpuTextureView sourceView;
    @Nullable GpuSampler sourceSampler;
    @Nullable GpuTextureView blurredView;
    @Nullable GpuSampler blurredSampler;
    Renderer2D.BlurQuality quality = Renderer2D.DEFAULT_BLUR_QUALITY;
    float offsetPx = Renderer2D.DEFAULT_KAWASE_OFFSET_PX;

    public void reset() {
        sourceView = null;
        sourceSampler = null;
        blurredView = null;
        blurredSampler = null;
        quality = Renderer2D.DEFAULT_BLUR_QUALITY;
        offsetPx = Renderer2D.DEFAULT_KAWASE_OFFSET_PX;
    }

    public @Nullable GpuTextureView blurredView() {
        return blurredView;
    }

    public @Nullable GpuSampler blurredSampler() {
        return blurredSampler;
    }

    public @Nullable GpuTextureView matchingView(@Nullable GpuTextureView sourceView, @Nullable GpuSampler sourceSampler) {
        if (sourceView == null || sourceSampler == null) return null;
        if (blurredView == null || blurredSampler == null) return null;
        if (this.sourceView != sourceView || this.sourceSampler != sourceSampler) return null;
        return blurredView;
    }

    public @Nullable GpuSampler matchingSampler(@Nullable GpuTextureView sourceView, @Nullable GpuSampler sourceSampler) {
        if (matchingView(sourceView, sourceSampler) == null) return null;
        return blurredSampler;
    }

    /**
     * Подхватывает уже посчитанный в этом кадре/фазе блюр (кэш UiBlurResources) —
     * используется при активном shape-clip, где новый проход рисовать нельзя.
     */
    public boolean adoptCached(@Nullable GpuTextureView sourceView,
                               @Nullable GpuSampler sourceSampler,
                               float screenW,
                               float screenH,
                               float uiScale,
                               Renderer2D.BlurQuality quality,
                               float offsetPx) {
        if (sourceView == null || sourceSampler == null) return false;
        FrameBlurCacheEntry cachedBlur = UiBlurResources.findReusable(
                UiBlurResources.currentFrameId(),
                UiBlurResources.currentPhase(),
                sourceView,
                sourceSampler,
                screenW,
                screenH,
                uiScale,
                quality,
                offsetPx
        );
        if (cachedBlur == null || cachedBlur.blurredView == null || cachedBlur.blurredSampler == null) {
            return false;
        }
        this.sourceView = sourceView;
        this.sourceSampler = sourceSampler;
        blurredView = cachedBlur.blurredView;
        blurredSampler = cachedBlur.blurredSampler;
        this.quality = quality != null ? quality : Renderer2D.DEFAULT_BLUR_QUALITY;
        this.offsetPx = Float.isFinite(offsetPx) ? Math.max(0.0f, offsetPx) : Renderer2D.DEFAULT_KAWASE_OFFSET_PX;
        return true;
    }

    public int prepare(Minecraft mc,
                       @Nullable GpuTextureView sourceView,
                       @Nullable GpuSampler sourceSampler,
                       float screenW,
                       float screenH,
                       float uiScale,
                       Renderer2D.BlurQuality blurQuality,
                       float offsetPx) {
        return KawaseBlurPreparer.prepare(this, mc, sourceView, sourceSampler, screenW, screenH, uiScale, blurQuality, offsetPx);
    }

    /**
     * Прогрев liquid-glass блюра вне кадра: пассы исполняются сразу, результат
     * оседает в кэше UiBlurResources, собственное состояние не сохраняется.
     */
    public static void prewarmLiquidGlassBlur(Minecraft mc,
                                              @Nullable GpuTextureView sourceView,
                                              @Nullable GpuSampler sourceSampler,
                                              float screenW,
                                              float screenH,
                                              float uiScale) {
        KawaseBlurPreparer.prewarm(mc, sourceView, sourceSampler, screenW, screenH, uiScale);
    }
}
