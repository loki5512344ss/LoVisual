package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.order;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.TextBatch;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import dev.loki.lovisual.render.engine.renderer.ui.dispatch.UiRenderDispatcher;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import dev.loki.lovisual.render.helpers.clip.ClipFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Пулы item- и text-batch'ей. Текст обязан оставаться в точном порядке отрисовки:
 * склеиваются только соседние совместимые прогоны, текст никогда не переставляется
 * через другую запись order-списка (чат, nametags, слоёные HUD, marquee/clip-стеки).
 */
final class ItemTextPools {
    private final ObjectArrayList<ItemBatch> itemPool = new ObjectArrayList<>(32);
    private final ObjectArrayList<TextBatch> textPool = new ObjectArrayList<>(32);
    private int itemPoolCursor;
    private int textPoolCursor;
    private final ObjectArrayList<Object> order;

    ItemTextPools(ObjectArrayList<Object> order) {
        this.order = order;
    }

    ItemBatch getOrCreateItemBatch(@Nullable GuiGraphicsExtractor context, ItemStack stack) {
        ScreenRectangle scissor = context != null ? context.scissorStack.peek() : null;
        if (!order.isEmpty()) {
            Object last = order.get(order.size() - 1);
            if (last instanceof ItemBatch itemBatch && itemBatch.canMerge(context, scissor)) {
                return itemBatch;
            }
        }
        ItemBatch batch = obtainItemBatch();
        batch.begin(context, scissor);
        order.add(batch);
        UiRenderDispatcher.recordBackendCommand("ITEM");
        return batch;
    }

    TextBatch getOrCreateTextBatch(String label, GlyphFont font, RenderPipeline pipeline,
                                   TextPlacementMode placement, boolean liquidGlassText) {
        if (liquidGlassText) {
            UiBlurResources.requestLiquidGlassBlur();
        }
        TextPlacementMode normalizedPlacement = placement != null ? placement : TextPlacementMode.UI;
        boolean shapeClipActive = ClipFunction.isShapeClipActive();
        if (!order.isEmpty()) {
            Object last = order.get(order.size() - 1);
            if (last instanceof TextBatch textBatch
                    && textBatch.canMerge(font, pipeline, normalizedPlacement, liquidGlassText, shapeClipActive)) {
                return textBatch;
            }
        }
        TextBatch batch = obtainTextBatch();
        batch.begin(label, font, pipeline, normalizedPlacement, liquidGlassText, shapeClipActive);
        order.add(batch);
        UiRenderDispatcher.recordBackendCommand(liquidGlassText ? "LIQUID_GLASS_TEXT" : "TEXT");
        return batch;
    }

    private ItemBatch obtainItemBatch() {
        int cursor = itemPoolCursor;
        ItemBatch batch;
        if (cursor < itemPool.size()) {
            batch = itemPool.get(cursor);
        } else {
            batch = new ItemBatch();
            itemPool.add(batch);
        }
        itemPoolCursor = cursor + 1;
        return batch;
    }

    private TextBatch obtainTextBatch() {
        int cursor = textPoolCursor;
        TextBatch batch;
        if (cursor < textPool.size()) {
            batch = textPool.get(cursor);
        } else {
            batch = new TextBatch();
            textPool.add(batch);
        }
        textPoolCursor = cursor + 1;
        return batch;
    }

    void resetCursors() {
        itemPoolCursor = 0;
        textPoolCursor = 0;
    }

    int poolTotal() {
        return itemPool.size() + textPool.size();
    }

    void closeRetained() {
        for (int i = 0, size = textPool.size(); i < size; i++) {
            TextBatch batch = textPool.get(i);
            if (batch != null && batch.mesh != null) {
                batch.mesh.close();
            }
        }
        for (int i = 0, size = itemPool.size(); i < size; i++) {
            ItemBatch batch = itemPool.get(i);
            if (batch != null) {
                batch.commands.clear();
                batch.resolvedStates.clear();
                batch.context = null;
                batch.scissor = null;
            }
        }
    }
}
