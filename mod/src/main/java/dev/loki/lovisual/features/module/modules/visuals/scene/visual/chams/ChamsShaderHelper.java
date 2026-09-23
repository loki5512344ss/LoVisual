package dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.Chams;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.uniform.impl.hand.HandBlendUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.hand.HandChamsUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.hand.HandGlassUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.hand.HandMetallicUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.hand.effects.HandNoiseUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.hand.effects.HandSmokeUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public final class ChamsShaderHelper {
    private static final float NOISE_OCTAVES = 3.0f;
    private static final float NOISE_SCALE = 3.4f;
    private static final float NOISE_SPEED = 0.85f;
    private static final float NOISE_CONTRAST = 1.15f;
    private static final float NOISE_BREATHE = 0.55f;
    private static final float NOISE_DENSITY = 1.0f;
    private final Chams chams;
    private TextureTarget dualA;
    private TextureTarget dualB;
    private int bufferW = -1;
    private int bufferH = -1;

    public ChamsShaderHelper(Chams chams) { this.chams = chams; }

    private void ensureDualBuffers() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;
        if (dualA == null) {
            dualA = new TextureTarget("lovisual-hand-dual-a", w, h, true, GpuFormat.RGBA8_UNORM);
            dualB = new TextureTarget("lovisual-hand-dual-b", w, h, true, GpuFormat.RGBA8_UNORM);
        } else if (w != bufferW || h != bufferH) {
            dualA.resize(w, h); dualB.resize(w, h);
        }
        bufferW = w; bufferH = h;
    }
    private static void clearTexture(TextureTarget target) {
        if (target == null || target.getColorTexture() == null) return;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(target.getColorTexture(), new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
    }
    private float getRawTime() { return (float) (Util.getMillis() / 1000.0); }

    public boolean renderHands(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, float tickDelta) {
        if (!mask.isMaskReady()) return false;
        var maskView = mask.getMaskView();
        if (maskView == null) { mask.setMaskReady(false); return false; }
        float rawTime = getRawTime();
        if (chams.dualShader.get()) { renderDualShader(mask, src, dst, maskView, rawTime); mask.setMaskReady(false); return true; }
        String mode = chams.mode.get();
        if ("Glass".equals(mode)) renderGlass(mask, src, dst, maskView, rawTime);
        else if ("Metallic".equals(mode)) renderMetallic(mask, src, dst, maskView, rawTime);
        else if ("Smoke".equals(mode)) renderSmoke(mask, src, dst, maskView, rawTime);
        else if ("Noise".equals(mode)) renderNoise(mask, src, dst, maskView, rawTime);
        else renderChamsEffect(mask, src, dst, maskView, mode, rawTime);
        mask.setMaskReady(false); return true;
    }
    private void renderGlass(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, GpuTextureView maskView, float rawTime) {
        Minecraft mc = Minecraft.getInstance();
        int fillArgb = chams.fillColor.getArgb();
        float fillR = ChamsColorHelper.channel(fillArgb, 16);
        float fillG = ChamsColorHelper.channel(fillArgb, 8);
        float fillB = ChamsColorHelper.channel(fillArgb, 0);
        float fillA = ChamsColorHelper.channel(fillArgb, 24);
        int w = mc.getWindow().getWidth(); int h = mc.getWindow().getHeight();
        float glassAlpha = chams.hideHand.get() ? 1.0f : ChamsColorHelper.clamp01(fillA);
        float glassStrength = Math.max(0.0f, fillA * 4.0f);
        float glassLevel = Math.min(glassStrength, 4.0f);
        float edgeWidth = 12.0f + glassLevel * 2.25f;
        float refractionPx = 5.5f + glassLevel * 2.35f;
        float hazeStrength = 0.72f + Math.min(glassStrength, 3.0f) * 0.085f;
        float frostBlurPx = 5.0f + glassLevel * 1.65f;
        float bodyFrost = 0.48f + Math.min(glassStrength, 3.0f) * 0.105f;
        float chromaticPx = 1.65f + glassLevel * 0.42f;
        float edgeRefractionMul = 1.15f + glassLevel * 0.11f;
        float clarity = 0.08f;
        HandGlassUniforms.update(w, h, rawTime, edgeWidth, fillR, fillG, fillB, glassAlpha,
                glassStrength, refractionPx, hazeStrength, frostBlurPx, bodyFrost, chromaticPx, edgeRefractionMul, clarity);
        FullScreenRenderer.begin("LoVisual Fullscreen Pass").attachment(dst).pipeline(LoVisualRenderPipelines.HAND_GLASS)
                .uniform("HandGlass", HandGlassUniforms.get()).sampler("u_Src", src, PostProcessManager.getSampler())
                .sampler("u_Mask", maskView, mask.getHandMaskSampler()).end();
    }
    private void renderMetallic(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, GpuTextureView maskView, float rawTime) {
        int fillArgb = chams.fillColor.getArgb();
        int baseRgb = ChamsColorHelper.metallicBaseRgb(chams.visualColorSource.get(), chams.metallicBase.getArgb());
        int hiRgb = ChamsColorHelper.metallicHighlightRgb(chams.visualColorSource.get(), chams.metallicHighlight.getArgb());
        int glowRgb = ChamsColorHelper.glowRgb(chams.effectColorSource.get(), chams.glowColor.getArgb(), baseRgb);
        int shadowRgb = ChamsColorHelper.shadowRgb(chams.effectColorSource.get(), chams.shadowColor.getArgb(), baseRgb);
        float fillA = chams.hideHand.get() ? 1.0f : ChamsColorHelper.channel(fillArgb, 24);
        float glowA = chams.glow.get() ? chams.glowAlpha.get() : 0.0f;
        float shadowA = chams.shadow.get() ? chams.shadowAlpha.get() : 0.0f;
        HandMetallicUniforms.update(ChamsColorHelper.channel(baseRgb,16), ChamsColorHelper.channel(baseRgb,8), ChamsColorHelper.channel(baseRgb,0), fillA,
                ChamsColorHelper.channel(hiRgb,16), ChamsColorHelper.channel(hiRgb,8), ChamsColorHelper.channel(hiRgb,0), 1.0f,
                ChamsColorHelper.channel(glowRgb,16), ChamsColorHelper.channel(glowRgb,8), ChamsColorHelper.channel(glowRgb,0), glowA,
                ChamsColorHelper.channel(shadowRgb,16), ChamsColorHelper.channel(shadowRgb,8), ChamsColorHelper.channel(shadowRgb,0), shadowA,
                chams.metallicIntensity.get(), chams.metallicSharpness.get(), chams.metallicEdge.get(), rawTime,
                chams.metallicSweepSpeed.get(), chams.metallicSweepScale.get(), chams.metallicBrushedLines.get(), chams.metallicFlakes.get(),
                chams.glowStrength.get(), chams.shadowStrength.get(), chams.edgeWidth.get(), chams.metallicPrism.get());
        FullScreenRenderer.begin("LoVisual Fullscreen Pass").attachment(dst).pipeline(LoVisualRenderPipelines.HAND_METALLIC)
                .uniform("HandMetallic", HandMetallicUniforms.get()).sampler("u_Src", src, PostProcessManager.getSampler())
                .sampler("u_Mask", maskView, mask.getHandMaskSampler()).end();
    }
    private void renderSmoke(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, GpuTextureView maskView, float rawTime) {
        Minecraft mc = Minecraft.getInstance();
        int fillArgb = chams.fillColor.getArgb();
        int fillRgb = ChamsColorHelper.materialFillRgb(fillArgb, chams.visualColorSource.get());
        int glowRgb = ChamsColorHelper.glowRgb(chams.effectColorSource.get(), chams.glowColor.getArgb(), fillRgb);
        int shadowRgb = ChamsColorHelper.shadowRgb(chams.effectColorSource.get(), chams.shadowColor.getArgb(), fillRgb);
        float fillA = chams.hideHand.get() ? 1.0f : ChamsColorHelper.channel(fillArgb, 24);
        float glowA = chams.glow.get() ? chams.glowAlpha.get() : 0.0f;
        float shadowA = chams.shadow.get() ? chams.shadowAlpha.get() : 0.0f;
        int w = mc.getWindow().getWidth(); int h = mc.getWindow().getHeight();
        HandSmokeUniforms.update(ChamsColorHelper.channel(fillRgb,16), ChamsColorHelper.channel(fillRgb,8), ChamsColorHelper.channel(fillRgb,0), fillA,
                ChamsColorHelper.channel(glowRgb,16), ChamsColorHelper.channel(glowRgb,8), ChamsColorHelper.channel(glowRgb,0), glowA,
                ChamsColorHelper.channel(shadowRgb,16), ChamsColorHelper.channel(shadowRgb,8), ChamsColorHelper.channel(shadowRgb,0), shadowA,
                chams.edgeWidth.get(), chams.quality.get(), chams.smokeOctaves.get(), rawTime * chams.smokeSpeed.get(),
                w, h, chams.smokeScale.get(), chams.smokeContrast.get(), chams.smokeSwirl.get(), chams.glowStrength.get(), chams.shadowStrength.get(), chams.smokeDensity.get());
        FullScreenRenderer.begin("LoVisual Fullscreen Pass").attachment(dst).pipeline(LoVisualRenderPipelines.HAND_SMOKE)
                .uniform("HandSmoke", HandSmokeUniforms.get()).sampler("u_Src", src, PostProcessManager.getSampler())
                .sampler("u_Mask", maskView, mask.getHandMaskSampler()).end();
    }
    private void renderNoise(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, GpuTextureView maskView, float rawTime) {
        Minecraft mc = Minecraft.getInstance();
        int fillRgb = ChamsColorHelper.materialFillRgb(chams.fillColor.getArgb(), chams.visualColorSource.get());
        int glowRgb = ChamsColorHelper.glowRgb(chams.effectColorSource.get(), chams.glowColor.getArgb(), fillRgb);
        float fillA = chams.hideHand.get() ? 1.0f : ChamsColorHelper.clamp01(chams.noiseAlpha.get());
        float glowA = chams.glow.get() ? chams.glowAlpha.get() : 0.0f;
        int w = mc.getWindow().getWidth(); int h = mc.getWindow().getHeight();
        HandNoiseUniforms.update(ChamsColorHelper.channel(fillRgb,16), ChamsColorHelper.channel(fillRgb,8), ChamsColorHelper.channel(fillRgb,0), fillA,
                ChamsColorHelper.channel(glowRgb,16), ChamsColorHelper.channel(glowRgb,8), ChamsColorHelper.channel(glowRgb,0), glowA,
                chams.edgeWidth.get(), chams.quality.get(), NOISE_OCTAVES, rawTime, w, h, NOISE_SCALE, NOISE_SPEED,
                NOISE_CONTRAST, NOISE_BREATHE, chams.glowStrength.get(), NOISE_DENSITY);
        FullScreenRenderer.begin("LoVisual Fullscreen Pass").attachment(dst).pipeline(LoVisualRenderPipelines.HAND_NOISE)
                .uniform("HandNoise", HandNoiseUniforms.get()).sampler("u_Src", src, PostProcessManager.getSampler())
                .sampler("u_Mask", maskView, mask.getHandMaskSampler()).end();
    }
    public void renderChamsEffect(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, GpuTextureView maskView, String chamsMode, float rawTime) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth(); int h = mc.getWindow().getHeight();
        float time = rawTime;
        int colorA = 0xFFFFFFFF, colorB = 0xFFFFFFFF, colorC = 0xFFFFFFFF, colorD = 0xFFFFFFFF;
        float chromaSpeedV = 0.3f, chromaSat = 0.5f, chromaBright = 1.0f;
        float snowInt = 1.0f, snowSpd = 1.0f; float stripeW = 0.3f, stripeSpd = 0.3f;
        boolean glowFill = true; float glowRadiusV = 0.04f, glowPowerV = 0.75f, glowDisp = 5.0f;
        switch (chamsMode) {
            case "Snow" -> { colorA = chams.snowColor.getArgb(); snowInt = chams.snowIntensity.get(); snowSpd = chams.snowSpeed.get(); }
            case "Chroma" -> { chromaSpeedV = chams.chromaSpeed.get(); chromaSat = chams.chromaSaturation.get(); chromaBright = chams.chromaBrightness.get(); }
            case "Solid" -> { colorA = chams.solidColor1.getArgb(); colorB = chams.solidColor2.getArgb(); time = rawTime * chams.solidSpeed.get(); }
            case "Stripes" -> { colorA = chams.stripesColor1.getArgb(); colorB = chams.stripesColor2.getArgb(); stripeW = chams.stripesWidth.get(); stripeSpd = chams.stripesSpeed.get(); }
            case "Glow" -> { colorC = chams.glowShaderColor.getArgb(); glowFill = chams.glowShaderFill.get(); glowRadiusV = chams.glowShaderRadius.get(); glowPowerV = chams.glowShaderPower.get(); glowDisp = chams.glowShaderDispersion.get(); }
            case "Invert" -> {}
            default -> { colorA = chams.balatroColor1.getArgb(); colorB = chams.balatroColor2.getArgb(); colorC = chams.balatroColor3.getArgb(); colorD = ChamsColorHelper.rgb01(chams.balatroContrast.get(), chams.balatroLighting.get()); }
        }
        HandChamsUniforms.update(colorA, colorB, colorC, colorD, w, h, time,
                chams.hideHand.get() ? 1.0f : 0.85f, chromaSpeedV, chromaSat, chromaBright, snowInt, snowSpd, stripeW, stripeSpd,
                glowFill, glowRadiusV, glowPowerV, glowDisp, chams.hideHand.get());
        RenderPipeline pipeline = switch (chamsMode) {
            case "Snow" -> LoVisualRenderPipelines.HAND_CHAMS_SNOW;
            case "Chroma" -> LoVisualRenderPipelines.HAND_CHAMS_CHROMA;
            case "Solid" -> LoVisualRenderPipelines.HAND_CHAMS_SOLID;
            case "Stripes" -> LoVisualRenderPipelines.HAND_CHAMS_STRIPES;
            case "Glow" -> LoVisualRenderPipelines.HAND_CHAMS_GLOW;
            case "Invert" -> LoVisualRenderPipelines.HAND_CHAMS_INVERT;
            default -> LoVisualRenderPipelines.HAND_CHAMS_BALATRO;
        };
        FullScreenRenderer.begin("LoVisual Fullscreen Pass").attachment(dst).pipeline(pipeline)
                .uniform("HandChams", HandChamsUniforms.get()).sampler("u_Src", src, PostProcessManager.getSampler())
                .sampler("u_Mask", maskView, mask.getHandMaskSampler()).end();
    }
    private void renderDualShader(ChamsMaskHelper mask, GpuTextureView src, GpuTextureView dst, GpuTextureView maskView, float rawTime) {
        ensureDualBuffers();
        if (dualA == null || dualB == null || dualA.getColorTextureView() == null || dualB.getColorTextureView() == null) return;
        clearTexture(dualA); clearTexture(dualB);
        renderChamsEffect(mask, src, dualA.getColorTextureView(), maskView, chams.shader1.get(), rawTime);
        renderChamsEffect(mask, src, dualB.getColorTextureView(), maskView, chams.shader2.get(), rawTime);
        HandBlendUniforms.update(ChamsModeHelper.blendModeIndex(chams.blendMode.get()), chams.blendIntensity.get(), chams.shader1Strength.get(), chams.shader2Strength.get());
        FullScreenRenderer.begin("LoVisual Fullscreen Pass").attachment(dst).pipeline(LoVisualRenderPipelines.HAND_CHAMS_BLEND)
                .uniform("HandBlend", HandBlendUniforms.get()).sampler("u_Shader1", dualA.getColorTextureView(), PostProcessManager.getSampler())
                .sampler("u_Shader2", dualB.getColorTextureView(), PostProcessManager.getSampler()).sampler("u_Mask", maskView, mask.getHandMaskSampler()).end();
    }
}
