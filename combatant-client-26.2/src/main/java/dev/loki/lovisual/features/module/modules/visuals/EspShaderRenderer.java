/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.msaa.MsaaFramebuffer;
import dev.loki.lovisual.render.engine.msaa.MsaaWorldTarget;
import dev.loki.lovisual.render.engine.postprocess.PostProcessManager;
import dev.loki.lovisual.render.iris.IrisLoVisualFrameHooks;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class EspShaderRenderer {

    private EspShaderRenderer() {
    }

    static boolean render(ESP esp, GpuTextureView src, GpuTextureView dst, float tickDelta) {
        if (!esp.isEnabled() || !esp.isShaderBox() || esp.mc.level == null || esp.mc.player == null) {
            return false;
        }
        if (!esp.shaderGlow.get() && !esp.shaderOutline.get() && !esp.shaderFill.get()) {
            return false;
        }

        Vec3 camPos = esp.mc.gameRenderer.mainCamera().position();
        List<EspModel.RenderEntry> entries = EspEntityCollector.collectShaderEntries(esp, camPos);
        if (entries.isEmpty()) {
            return false;
        }

        ensureShaderBuffers(esp);
        if (esp.shaderMask == null || esp.shaderBlurBuffer == null || esp.shaderEffectBuffer == null) {
            return false;
        }

        if (!EspShaderMask.renderEntityMask(esp, entries, tickDelta)) {
            return false;
        }

        PostProcessManager.copy(src, dst);

        int width = esp.mc.getWindow().getWidth();
        int height = esp.mc.getWindow().getHeight();
        return EspShaderColor.renderColorPass(esp, dst, width, height);
    }

    static void ensureShaderBuffers(ESP esp) {
        int width = esp.mc.getWindow().getWidth();
        int height = esp.mc.getWindow().getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean sizeChanged = width != esp.shaderBufferW || height != esp.shaderBufferH;
        if (esp.shaderMask == null) {
            esp.shaderMask = new TextureTarget("lovisual-shader-esp-mask", width, height, true, GpuFormat.RGBA8_UNORM);
            esp.shaderBlurBuffer = new TextureTarget("lovisual-shader-esp-blur", width, height, false, GpuFormat.RGBA8_UNORM);
            esp.shaderEffectBuffer = new TextureTarget("lovisual-shader-esp-effect", width, height, false, GpuFormat.RGBA8_UNORM);
            esp.shaderBufferW = width;
            esp.shaderBufferH = height;
        } else if (sizeChanged) {
            esp.shaderMask.resize(width, height);
            esp.shaderBlurBuffer.resize(width, height);
            esp.shaderEffectBuffer.resize(width, height);
            esp.shaderBufferW = width;
            esp.shaderBufferH = height;
        }

        int samples = MsaaWorldTarget.getSamples();
        if (samples > 1) {
            if (esp.shaderMaskMsaa == null || esp.shaderMaskSamples != samples) {
                if (esp.shaderMaskMsaa != null) {
                    esp.shaderMaskMsaa.destroyBuffers();
                }
                esp.shaderMaskMsaa = new MsaaFramebuffer("lovisual-shader-esp-mask-msaa", width, height, true, samples);
                esp.shaderMaskSamples = samples;
            } else if (sizeChanged) {
                esp.shaderMaskMsaa.resize(width, height);
            }
        } else if (esp.shaderMaskMsaa != null) {
            esp.shaderMaskMsaa.destroyBuffers();
            esp.shaderMaskMsaa = null;
            esp.shaderMaskSamples = 0;
        }
    }

    static FeatureRenderDispatcher getShaderEspRenderDispatcher(ESP esp) {
        if (IrisLoVisualFrameHooks.isRenderingAfterIrisFinalization()) {
            return getStandaloneShaderEspRenderDispatcher(esp);
        }
        if (esp.mc != null && esp.mc.levelRenderer instanceof dev.loki.lovisual.mixins.accessors.LevelRendererAccessor accessor) {
            FeatureRenderDispatcher dispatcher = accessor.lovisual$getEntityRenderDispatcher();
            if (dispatcher != null) {
                return dispatcher;
            }
        }
        return getStandaloneShaderEspRenderDispatcher(esp);
    }

    static FeatureRenderDispatcher getStandaloneShaderEspRenderDispatcher(ESP esp) {
        if (esp.shaderEspRenderDispatcher != null) {
            return esp.shaderEspRenderDispatcher;
        }
        if (esp.mc.gameRenderer == null || esp.mc.getModelManager() == null || esp.mc.getAtlasManager() == null || esp.mc.font == null) {
            return null;
        }
        esp.shaderEspRenderBuffers = new RenderBuffers(1);
        esp.shaderEspRenderDispatcher = new FeatureRenderDispatcher(
                esp.shaderEspRenderBuffers,
                esp.mc.getModelManager(),
                esp.mc.getAtlasManager(),
                esp.mc.font,
                esp.mc.gameRenderer.gameRenderState()
        );
        return esp.shaderEspRenderDispatcher;
    }

    static void closeDispatcher(ESP esp) {
        if (esp.shaderEspRenderDispatcher != null) {
            esp.shaderEspRenderDispatcher.close();
            esp.shaderEspRenderDispatcher = null;
        }
        if (esp.shaderEspRenderBuffers != null) {
            esp.shaderEspRenderBuffers.close();
            esp.shaderEspRenderBuffers = null;
        }
    }
}
