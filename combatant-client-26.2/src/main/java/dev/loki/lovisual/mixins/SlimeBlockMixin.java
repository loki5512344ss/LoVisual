/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.movement.NoStun;

@Mixin(SlimeBlock.class)
public class SlimeBlockMixin {

    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void nostun$onSteppedOn(Level world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        NoStun ns = Modules.get(NoStun.class);
        if (ns == null || !ns.isEnabled()) return;
        if (!ns.isFunctionEnabled(NoStun.fnEnvBlocks())) return;
        if (!ns.isEnvBlockEnabled(NoStun.envSlime())) return;
        ci.cancel();
    }
}
