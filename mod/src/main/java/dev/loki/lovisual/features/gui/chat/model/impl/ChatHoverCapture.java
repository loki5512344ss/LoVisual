package dev.loki.lovisual.features.gui.chat.model;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatHoverCache;

import java.util.Optional;

/**
 * Hover-event extraction for {@link BetterChatStoreManager}: walks a message component tree and
 * records item/entity tooltips into the per-key {@link BetterChatHoverCache}.
 */
final class ChatHoverCapture {
    private ChatHoverCapture() {
    }

    static boolean captureHovers(Component text, BetterChatHoverCache cache) {
        if (text == null || cache == null) return false;
        final boolean[] changed = {false};
        text.visit((style, string) -> {
            if (style != null) {
                HoverEvent hover = style.getHoverEvent();
                if (hover instanceof HoverEvent.ShowItem(net.minecraft.world.item.ItemStackTemplate itemTemplate)) {
                    ItemStack item = itemTemplate.create();
                    String keyId = BetterChatStoreManager.hoverItemKey(item);
                    if (cache.putItem(keyId, item)) {
                        changed[0] = true;
                    }
                } else if (hover instanceof HoverEvent.ShowEntity(HoverEvent.EntityTooltipInfo entity)) {
                    String keyId = BetterChatStoreManager.hoverEntityKey(entity);
                    if (cache.putEntity(keyId, entity)) {
                        changed[0] = true;
                    }
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0];
    }
}
