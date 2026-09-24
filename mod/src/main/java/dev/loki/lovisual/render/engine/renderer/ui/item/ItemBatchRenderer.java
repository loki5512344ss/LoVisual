package dev.loki.lovisual.render.engine.renderer.ui.item;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

public final class ItemBatchRenderer {
    private final ItemBatchMeshes meshes;
    private final ItemBatchAtlas atlas;
    private final ItemBatchGeometry geometry;
    private final ItemBatchOverlays overlays;
    public ItemBatchRenderer() {
        this.meshes = new ItemBatchMeshes();
        this.atlas = new ItemBatchAtlas();
        this.geometry = new ItemBatchGeometry();
        this.overlays = new ItemBatchOverlays();
    }
    public void init() {
        meshes.init();
    }
    public int flush(ItemBatch batch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || batch.isEmpty()) return 0;
        try (RenderCostProfiler.Scope scope = RenderCostProfiler.itemRender("item_batch:" + batch.commands.size())) {
            int drawCalls = 0;
            Object2ObjectOpenHashMap<ItemResolveKey, TrackingItemStackRenderState> resolved = batch.resolvedStates;
            resolved.clear();
            GpuTextureView prevColor = RenderSystem.outputColorTextureOverride;
            GpuTextureView prevDepth = RenderSystem.outputDepthTextureOverride;
            try {
                FeatureRenderDispatcher dispatcher = atlas.getDispatcher(mc);
                ObjectOpenHashSet<Object> identities = new ObjectOpenHashSet<>();
                for (ItemDrawCommand c : batch.commands) {
                    if (c.stack.isEmpty() || !c.drawItem || c.alpha <= 0.001f) continue;
                    TrackingItemStackRenderState st = resolved.computeIfAbsent(
                            new ItemResolveKey(c.player, c.stack, c.seed), key -> {
                                try (RenderCostProfiler.Scope s = RenderCostProfiler.itemRender("resolve_model")) {
                                    TrackingItemStackRenderState ns = new TrackingItemStackRenderState();
                                    mc.getItemModelResolver().updateForTopItem(ns, c.stack,
                                            ItemDisplayContext.GUI,
                                            c.player != null ? c.player.level() : mc.level,
                                            c.player, c.seed);
                                    return ns;
                                }
                            });
                    if (!st.isEmpty()) identities.add(st.getModelIdentity());
                }
                GuiItemAtlas atlasObj = null;
                MeshBuilder itemMesh = null;
                GpuSampler sampler = null;
                if (!identities.isEmpty()) {
                    atlasObj = atlas.ensureAtlas(mc, dispatcher, identities);
                    itemMesh = meshes.beginBlitMesh(batch.commands.size());
                    sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
                }
                GpuTextureView atlasView = null;
                GpuBufferSlice prevProj = RenderSystem.getProjectionMatrixBuffer();
                ProjectionType prevType = RenderSystem.getProjectionType();
                try {
                    if (atlasObj != null && itemMesh != null && sampler != null) {
                        atlasView = atlas.fillBlits(batch, resolved, atlasObj, itemMesh, geometry);
                    }
                } finally {
                    if (prevProj != null && prevType != null) RenderSystem.setProjectionMatrix(prevProj, prevType);
                    RenderSystem.outputColorTextureOverride = prevColor;
                    RenderSystem.outputDepthTextureOverride = prevDepth;
                }
                if (meshes.submitBlitMesh(mc, itemMesh, atlasView, sampler)) drawCalls++;
                if (atlasObj != null) atlasObj.endFrame();
                drawCalls += overlays.handleOverlays(batch, mc, meshes, geometry);
                overlays.renderTexts(batch, geometry);
            } finally {
                RenderSystem.outputColorTextureOverride = prevColor;
                RenderSystem.outputDepthTextureOverride = prevDepth;
                resolved.clear();
            }
            return drawCalls;
        }
    }
}
