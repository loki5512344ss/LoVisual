package dev.loki.lovisual.features.module.modules.visuals.esp.shader;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.mixins.accessors.renderer.EntityRenderManagerAccessor;
import dev.loki.lovisual.mixins.accessors.renderer.EntityRendererAccessor;
import dev.loki.lovisual.mixins.accessors.world.LevelRendererAccessor;
import dev.loki.lovisual.render.ShaderEspRenderContext;
import dev.loki.lovisual.render.compat.EntityCullingCompat;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.msaa.MsaaFramebuffer;
import dev.loki.lovisual.render.engine.msaa.MsaaWorldTarget;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.features.module.modules.visuals.esp.ESP;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import dev.loki.lovisual.features.module.modules.visuals.esp.shader.EspShaderRenderer;

public final class EspShaderMask {

    private static ProjectionMatrixBuffer shaderEspProjection;

    private EspShaderMask() {
    }

    static boolean renderEntityMask(ESP esp, List<EspModel.RenderEntry> entries, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (entries.isEmpty() || esp.shaderMask == null) {
            return false;
        }

        LevelRenderer worldRenderer = mc.levelRenderer;
        if (worldRenderer == null) {
            return false;
        }

        MsaaFramebuffer worldMsaa = MsaaWorldTarget.getMsaaFramebuffer();
        boolean useMsaaMask = worldMsaa != null
                && worldMsaa.getSamples() > 1
                && esp.shaderMaskMsaa != null;
        RenderTarget target = useMsaaMask ? esp.shaderMaskMsaa : esp.shaderMask;

        var colorTex = target.getColorTexture();
        if (colorTex == null) {
            return false;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(colorTex, new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));

        var depthTex = target.getDepthTexture();
        if (depthTex != null) {
            encoder.clearDepthTexture(depthTex, 1.0);
        }

        GpuTextureView prevColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView prevDepth = RenderSystem.outputDepthTextureOverride;
        GpuBufferSlice prevProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType prevProjectionType = RenderSystem.getProjectionType();
        Matrix4f prevMeshProjection = MeshRenderer.projection();
        Matrix4f prevWorldProjection = new Matrix4f(RenderState.worldProjection);
        boolean prevRendering3D = RenderState.rendering3D;

        RenderSystem.outputColorTextureOverride = target.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();

        if (shaderEspProjection == null) {
            shaderEspProjection = new ProjectionMatrixBuffer("lovisual-shader-esp-projection");
        }
        Matrix4f espProjection = LoVisualWorldMatrices.renderProjectionMatrix();
        if (espProjection == null) {
            espProjection = new Matrix4f(RenderState.worldProjection);
        }
        RenderSystem.setProjectionMatrix(shaderEspProjection.getBuffer(espProjection), ProjectionType.PERSPECTIVE);
        MeshRenderer.setProjection(espProjection);
        RenderState.worldProjection.set(espProjection);
        RenderState.rendering3D = true;

        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        Matrix4f capturedPosition = LoVisualWorldMatrices.positionMatrix();
        if (capturedPosition != null) {
            modelView.mul(capturedPosition);
        } else if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            modelView.mul(new Matrix4f().rotation(mc.gameRenderer.mainCamera().rotation().conjugate(new Quaternionf())));
        }

        PoseStack matrices = new PoseStack();

        try (ShaderEspRenderContext.Scope ignored = ShaderEspRenderContext.enter()) {
            LevelRendererAccessor accessor = (LevelRendererAccessor) worldRenderer;
            LevelRenderState base = accessor.lovisual$getWorldRenderState();
            if (base == null || base.cameraRenderState == null) {
                return false;
            }

            LevelRenderState temp = new LevelRenderState();
            temp.cameraRenderState = base.cameraRenderState;
            temp.shouldShowEntityOutlines = true;

            esp.shaderEspCommandQueue.getSubmitsPerOrder().clear();

            EntityRenderDispatcher entityRenderManager = mc.getEntityRenderDispatcher();
            for (EspModel.RenderEntry entry : entries) {
                Entity entity = entry.entity();
                EntityCullingCompat.forceVisibleForShaderEsp(entity);
                EntityRenderer<?, ?> entityRenderer =
                        ((EntityRenderManagerAccessor) entityRenderManager).lovisual$invokeGetRenderer(entity);
                if (entityRenderer == null) {
                    continue;
                }

                EntityRenderState state = ((EntityRendererAccessor) entityRenderer).lovisual$invokeCreateRenderState();
                ((EntityRendererAccessor) entityRenderer).lovisual$invokeUpdateRenderState(entity, state, tickDelta);
                if (state == null) {
                    continue;
                }

                state.outlineColor = 0xFF000000 | (entry.color() & 0x00FFFFFF);
                state.shadowRadius = 0.0f;
                state.shadowPieces.clear();
                state.nameTag = null;
                state.nameTagAttachment = null;
                temp.entityRenderStates.add(state);
            }

            if (temp.entityRenderStates.isEmpty()) {
                return false;
            }

            FeatureRenderDispatcher dispatcher = EspShaderRenderer.getShaderEspRenderDispatcher(esp);
            if (dispatcher == null) {
                return false;
            }

            boolean standaloneDispatcher = dispatcher == esp.shaderEspRenderDispatcher && esp.shaderEspRenderBuffers != null;
            accessor.lovisual$invokeSubmitEntities(matrices, temp, esp.shaderEspCommandQueue);
            try {
                try (FeatureRenderDispatcher.PreparedFrame prepared = dispatcher.prepareFrame(esp.shaderEspCommandQueue)) {
                    if (!prepared.hasAnyOutline()) {
                        return false;
                    }
                    prepared.executeOutline();
                }
            } finally {
                if (standaloneDispatcher) {
                    esp.shaderEspRenderBuffers.endFrame();
                }
            }
        } finally {
            modelView.popMatrix();
            MeshRenderer.setProjection(prevMeshProjection);
            RenderState.worldProjection.set(prevWorldProjection);
            RenderState.rendering3D = prevRendering3D;
            if (prevProjection != null && prevProjectionType != null) {
                RenderSystem.setProjectionMatrix(prevProjection, prevProjectionType);
            }
            RenderSystem.outputColorTextureOverride = prevColor;
            RenderSystem.outputDepthTextureOverride = prevDepth;
            esp.shaderEspCommandQueue.getSubmitsPerOrder().clear();
        }

        return !useMsaaMask || LoVisualRenderSystem.rhi().msaa().resolve(esp.shaderMaskMsaa, esp.shaderMask, true, false);
    }
}
