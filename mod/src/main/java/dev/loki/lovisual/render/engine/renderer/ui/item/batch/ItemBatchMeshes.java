package dev.loki.lovisual.render.engine.renderer.ui.item;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.ui.UIBatchUniforms;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

final class ItemBatchMeshes {
    private final int preallocatedItems;
    private MeshBuilder itemBlitMesh;
    private MeshBuilder itemDurabilityGlowMesh;
    private MeshBuilder itemDurabilityRoundedMesh;
    private MeshBuilder itemCooldownMesh;
    ItemBatchMeshes() {
        int configured = Integer.getInteger("lovisual.render.itemOverlay.preallocatedItems", 256);
        this.preallocatedItems = Math.max(32, configured);
    }
    void init() {
        itemBlitMesh = createMesh(LoVisualRenderPipelines.UI_TEXTURED, 1);
        itemDurabilityGlowMesh = createMesh(LoVisualRenderPipelines.UI_ROUNDED_GLOW_BATCH, 1);
        itemDurabilityRoundedMesh = createMesh(LoVisualRenderPipelines.UI_ROUNDED_BATCH, 3);
        itemCooldownMesh = createMesh(LoVisualRenderPipelines.UI_COLORED, 1);
    }
    MeshBuilder createMesh(RenderPipeline p, int q) {
        int quads = Math.max(1, preallocatedItems * Math.max(1, q));
        return new MeshBuilder(p.getVertexFormatBinding(0), p.getPrimitiveTopology(), quads * 4, quads * 6);
    }
    MeshBuilder beginOverlayMesh(MeshBuilder mesh, int count, int qpc) {
        if (mesh.isBuilding()) mesh.end();
        int quads = Math.max(1, Math.max(preallocatedItems, count) * Math.max(1, qpc));
        mesh.reserve(quads * 4, quads * 6); mesh.begin(); return mesh;
    }
    MeshBuilder beginBlitMesh(int c) {
        if (itemBlitMesh == null) itemBlitMesh = createMesh(LoVisualRenderPipelines.UI_TEXTURED, 1);
        return beginOverlayMesh(itemBlitMesh, c, 1);
    }
    MeshBuilder beginDurabilityGlowMesh(int c) {
        if (itemDurabilityGlowMesh == null) itemDurabilityGlowMesh = createMesh(LoVisualRenderPipelines.UI_ROUNDED_GLOW_BATCH, 1);
        return beginOverlayMesh(itemDurabilityGlowMesh, c, 1);
    }
    MeshBuilder beginDurabilityRoundedMesh(int c) {
        if (itemDurabilityRoundedMesh == null) itemDurabilityRoundedMesh = createMesh(LoVisualRenderPipelines.UI_ROUNDED_BATCH, 3);
        return beginOverlayMesh(itemDurabilityRoundedMesh, c, 3);
    }
    MeshBuilder beginCooldownMesh(int c) {
        if (itemCooldownMesh == null) itemCooldownMesh = createMesh(LoVisualRenderPipelines.UI_COLORED, 1);
        return beginOverlayMesh(itemCooldownMesh, c, 1);
    }
    boolean submitBlitMesh(Minecraft mc, @Nullable MeshBuilder mesh, @Nullable GpuTextureView view, @Nullable GpuSampler s) {
        if (mesh == null || s == null || view == null) return false;
        if (mesh.isBuilding()) mesh.end();
        if (mesh.getIndicesCount() <= 0) return false;
        MeshRenderer.begin().attachments(mc.gameRenderer.mainRenderTarget().getColorTextureView(), null)
                .pipeline(LoVisualRenderPipelines.UI_TEXTURED).mesh(mesh).sampler("u_Texture", view, s).end();
        return true;
    }
    boolean submitOverlayMesh(Minecraft mc, RenderPipeline p, @Nullable MeshBuilder mesh, boolean uniform) {
        if (mesh == null) return false;
        if (mesh.isBuilding()) mesh.end();
        if (mesh.getIndicesCount() <= 0) return false;
        MeshRenderer r = MeshRenderer.begin().attachments(mc.gameRenderer.mainRenderTarget().getColorTextureView(), null).pipeline(p).mesh(mesh);
        if (uniform) { UIBatchUniforms.update(mc.getWindow().getWidth(), mc.getWindow().getHeight()); r.uniform("UIBatch", UIBatchUniforms.get()); }
        r.end(); return true;
    }
}
