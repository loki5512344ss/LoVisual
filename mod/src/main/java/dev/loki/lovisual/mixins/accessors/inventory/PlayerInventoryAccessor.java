package dev.loki.lovisual.mixins.accessors.inventory;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selected")
    int lovisual$getSelectedSlot();

    @Accessor("selected")
    void lovisual$setSelectedSlot(int slot);
}

