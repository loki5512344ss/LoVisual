package dev.loki.lovisual.features.module.modules.misc.items;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.features.module.modules.misc.items.ScrollMovePolicy;

/**
 * Shift-moves items with the mouse wheel: scrolling over a container screen
 * sends a QUICK_MOVE click for the hovered slot, or for the matching hotbar
 * slot when the cursor is over no slot at all.
 */
@ModuleInfo(
        id = "itemscroller",
        displayName = "Item Scroller",
        aliases = {"itemscroller", "scrollmove"},
        category = ModuleCategory.MISC
)
public class ItemScroller extends Module {
    private static final String SETTING_HOTBAR_FALLBACK = "hotbar_fallback";

    private final BooleanValue hotbarFallback =
            bool("itemScrollerHotbarFallback", SETTING_HOTBAR_FALLBACK, true);

    /** Called from AbstractContainerScreenMixin before vanilla scroll handling. */
    public boolean onMouseScrolled(double vertical, Object hoveredSlotObj, ItemStack hoveredStack) {
        if (!isEnabled() || !ScrollMovePolicy.isScrollSignificant(vertical)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return false;
        if (!(ClientScreen.current() instanceof AbstractContainerScreen<?> screen)) return false;

        AbstractContainerMenu menu = screen.getMenu();
        net.minecraft.world.inventory.Slot hovered = (net.minecraft.world.inventory.Slot) hoveredSlotObj;
        int target = hovered != null
                ? ScrollMovePolicy.resolveTargetSlot(true, hovered.index, -1, menu.slots.size())
                : hotbarFallbackTarget(menu, mc.player);

        if (!ScrollMovePolicy.canClick(menu, target, hoveredStack)) return false;
        if (hovered == null && menu.getCarried() != null && !menu.getCarried().isEmpty()) {
            // Holding an item stack: let vanilla handle the drag.
            return false;
        }

        mc.gameMode.handleContainerInput(menu.containerId, target,
                ScrollMovePolicy.quickMoveButton(), ContainerInput.QUICK_MOVE, mc.player);
        return true;
    }

    private int hotbarFallbackTarget(AbstractContainerMenu menu, Player player) {
        if (!hotbarFallback.get()) return -1;
        return ScrollMovePolicy.resolveTargetSlot(false, -1,
                player.getInventory().getSelectedSlot(), menu.slots.size());
    }}
