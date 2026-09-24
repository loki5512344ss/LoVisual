package dev.loki.lovisual.mixins.accessors.inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface ItemCooldownEntryAccessor {
    @Accessor("endTime")
    int getEndTick();
}
