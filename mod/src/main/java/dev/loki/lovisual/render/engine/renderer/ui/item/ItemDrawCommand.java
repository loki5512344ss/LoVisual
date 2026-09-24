package dev.loki.lovisual.render.engine.renderer.ui.item;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemDrawCommand {
    final @Nullable GuiGraphicsExtractor context;
    final @Nullable LocalPlayer player;
    final ItemStack stack;
    final float x;
    final float y;
    final float scaleX;
    final float scaleY;
    final float pivotX;
    final float pivotY;
    final boolean pivoted;
    final int seed;
    final boolean drawItem;
    final int overlayFlags;
    final @Nullable String stackCountText;
    final int durabilityThresholdPercent;
    final int durabilityTextColorThresholdPercent;
    final float alpha;

    public ItemDrawCommand(@Nullable GuiGraphicsExtractor context,
                            @Nullable LocalPlayer player,
                            ItemStack stack,
                            float x,
                            float y,
                            float scaleX,
                            float scaleY,
                            float pivotX,
                            float pivotY,
                            boolean pivoted,
                            int seed,
                            boolean drawItem,
                            int overlayFlags,
                            @Nullable String stackCountText,
                            int durabilityThresholdPercent,
                            int durabilityTextColorThresholdPercent,
                            float alpha) {
        this.context = context;
        this.player = player;
        this.stack = stack.copy();
        this.x = x;
        this.y = y;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivoted = pivoted;
        this.seed = seed;
        this.drawItem = drawItem;
        this.overlayFlags = overlayFlags;
        this.stackCountText = stackCountText;
        this.durabilityThresholdPercent = Mth.clamp(durabilityThresholdPercent, 0, 100);
        this.durabilityTextColorThresholdPercent = Mth.clamp(durabilityTextColorThresholdPercent, 0, 100);
        this.alpha = Mth.clamp(alpha, 0.0f, 1.0f);
    }
}
