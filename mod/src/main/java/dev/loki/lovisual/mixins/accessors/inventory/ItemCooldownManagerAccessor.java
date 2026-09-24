package dev.loki.lovisual.mixins.accessors.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public interface ItemCooldownManagerAccessor {
    @Accessor("cooldowns")
    Map<Identifier, Object> getEntries();

    @Accessor("tickCount")
    int getTick();
}

