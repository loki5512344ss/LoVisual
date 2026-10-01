package dev.loki.lovisual.features.module.modules.visuals.items.held.lua;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Контекст кадра руки, который видят Lua-скрипты (как `context` в HoldMyItems). */
public record HandFrame(
        Player player,
        ItemStack stack,
        boolean mainHand,
        boolean rightArm,
        float swingProgress,
        float equipProgress,
        float tickDelta,
        PoseStack pose
) {
}
