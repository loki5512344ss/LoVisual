package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.order;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.TextBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Order-состояние батчера: общий список записей отрисовки и пулы batch'ей
 * с переиспользованием. Единственный владелец порядка; flush/deferral только
 * читают entries().
 */
public final class OrderState {
    private final ObjectArrayList<Object> order = new ObjectArrayList<>(128);
    private final DrawBatchPool drawPools = new DrawBatchPool(order);
    private final ItemTextPools itemTextPools = new ItemTextPools(order);

    public boolean hasPendingWork() {
        return !order.isEmpty();
    }

    public int entryCount() {
        return order.size();
    }

    public ObjectArrayList<Object> entries() {
        return order;
    }

    public boolean onlyItems() {
        if (order.isEmpty()) return false;
        for (int i = 0, size = order.size(); i < size; i++) {
            if (!(order.get(i) instanceof ItemBatch)) {
                return false;
            }
        }
        return true;
    }

    public DrawBatch getOrCreate(UiBatchType type, GpuTextureView view, GpuSampler sampler) {
        return drawPools.getOrCreate(type, view, sampler);
    }

    public DrawBatch getOrCreateBlur(UiBatchType type, GpuTextureView view, GpuSampler sampler,
                                     Renderer2D.BlurQuality quality, float offsetPx) {
        return drawPools.getOrCreateBlur(type, view, sampler, quality, offsetPx);
    }

    public DrawBatch getOrCreateMsdf(GpuTextureView view, GpuSampler sampler, float pxRange, int atlasWidth, int atlasHeight) {
        return drawPools.getOrCreateMsdf(view, sampler, pxRange, atlasWidth, atlasHeight);
    }

    public ItemBatch getOrCreateItemBatch(@Nullable GuiGraphicsExtractor context, ItemStack stack) {
        return itemTextPools.getOrCreateItemBatch(context, stack);
    }

    public TextBatch getOrCreateTextBatch(String label, GlyphFont font, RenderPipeline pipeline,
                                          TextPlacementMode placement, boolean liquidGlassText) {
        return itemTextPools.getOrCreateTextBatch(label, font, pipeline, placement, liquidGlassText);
    }

    public void resetOrder() {
        order.clear();
        drawPools.resetCursors();
        itemTextPools.resetCursors();
    }

    public int poolTotal() {
        return drawPools.poolTotal() + itemTextPools.poolTotal();
    }

    public void closeRetained() {
        drawPools.closeRetained();
        itemTextPools.closeRetained();
    }
}
