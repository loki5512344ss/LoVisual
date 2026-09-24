package dev.loki.lovisual.render.engine.renderer.ui.item;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;

final class ItemBatchAtlas {
    private FeatureRenderDispatcher dispatcher;
    private GuiItemAtlas atlas;
    private int slotTextureSize;
    private int textureSize;
    FeatureRenderDispatcher getDispatcher(Minecraft mc) {
        if (dispatcher == null) dispatcher = new FeatureRenderDispatcher(mc.gameRenderer.renderBuffers(),
                mc.getModelManager(), mc.getAtlasManager(), mc.font, mc.gameRenderer.gameRenderState());
        return dispatcher;
    }
    GuiItemAtlas ensureAtlas(Minecraft mc, FeatureRenderDispatcher d, ObjectOpenHashSet<Object> ids) {
        int guiScale = Math.max(1, (int) Math.round(mc.getWindow().getGuiScale()));
        int slotSize = Math.max(16, 16 * guiScale);
        int req = GuiItemAtlas.computeTextureSizeFor(slotSize, Math.max(1, ids.size()));
        if (atlas != null && slotTextureSize == slotSize && textureSize >= req && !atlas.tryPrepareFor(ids)) closeAtlas();
        if (atlas == null || slotTextureSize != slotSize || textureSize < req) {
            closeAtlas(); atlas = new GuiItemAtlas(d, req, slotSize); slotTextureSize = slotSize; textureSize = req;
        }
        return atlas;
    }
    void closeAtlas() { if (atlas != null) { atlas.close(); atlas = null; } slotTextureSize = 0; textureSize = 0; }
    GpuTextureView fillBlits(dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch batch,
                             Object2ObjectOpenHashMap<ItemResolveKey, TrackingItemStackRenderState> resolved,
                             GuiItemAtlas atlasObj, MeshBuilder mesh, ItemBatchGeometry g) {
        GpuTextureView view = null;
        for (ItemDrawCommand c : batch.commands) {
            if (c.stack.isEmpty() || !c.drawItem || c.alpha <= 0.001f) continue;
            TrackingItemStackRenderState st = resolved.get(new ItemResolveKey(c.player, c.stack, c.seed));
            if (st == null || st.isEmpty()) continue;
            try (RenderCostProfiler.Scope ign = RenderCostProfiler.itemRender("atlas_model")) {
                GuiItemAtlas.SlotView slot = atlasObj.getOrUpdate(st);
                if (slot != null && slot.textureView() != null) { view = slot.textureView(); g.appendItemAtlasBlit(mesh, c, slot); }
            }
        }
        return view;
    }
}
