package dev.loki.lovisual.mixins.accessors.misc;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    @Accessor("destroyBlockPos")
    BlockPos lovisual$getCurrentBreakingPos();

    @Accessor("destroyProgress")
    float lovisual$getCurrentBreakingProgress();

    @Accessor("destroyProgress")
    void lovisual$setCurrentBreakingProgress(float progress);

    @Accessor("destroyDelay")
    int lovisual$getBlockBreakingCooldown();

    @Accessor("destroyDelay")
    void lovisual$setBlockBreakingCooldown(int cooldown);

    @Accessor("carriedIndex")
    int lovisual$getLastSelectedSlot();

    @Accessor("carriedIndex")
    void lovisual$setLastSelectedSlot(int slot);

    @Invoker("ensureHasSentCarriedItem")
    void lovisual$syncSelectedSlot();
}
