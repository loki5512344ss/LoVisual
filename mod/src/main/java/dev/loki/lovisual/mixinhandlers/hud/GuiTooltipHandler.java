package dev.loki.lovisual.mixinhandlers.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.SwapTooltip;
import dev.loki.lovisual.util.item.food.IllegalItemUtil;
import dev.loki.lovisual.util.item.rarity.RarityColorUtil;
import dev.loki.lovisual.util.item.enchant.TopEnchantUtil;

/**
 * Held-item tooltip recoloring for {@link GuiMixin}. Captures the vanilla swap-item tooltip with a
 * rarity / illegal / top-enchant derived name color and a fade driven by the highlight timer,
 * cancelling the vanilla render when the custom SwapTooltip is active.
 */
public final class GuiTooltipHandler {
    private GuiTooltipHandler() {
    }

    public static void colorizeHeldItem(int toolHighlightTimer, ItemStack lastToolHighlight, Minecraft minecraft, CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        SwapTooltip swapTooltip = SwapTooltip.get();
        if (swapTooltip == null || !swapTooltip.isEnabled()) {
            return;
        }
        if (toolHighlightTimer <= 0 || lastToolHighlight == null || lastToolHighlight.isEmpty()) {
            SwapTooltip.clear();
            return;
        }

        MutableComponent name = Component.empty().append(lastToolHighlight.getHoverName());
        if (lastToolHighlight.has(DataComponents.CUSTOM_NAME)) {
            name.withStyle(ChatFormatting.ITALIC);
        }

        int illegalColor = IllegalItemUtil.illegalColor();
        int topColor = TopEnchantUtil.topColor();
        int rarityColor = argbFrom(RarityColorUtil.INSTANCE.getRarityColor(lastToolHighlight));
        boolean hasIllegal = IllegalItemUtil.isIllegal(lastToolHighlight);
        boolean hasTop = !hasIllegal && TopEnchantUtil.hasTopEnchant(lastToolHighlight);
        int nameColor = hasIllegal ? illegalColor : (hasTop ? topColor : rarityColor);
        int alpha = (int) ((float) toolHighlightTimer * 256.0F / 10.0F);
        if (alpha > 255) alpha = 255;
        if (alpha <= 0) {
            SwapTooltip.clear();
            return;
        }
        float fade = alpha / 255f;
        int argb = 0xFF000000 | (nameColor & 0x00FFFFFF);
        SwapTooltip.capture(
                name.getString(),
                argb,
                fade,
                minecraft.gameMode.canHurtPlayer()
        );
        ci.cancel();
    }

    private static int argbFrom(float[] rgba) {
        if (rgba == null || rgba.length < 3) return 0xFFFFFFFF;
        float r = rgba[0], g = rgba[1], b = rgba[2];
        float a = rgba.length > 3 ? rgba[3] : 1f;
        int ai = (int) (a * 255) & 0xFF;
        int ri = (int) (r * 255) & 0xFF;
        int gi = (int) (g * 255) & 0xFF;
        int bi = (int) (b * 255) & 0xFF;
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}
