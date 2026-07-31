/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.config.values.RGBColorValue;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.ShaderEspBlurUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.ShaderEspGradientUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.ShaderEspSmokeUniforms;

import java.util.OptionalDouble;

final class EspShaderColor {

    private static GpuSampler shaderEspSampler;

    private EspShaderColor() {
    }

    static boolean renderColorPass(ESP esp, GpuTextureView dst, int width, int height) {
        if (esp.shaderMask.getColorTextureView() == null) {
            return false;
        }

        float darkMultiplier = esp.shaderDarkMultiplier.get();
        boolean rendered = false;

        if (esp.shaderGlow.get()) {
            float glowRadius = Math.max(1.0f, esp.shaderGlowStrength.get());
            blurShaderMask(esp, esp.shaderMask, esp.shaderEffectBuffer, glowRadius, width, height);
            drawShaderGradient(esp, esp.shaderEffectBuffer, dst, width, height,
                    EspColorUtil.alpha255(esp.shaderGlowAlpha.get()),
                    darkMultiplier,
                    esp.shaderGlowColorMode,
                    esp.shaderGlowColor,
                    esp.shaderGlowIntensity.get());
            rendered = true;
        }

        if (esp.shaderFill.get()) {
            if (esp.isShaderSmokeFill()) {
                drawShaderSmoke(esp, esp.shaderMask, dst, width, height);
            } else {
                drawShaderGradient(esp, esp.shaderMask, dst, width, height,
                        EspColorUtil.alpha255(esp.shaderFillAlpha.get()),
                        darkMultiplier,
                        esp.shaderFillColorMode,
                        esp.shaderFillColor,
                        1.0f);
            }
            rendered = true;
        }

        if (esp.shaderOutline.get()) {
            float outlineRadius = Math.max(0.5f, esp.shaderOutlineWidth.get());
            blurShaderMask(esp, esp.shaderMask, esp.shaderEffectBuffer, outlineRadius, width, height);
            drawShaderGradient(esp, esp.shaderEffectBuffer, dst, width, height,
                    EspColorUtil.alpha255(esp.shaderOutlineAlpha.get()),
                    darkMultiplier,
                    esp.shaderShadowColorMode,
                    esp.shaderShadowColor,
                    esp.shaderOutlineIntensity.get());
            rendered = true;
        }

        return rendered;
    }

    private static void blurShaderMask(ESP esp, RenderTarget input, RenderTarget output, float radius, int width, int height) {

        clearFramebuffer(esp.shaderBlurBuffer);
        ShaderEspBlurUniforms.update(width, height, Math.min(radius, 63.0f), 1.0f, 0.0f);
        FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                .attachment(esp.shaderBlurBuffer.getColorTextureView())
                .pipeline(LoVisualRenderPipelines.SHADER_ESP_SHADOW)
                .uniform("ShaderEspBlur", ShaderEspBlurUniforms.get())
                .sampler("u_Texture", input.getColorTextureView(), getShaderEspSampler())
                .sampler("u_Mask", input.getColorTextureView(), getShaderEspSampler())
                .end();

        clearFramebuffer(output);
        ShaderEspBlurUniforms.update(width, height, Math.min(radius, 63.0f), 0.0f, 1.0f);
        FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                .attachment(output.getColorTextureView())
                .pipeline(LoVisualRenderPipelines.SHADER_ESP_SHADOW)
                .uniform("ShaderEspBlur", ShaderEspBlurUniforms.get())
                .sampler("u_Texture", esp.shaderBlurBuffer.getColorTextureView(), getShaderEspSampler())
                .sampler("u_Mask", input.getColorTextureView(), getShaderEspSampler())
                .end();
    }

    private static void drawShaderGradient(ESP esp, RenderTarget input, GpuTextureView dst, int width, int height, int alpha,
                                           float darkMultiplier, ModeValue colorMode, RGBColorValue customColor,
                                           float intensity) {
        int rgb = resolveShaderColor(colorMode, customColor);
        boolean overrideColor = usesShaderOverrideColor(colorMode);
        int passColor = EspColorUtil.withAlpha(rgb, alpha);
        ShaderEspGradientUniforms.update(
                0.0f,
                0.0f,
                width,
                height,
                passColor,
                darkMultiplier,
                overrideColor ? 1.0f : 0.0f,
                Math.max(0.0f, Math.min(4.0f, intensity))
        );
        FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                .attachment(dst)
                .pipeline(LoVisualRenderPipelines.SHADER_ESP_GRADIENT)
                .uniform("ShaderEspGradient", ShaderEspGradientUniforms.get())
                .sampler("u_Texture", input.getColorTextureView(), getShaderEspSampler())
                .end();
    }

    private static void drawShaderSmoke(ESP esp, RenderTarget input, GpuTextureView dst, int width, int height) {
        int rgb = resolveShaderColor(esp.shaderFillColorMode, esp.shaderFillColor);
        boolean overrideColor = usesShaderOverrideColor(esp.shaderFillColorMode);
        ShaderEspSmokeUniforms.update(
                0.0f,
                0.0f,
                width,
                height,
                EspColorUtil.shaderSmokeTime(),
                esp.shaderSmokeScale.get(),
                esp.shaderSmokeSpeed.get(),
                EspColorUtil.clamp01(esp.shaderFillAlpha.get()),
                esp.shaderSmokeOctaves.get(),
                esp.shaderSmokeContrast.get(),
                overrideColor ? 1.0f : 0.0f,
                1.0f,
                EspColorUtil.deriveSmokeColor(rgb, 0),
                EspColorUtil.deriveSmokeColor(rgb, 1),
                EspColorUtil.deriveSmokeColor(rgb, 2)
        );
        FullScreenRenderer.begin("LoVisual Fullscreen Pass")
                .attachment(dst)
                .pipeline(LoVisualRenderPipelines.SHADER_ESP_SMOKE)
                .uniform("ShaderEspSmoke", ShaderEspSmokeUniforms.get())
                .sampler("u_Texture", input.getColorTextureView(), getShaderEspSampler())
                .end();
    }

    private static GpuSampler getShaderEspSampler() {
        if (shaderEspSampler != null) {
            return shaderEspSampler;
        }
        shaderEspSampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                1,
                OptionalDouble.empty()
        );
        return shaderEspSampler;
    }

    private static void clearFramebuffer(RenderTarget framebuffer) {
        if (framebuffer == null || framebuffer.getColorTexture() == null) {
            return;
        }
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(framebuffer.getColorTexture(), new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
    }

    private static int resolveShaderColor(ModeValue mode, RGBColorValue customColor) {
        if (mode != null) {
            String value = mode.get();
            if (ESP.SHADER_COLOR_THEME.equalsIgnoreCase(value)) {
                return Theme.theme().accent() & 0x00FFFFFF;
            }
            if (ESP.SHADER_COLOR_CUSTOM.equalsIgnoreCase(value) && customColor != null) {
                return customColor.getArgb() & 0x00FFFFFF;
            }
        }
        return 0xFFFFFF;
    }

    private static boolean usesShaderOverrideColor(ModeValue mode) {
        if (mode == null) return false;
        String value = mode.get();
        return ESP.SHADER_COLOR_THEME.equalsIgnoreCase(value) || ESP.SHADER_COLOR_CUSTOM.equalsIgnoreCase(value);
    }
}
