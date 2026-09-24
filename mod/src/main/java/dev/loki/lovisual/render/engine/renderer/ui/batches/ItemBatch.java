package dev.loki.lovisual.render.engine.renderer.ui.batches;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import dev.loki.lovisual.render.engine.renderer.ui.item.ItemDrawCommand;
import dev.loki.lovisual.render.engine.renderer.ui.item.ItemResolveKey;

public final class ItemBatch {
    public final ObjectArrayList<ItemDrawCommand> commands = new ObjectArrayList<>(64);

    final PoseStack matrices = new PoseStack();
    public final Object2ObjectOpenHashMap<ItemResolveKey, TrackingItemStackRenderState> resolvedStates = new Object2ObjectOpenHashMap<>(64);

    public @Nullable GuiGraphicsExtractor context;
    public @Nullable ScreenRectangle scissor;

    public void begin(@Nullable GuiGraphicsExtractor context, @Nullable ScreenRectangle scissor) {

        this.context = context;
        this.scissor = scissor;
        this.commands.clear();
        this.resolvedStates.clear();
    }

    public boolean canMerge(@Nullable GuiGraphicsExtractor context, @Nullable ScreenRectangle scissor) {

        return this.context == context && Objects.equals(this.scissor, scissor);
    }

    public void add(ItemDrawCommand command) {
        commands.add(command);
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }
}
