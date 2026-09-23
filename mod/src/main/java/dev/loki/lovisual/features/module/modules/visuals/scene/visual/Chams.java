/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams.ChamsMaskHelper;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams.ChamsModeHelper;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams.ChamsShaderHelper;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;

@ModuleInfo(id = "chams", displayName = "Chams", category = ModuleCategory.VISUALS)
public class Chams extends Module {
    public final ModeValue mode = modeSetting("handChamsMode", "mode", "Smoke", "Smoke", "Metallic", "Glass", "Noise", "Snow", "Chroma", "Solid", "Stripes", "Glow", "Balatro", "Invert");
    public final BooleanValue hands = bool("chamsHands", "hands", true);
    public final RGBAColorValue fillColor = color("handChamsFillColor", "fill", "#66FFFFFF");
    public final ModeValue visualColorSource = visibleWhen(modeSetting("handChamsVisualColorSource", "visual_color_source", "Custom", "Custom", "Theme"), this::isHandEffectMode);
    public final RGBColorValue metallicBase = visibleWhen(colorNoAlpha("handChamsMetallicBase", "metallic_base", "#B0B4BA"), this::isCustomMetallicVisualColor);
    public final RGBColorValue metallicHighlight = visibleWhen(colorNoAlpha("handChamsMetallicHighlight", "metallic_highlight", "#FFFFFF"), this::isCustomMetallicVisualColor);
    public final ModeValue effectColorSource = visibleWhen(modeSetting("handChamsEffectColorSource", "effect_color_source", "Theme", "Material", "Theme", "Custom"), this::isHandEffectMode);
    public final RGBColorValue glowColor = visibleWhen(colorNoAlpha("handChamsGlowColor", "glow_color", "#87DFFF"), this::isCustomEffectColor);
    public final RGBColorValue shadowColor = visibleWhen(colorNoAlpha("handChamsShadowColor", "shadow_color", "#10131A"), this::isCustomEffectColor);
    public final BooleanValue glow = visibleWhen(bool("handChamsGlow", "glow", true), this::isHandEffectMode);
    public final NumberValue<Float> glowAlpha = visibleWhen(num("handChamsGlowAlpha", "glow_alpha", 0.72f, 0.0f, 1.0f), this::isGlowVisible);
    public final NumberValue<Float> glowStrength = visibleWhen(num("handChamsGlowStrength", "glow_strength", 1.15f, 0.0f, 6.0f), this::isGlowVisible);
    public final BooleanValue shadow = visibleWhen(bool("handChamsShadow", "shadow", true), this::isHandEffectMode);
    public final NumberValue<Float> shadowAlpha = visibleWhen(num("handChamsShadowAlpha", "shadow_alpha", 0.48f, 0.0f, 1.0f), this::isShadowVisible);
    public final NumberValue<Float> shadowStrength = visibleWhen(num("handChamsShadowStrength", "shadow_strength", 1.0f, 0.0f, 4.0f), this::isShadowVisible);
    public final NumberValue<Float> edgeWidth = visibleWhen(num("handChamsEdgeWidth", "edge_width", 10.0f, 0.0f, 36.0f), this::isHandEffectMode);
    public final NumberValue<Integer> quality = visibleWhen(num("handChamsQuality", "quality", 2, 1, 4), this::isHandEffectMode);
    public final NumberValue<Integer> smokeOctaves = visibleWhen(num("handChamsSmokeOctaves", "smoke_octaves", 4, 1, 6), this::isSmokeMode);
    public final NumberValue<Float> smokeSpeed = visibleWhen(num("chamsSmokeSpeed", "smoke_speed", 0.72f, 0.0f, 3.0f), this::isSmokeMode);
    public final NumberValue<Float> smokeScale = visibleWhen(num("handChamsSmokeScale", "smoke_scale", 4.4f, 1.0f, 12.0f), this::isSmokeMode);
    public final NumberValue<Float> smokeContrast = visibleWhen(num("handChamsSmokeContrast", "smoke_contrast", 1.12f, 0.35f, 2.4f), this::isSmokeMode);
    public final NumberValue<Float> smokeSwirl = visibleWhen(num("handChamsSmokeSwirl", "smoke_swirl", 0.85f, 0.0f, 2.5f), this::isSmokeMode);
    public final NumberValue<Float> smokeDensity = visibleWhen(num("handChamsSmokeDensity", "smoke_density", 1.0f, 0.2f, 2.5f), this::isSmokeMode);
    public final NumberValue<Float> noiseAlpha = visibleWhen(num("handChamsNoiseAlpha", "noise_alpha", 0.85f, 0.0f, 1.0f), this::isNoiseMode);
    public final NumberValue<Float> metallicIntensity = visibleWhen(num("handChamsMetallicIntensity", "metallic_intensity", 1.35f, 0.0f, 5.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicSharpness = visibleWhen(num("handChamsMetallicSharpness", "metallic_sharpness", 2.2f, 0.35f, 10.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicEdge = visibleWhen(num("handChamsMetallicEdge", "metallic_edge", 1.25f, 0.0f, 4.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicSweepSpeed = visibleWhen(num("handChamsMetallicSweepSpeed", "metallic_sweep_speed", 1.15f, 0.0f, 5.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicSweepScale = visibleWhen(num("handChamsMetallicSweepScale", "metallic_sweep_scale", 10.0f, 1.0f, 36.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicBrushedLines = visibleWhen(num("handChamsMetallicBrushedLines", "metallic_brushed_lines", 0.55f, 0.0f, 1.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicFlakes = visibleWhen(num("handChamsMetallicFlakes", "metallic_flakes", 0.25f, 0.0f, 1.0f), this::isMetallicMode);
    public final NumberValue<Float> metallicPrism = visibleWhen(num("handChamsMetallicPrism", "metallic_prism", 0.18f, 0.0f, 1.0f), this::isMetallicMode);
    public final BooleanValue dualShader = bool("chamsDualShader", "dual_shader", false);
    public final ModeValue shader1 = visibleWhen(modeSetting("chamsShader1", "shader1", "Glow", "Snow", "Chroma", "Solid", "Stripes", "Glow", "Balatro", "Invert"), dualShader::get);
    public final ModeValue shader2 = visibleWhen(modeSetting("chamsShader2", "shader2", "Chroma", "Snow", "Chroma", "Solid", "Stripes", "Glow", "Balatro", "Invert"), dualShader::get);
    public final ModeValue blendMode = visibleWhen(modeSetting("chamsBlendMode", "blend_mode", "Add", "Mix", "Add", "Multiply", "Screen", "Overlay", "Difference"), dualShader::get);
    public final NumberValue<Float> blendIntensity = visibleWhen(num("chamsBlendIntensity", "blend_intensity", 1.0f, 0.0f, 1.0f), dualShader::get);
    public final NumberValue<Float> shader1Strength = visibleWhen(num("chamsShader1Strength", "shader1_strength", 1.0f, 0.0f, 1.0f), dualShader::get);
    public final NumberValue<Float> shader2Strength = visibleWhen(num("chamsShader2Strength", "shader2_strength", 1.0f, 0.0f, 1.0f), dualShader::get);
    public final RGBColorValue snowColor = visibleWhen(colorNoAlpha("chamsSnowColor", "snow_color", "#FFFFFF"), this::isSnowMode);
    public final NumberValue<Float> snowIntensity = visibleWhen(num("chamsSnowIntensity", "snow_intensity", 1.0f, 0.1f, 3.0f), this::isSnowMode);
    public final NumberValue<Float> snowSpeed = visibleWhen(num("chamsSnowSpeed", "snow_speed", 1.0f, 0.1f, 2.0f), this::isSnowMode);
    public final NumberValue<Float> chromaSpeed = visibleWhen(num("chamsChromaSpeed", "chroma_speed", 0.3f, 0.05f, 1.0f), this::isChromaMode);
    public final NumberValue<Float> chromaSaturation = visibleWhen(num("chamsChromaSaturation", "chroma_saturation", 0.5f, 0.1f, 1.0f), this::isChromaMode);
    public final NumberValue<Float> chromaBrightness = visibleWhen(num("chamsChromaBrightness", "chroma_brightness", 1.0f, 0.5f, 1.5f), this::isChromaMode);
    public final RGBColorValue solidColor1 = visibleWhen(colorNoAlpha("chamsSolidColor1", "solid_color1", "#E91E63"), this::isSolidMode);
    public final RGBColorValue solidColor2 = visibleWhen(colorNoAlpha("chamsSolidColor2", "solid_color2", "#9B59B6"), this::isSolidMode);
    public final NumberValue<Float> solidSpeed = visibleWhen(num("chamsSolidSpeed", "solid_speed", 0.5f, 0.1f, 1.0f), this::isSolidMode);
    public final RGBColorValue stripesColor1 = visibleWhen(colorNoAlpha("chamsStripesColor1", "stripes_color1", "#778889"), this::isStripesMode);
    public final RGBColorValue stripesColor2 = visibleWhen(colorNoAlpha("chamsStripesColor2", "stripes_color2", "#5A6566"), this::isStripesMode);
    public final NumberValue<Float> stripesWidth = visibleWhen(num("chamsStripesWidth", "stripes_width", 0.3f, 0.05f, 0.5f), this::isStripesMode);
    public final NumberValue<Float> stripesSpeed = visibleWhen(num("chamsStripesSpeed", "stripes_speed", 0.3f, 0.1f, 2.0f), this::isStripesMode);
    public final BooleanValue glowShaderFill = visibleWhen(bool("chamsGlowShaderFill", "glow_shader_fill", true), this::isGlowShaderMode);
    public final NumberValue<Float> glowShaderRadius = visibleWhen(num("chamsGlowShaderRadius", "glow_shader_radius", 0.04f, 0.01f, 0.2f), this::isGlowShaderMode);
    public final NumberValue<Float> glowShaderPower = visibleWhen(num("chamsGlowShaderPower", "glow_shader_power", 0.75f, 0.1f, 2.0f), this::isGlowShaderMode);
    public final NumberValue<Float> glowShaderDispersion = visibleWhen(num("chamsGlowShaderDispersion", "glow_shader_dispersion", 5.0f, 1.0f, 10.0f), this::isGlowShaderMode);
    public final RGBColorValue glowShaderColor = visibleWhen(colorNoAlpha("chamsGlowShaderColor", "glow_shader_color", "#3498DB"), this::isGlowShaderMode);
    public final RGBColorValue balatroColor1 = visibleWhen(colorNoAlpha("chamsBalatroColor1", "balatro_color1", "#DE443B"), this::isBalatroMode);
    public final RGBColorValue balatroColor2 = visibleWhen(colorNoAlpha("chamsBalatroColor2", "balatro_color2", "#006BB4"), this::isBalatroMode);
    public final RGBColorValue balatroColor3 = visibleWhen(colorNoAlpha("chamsBalatroColor3", "balatro_color3", "#162325"), this::isBalatroMode);
    public final NumberValue<Float> balatroContrast = visibleWhen(num("chamsBalatroContrast", "balatro_contrast", 0.6f, 0.1f, 1.0f), this::isBalatroMode);
    public final NumberValue<Float> balatroLighting = visibleWhen(num("chamsBalatroLighting", "balatro_lighting", 0.6f, 0.0f, 1.0f), this::isBalatroMode);
    public final BooleanValue hideHand = bool("chamsHideHand", "hide_hand", false);
    private final ChamsMaskHelper maskHelper = new ChamsMaskHelper(this);
    private final ChamsShaderHelper shaderHelper = new ChamsShaderHelper(this);
    private final PostProcessPass handsPass = new HandsPass();
    { PostProcessManager.register(handsPass); }
    public boolean isActive() { Minecraft mc = Minecraft.getInstance(); return isEnabled() && mc.player != null && mc.level != null; }
    @Override public void onDisable() { maskHelper.onDisable(); }
    public boolean renderHandMask(GameRenderer r, CameraRenderState c, float t, Matrix4fc m) { return maskHelper.renderHandMask(r, c, t, m); }
    public boolean renderPreparedHandScene(FeatureRenderDispatcher d, SubmitNodeStorage s) { return maskHelper.renderPreparedHandScene(d, s); }
    public boolean renderIrisHandMask(GameRenderer r, CameraRenderState c, Matrix4fc m, float t) { return maskHelper.renderIrisHandMask(r, c, m, t); }
    private boolean isSmokeMode() { return ChamsModeHelper.isSmokeMode(mode.get()); }
    private boolean isMetallicMode() { return ChamsModeHelper.isMetallicMode(mode.get()); }
    private boolean isNoiseMode() { return ChamsModeHelper.isNoiseMode(mode.get()); }
    private boolean isHandEffectMode() { return ChamsModeHelper.isHandEffectMode(mode.get()); }
    private boolean isGlowVisible() { return ChamsModeHelper.isGlowVisible(mode.get(), glow.get()); }
    private boolean isShadowVisible() { return ChamsModeHelper.isShadowVisible(mode.get(), shadow.get()); }
    private boolean isCustomEffectColor() { return ChamsModeHelper.isCustomEffectColor(mode.get(), effectColorSource.get()); }
    private boolean isCustomMetallicVisualColor() { return ChamsModeHelper.isCustomMetallicVisualColor(mode.get(), visualColorSource.get()); }
    public boolean isGlassMode() { return ChamsModeHelper.isGlassMode(mode.get()); }
    private boolean isSnowMode() { return ChamsModeHelper.isSnowMode(mode.get()); }
    private boolean isChromaMode() { return ChamsModeHelper.isChromaMode(mode.get()); }
    private boolean isSolidMode() { return ChamsModeHelper.isSolidMode(mode.get()); }
    private boolean isStripesMode() { return ChamsModeHelper.isStripesMode(mode.get()); }
    private boolean isGlowShaderMode() { return ChamsModeHelper.isGlowShaderMode(mode.get()); }
    private boolean isBalatroMode() { return ChamsModeHelper.isBalatroMode(mode.get()); }
    private boolean renderHands(GpuTextureView src, GpuTextureView dst, float tickDelta) { return shaderHelper.renderHands(maskHelper, src, dst, tickDelta); }
    private final class HandsPass implements PostProcessPass {
        @Override public boolean isActive() { Minecraft mc = Minecraft.getInstance(); return Chams.this.isEnabled() && mc.player != null && mc.level != null && hands.get() && mc.options.getCameraType().isFirstPerson(); }
        @Override public Phase getPhase() { return Phase.POST_HAND; }
        @Override public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) {
            Minecraft mc = Minecraft.getInstance();
            if (!Chams.this.isEnabled() || mc.player == null || mc.level == null) return false;
            if (!hands.get() || !mc.options.getCameraType().isFirstPerson()) return false;
            return Chams.this.renderHands(src, dst, tickDelta);
        }
    }
}
