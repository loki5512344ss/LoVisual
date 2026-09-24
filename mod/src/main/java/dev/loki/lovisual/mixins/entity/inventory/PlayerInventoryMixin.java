package dev.loki.lovisual.mixins.entity.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    public abstract ItemStack getItem(int slot);

    // Hotbar lease removed (was InventorySwap) — keep original behavior

}
