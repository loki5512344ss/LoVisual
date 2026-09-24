package dev.loki.lovisual.render.engine.renderer.renderer2d.media;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.item.UiItemSubmission;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** ItemStack submission into the deferred item batch renderer. */
public final class Items2D {
    private Items2D() {
    }

    public static void submit(Renderer2D r,
                              @Nullable LocalPlayer player,
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
        UiItemSubmission.submit(
                r.isTextured(),
                r.getAlpha(),
                player,
                stack,
                x,
                y,
                scaleX,
                scaleY,
                pivotX,
                pivotY,
                pivoted,
                seed,
                drawItem,
                overlayFlags,
                stackCountText,
                durabilityThresholdPercent,
                durabilityTextColorThresholdPercent
        );
    }
}
