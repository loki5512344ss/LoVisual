package dev.loki.lovisual.render.engine.renderer.facade.shape;

import dev.loki.lovisual.render.engine.renderer.facade.Renderer2DGlass;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.renderer.renderer2d.media.Items2D;

/** Item-slot drawing family of the Renderer2D facade. */
public class Renderer2DItem extends Renderer2DGlass {

    protected Renderer2DItem(boolean textured) {
        super(textured);
    }

    public static final int ITEM_OVERLAY_NONE = 0;
    public static final int ITEM_OVERLAY_COUNT = 1;
    public static final int ITEM_OVERLAY_DURABILITY = 1 << 1;
    public static final int ITEM_OVERLAY_COOLDOWN = 1 << 2;
    public static final int ITEM_OVERLAY_DURABILITY_TEXT = 1 << 3;
    public static final int ITEM_OVERLAY_ALL = ITEM_OVERLAY_COUNT | ITEM_OVERLAY_DURABILITY | ITEM_OVERLAY_COOLDOWN;
    private static final int DEFAULT_ITEM_DURABILITY_THRESHOLD = 100;
    private static final int DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD = 70;

    // Use this path when the current projection is already the target space
    // (for example UNSCALED_LOGICAL HUD / ClickGUI coordinates).
    public void item(ItemStack stack, double x, double y) {
        item(stack, x, y, 1.0f, 0, ITEM_OVERLAY_NONE, null);
    }

    public void item(ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText) {
        item(null, stack, x, y, scale, seed, overlayFlags, stackCountText);
    }

    public void item(ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText,
                     int durabilityThresholdPercent,
                     int durabilityTextColorThresholdPercent) {
        item(null, stack, x, y, scale, seed, overlayFlags, stackCountText,
                durabilityThresholdPercent, durabilityTextColorThresholdPercent);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText) {
        item(player, stack, x, y, scale, seed, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText,
                     int durabilityThresholdPercent,
                     int durabilityTextColorThresholdPercent) {
        item(player, stack, x, y, scale, scale, 0.0f, 0.0f, false, seed, true, overlayFlags, stackCountText,
                durabilityThresholdPercent, durabilityTextColorThresholdPercent);
    }

    public void itemPivot(ItemStack stack,
                          double x,
                          double y,
                          float scaleX,
                          float scaleY,
                          float pivotX,
                          float pivotY,
                          int seed,
                          int overlayFlags,
                          @Nullable String stackCountText) {
        item(null, stack, x, y, scaleX, scaleY, pivotX, pivotY, true, seed, true, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    public void itemOverlay(ItemStack stack,
                            double x,
                            double y,
                            float scale,
                            int overlayFlags,
                            @Nullable String stackCountText) {
        item(null, stack, x, y, scale, scale, 0.0f, 0.0f, false, 0, false, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
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
                     int durabilityTextColorThresholdPercent) {
        Items2D.submit(self(),
                player, stack, x, y, scaleX, scaleY, pivotX, pivotY, pivoted, seed, drawItem, overlayFlags,
                stackCountText, durabilityThresholdPercent, durabilityTextColorThresholdPercent);
    }

}
