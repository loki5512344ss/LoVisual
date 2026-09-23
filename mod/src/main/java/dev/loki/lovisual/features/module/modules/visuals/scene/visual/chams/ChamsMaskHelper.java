package dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.Chams;
import dev.loki.lovisual.mixins.accessors.renderer.GameRendererAccessor;
import dev.loki.lovisual.mixins.iris.hand.IrisHandRendererAccessor;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.render.iris.state.IrisHandMaskState;
import net.irisshaders.iris.mixinterface.ItemInHandInterface;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;

public final class ChamsMaskHelper {
    private final Chams chams;
    private TextureTarget handMask;
    private GpuSampler handMaskSampler;
    private int bufferW = -1;
    private int bufferH = -1;
    private boolean maskReady;
    private RenderBuffers irisHandRenderBuffers;
    private FeatureRenderDispatcher irisHandFeatureDispatcher;

    public ChamsMaskHelper(Chams chams) { this.chams = chams; }

    public boolean isMaskReady() { return maskReady; }
    public void setMaskReady(boolean v) { maskReady = v; }
    public GpuTextureView getMaskView() {
        return handMask == null ? null : handMask.getColorTextureView();
    }
    public GpuSampler getHandMaskSampler() {
        if (handMaskSampler == null) {
            handMaskSampler = RenderSystem.getSamplerCache().getSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.NEAREST, FilterMode.NEAREST, false);
        }
        return handMaskSampler;
    }
    public boolean shouldRenderHand() {
        Freecam fc = Modules.get(Freecam.class);
        return fc == null || !fc.isEnabled() || fc.renderHand();
    }
    private void ensureBuffers() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;
        if (handMask == null) {
            handMask = new TextureTarget("lovisual-hand-mask", w, h, true, GpuFormat.RGBA8_UNORM);
            bufferW = w; bufferH = h;
        } else if (w != bufferW || h != bufferH) {
            handMask.resize(w, h);
            bufferW = w; bufferH = h;
        }
    }
    private FeatureRenderDispatcher getStandaloneHandFeatureDispatcher() {
        Minecraft mc = Minecraft.getInstance();
        if (irisHandFeatureDispatcher != null) return irisHandFeatureDispatcher;
        if (mc.gameRenderer == null || mc.getModelManager() == null || mc.getAtlasManager() == null || mc.font == null) return null;
        irisHandRenderBuffers = new RenderBuffers(1);
        irisHandFeatureDispatcher = new FeatureRenderDispatcher(
                irisHandRenderBuffers, mc.getModelManager(), mc.getAtlasManager(), mc.font, mc.gameRenderer.gameRenderState());
        return irisHandFeatureDispatcher;
    }
    private void endStandaloneHandFrame() {
        if (irisHandRenderBuffers != null) irisHandRenderBuffers.endFrame();
    }
    public void closeStandaloneHandRenderer() {
        if (irisHandFeatureDispatcher != null) { irisHandFeatureDispatcher.close(); irisHandFeatureDispatcher = null; }
        if (irisHandRenderBuffers != null) { irisHandRenderBuffers.close(); irisHandRenderBuffers = null; }
    }
    public void onDisable() { maskReady = false; closeStandaloneHandRenderer(); }
    public boolean renderHandMask(GameRenderer renderer, CameraRenderState cam, float tickDelta, Matrix4fc posMatrix) {
        Minecraft mc = Minecraft.getInstance();
        if (!chams.isEnabled() || mc.player == null || mc.level == null) return false;
        if (mc.gameMode == null) return false;
        if (!chams.hands.get() || !shouldRenderHand()) return false;
        if (!mc.options.getCameraType().isFirstPerson()) return false;
        maskReady = false; ensureBuffers();
        if (handMask == null) return false;
        if (mc.gameRenderer.mainRenderTarget() == null) return false;
        var colorTex = handMask.getColorTexture(); if (colorTex == null) return false;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(colorTex, new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
        var depthTex = handMask.getDepthTexture(); if (depthTex != null) encoder.clearDepthTexture(depthTex, 0.0);
        GpuTextureView prevColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView prevDepth = RenderSystem.outputDepthTextureOverride;
        boolean prevRendering3D = RenderState.rendering3D;
        RenderSystem.outputColorTextureOverride = handMask.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = handMask.getDepthTextureView();
        RenderState.rendering3D = true;
        try { ((GameRendererAccessor) renderer).invokeRenderHand(cam, tickDelta, posMatrix); }
        finally {
            RenderState.rendering3D = prevRendering3D;
            RenderSystem.outputColorTextureOverride = prevColor;
            RenderSystem.outputDepthTextureOverride = prevDepth;
        }
        maskReady = true; return true;
    }
    public boolean renderPreparedHandScene(FeatureRenderDispatcher dispatcher, SubmitNodeStorage storage) {
        Minecraft mc = Minecraft.getInstance();
        if (!chams.isEnabled() || mc.player == null || mc.level == null) return false;
        if (dispatcher == null || storage == null) return false;
        if (!chams.hands.get() || !shouldRenderHand()) return false;
        if (!mc.options.getCameraType().isFirstPerson()) return false;
        maskReady = false; ensureBuffers();
        if (handMask == null || handMask.getColorTexture() == null) return false;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(handMask.getColorTexture(), new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
        if (handMask.getDepthTexture() != null) encoder.clearDepthTexture(handMask.getDepthTexture(), 0.0);
        try (FeatureRenderDispatcher.PreparedFrame prepared = dispatcher.prepareFrame(storage)) {
            GpuTextureView prevColor = RenderSystem.outputColorTextureOverride;
            GpuTextureView prevDepth = RenderSystem.outputDepthTextureOverride;
            boolean prevRendering3D = RenderState.rendering3D;
            RenderSystem.outputColorTextureOverride = handMask.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = handMask.getDepthTextureView();
            RenderState.rendering3D = true;
            try { executePreparedHandFrame(prepared); } finally {
                RenderState.rendering3D = prevRendering3D;
                RenderSystem.outputColorTextureOverride = prevColor;
                RenderSystem.outputDepthTextureOverride = prevDepth;
            }
            maskReady = true; executePreparedHandFrame(prepared);
        }
        return true;
    }
    private static void executePreparedHandFrame(FeatureRenderDispatcher.PreparedFrame prepared) {
        prepared.executeSolid(); prepared.executeTranslucent();
        prepared.executeTranslucentAfterTerrain(); prepared.executeAlwaysOnTop();
    }
    public boolean renderIrisHandMask(GameRenderer renderer, CameraRenderState cam, Matrix4fc posMatrix, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!IrisRuntime.isShaderpackRendererActive() || !chams.isEnabled() || mc.player == null || mc.level == null) return false;
        if (!chams.hands.get() || !shouldRenderHand() || !mc.options.getCameraType().isFirstPerson()) return false;
        if (renderer == null || cam == null || posMatrix == null) return false;
        FeatureRenderDispatcher dispatcher = getStandaloneHandFeatureDispatcher();
        if (dispatcher == null) return false;
        maskReady = false; ensureBuffers();
        if (handMask == null || handMask.getColorTexture() == null) return false;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(handMask.getColorTexture(), new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
        if (handMask.getDepthTexture() != null) encoder.clearDepthTexture(handMask.getDepthTexture(), 0.0);
        GpuTextureView prevColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView prevDepth = RenderSystem.outputDepthTextureOverride;
        boolean prevRendering3D = RenderState.rendering3D;
        RenderSystem.outputColorTextureOverride = handMask.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = handMask.getDepthTextureView();
        RenderState.rendering3D = true;
        RenderSystem.backupProjectionMatrix();
        try {
            renderIrisHandMaskPhase(renderer, cam, posMatrix, dispatcher, tickDelta, true);
            renderIrisHandMaskPhase(renderer, cam, posMatrix, dispatcher, tickDelta, false);
        } finally {
            ((IrisHandRendererAccessor) HandRenderer.INSTANCE).lovisual$setRenderingSolid(false);
            RenderSystem.restoreProjectionMatrix();
            RenderState.rendering3D = prevRendering3D;
            RenderSystem.outputColorTextureOverride = prevColor;
            RenderSystem.outputDepthTextureOverride = prevDepth;
        }
        maskReady = true; return true;
    }
    private void renderIrisHandMaskPhase(GameRenderer renderer, CameraRenderState cam, Matrix4fc posMatrix,
                                         FeatureRenderDispatcher dispatcher, float tickDelta, boolean solidPhase) {
        IrisHandRendererAccessor irisHand = (IrisHandRendererAccessor) HandRenderer.INSTANCE;
        irisHand.lovisual$setRenderingSolid(solidPhase);
        PoseStack handMatrices = irisHand.lovisual$invokeSetupGlState(renderer, cam, posMatrix, tickDelta);
        handMatrices.pushPose();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix(); modelView.set(handMatrices.last().pose());
        SubmitNodeStorage handSubmits = new SubmitNodeStorage();
        IrisHandMaskState.renderBothPhases(() ->
                ((ItemInHandInterface) renderer.itemInHandRenderer).iris$renderHandsWithCustomRenderer(
                        HandRenderer.INSTANCE, tickDelta, new PoseStack(), handSubmits,
                        Minecraft.getInstance().player,
                        Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(Minecraft.getInstance().player, tickDelta)));
        try { dispatcher.renderAllFeatures(handSubmits); } finally {
            modelView.popMatrix(); handMatrices.popPose(); endStandaloneHandFrame();
        }
    }
}
