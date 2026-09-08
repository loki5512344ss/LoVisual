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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class EspShaderRenderer {

    private EspShaderRenderer() {
    }

    static boolean render(ESP esp, GpuTextureView src, GpuTextureView dst, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!esp.isEnabled() || !esp.isShaderBox() || mc.level == null || mc.player == null) {
            return false;
        }
        if (!esp.shaderGlow.get() && !esp.shaderOutline.get() && !esp.shaderFill.get()) {
            return false;
        }

        Vec3 camPos = mc.gameRenderer.mainCamera().position();
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

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        return EspShaderColor.renderColorPass(esp, dst, width, height);
    }

    static void ensureShaderBuffers(ESP esp) {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
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
        Minecraft mc = Minecraft.getInstance();
        if (IrisLoVisualFrameHooks.isRenderingAfterIrisFinalization()) {
            return getStandaloneShaderEspRenderDispatcher(esp);
        }
        if (mc != null && mc.levelRenderer instanceof dev.loki.lovisual.mixins.accessors.LevelRendererAccessor accessor) {
            FeatureRenderDispatcher dispatcher = accessor.lovisual$getEntityRenderDispatcher();
            if (dispatcher != null) {
                return dispatcher;
            }
        }
        return getStandaloneShaderEspRenderDispatcher(esp);
    }

    static FeatureRenderDispatcher getStandaloneShaderEspRenderDispatcher(ESP esp) {
        Minecraft mc = Minecraft.getInstance();
        if (esp.shaderEspRenderDispatcher != null) {
            return esp.shaderEspRenderDispatcher;
        }
        if (mc.gameRenderer == null || mc.getModelManager() == null || mc.getAtlasManager() == null || mc.font == null) {
            return null;
        }
        esp.shaderEspRenderBuffers = new RenderBuffers(1);
        esp.shaderEspRenderDispatcher = new FeatureRenderDispatcher(
                esp.shaderEspRenderBuffers,
                mc.getModelManager(),
                mc.getAtlasManager(),
                mc.font,
                mc.gameRenderer.gameRenderState()
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
