/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.renderer.ui.UiItemSubmission;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class Renderer2DItems {
    private Renderer2DItems() {
    }

    static void submit(Renderer2D r,
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
                r.textured,
                r.alpha,
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