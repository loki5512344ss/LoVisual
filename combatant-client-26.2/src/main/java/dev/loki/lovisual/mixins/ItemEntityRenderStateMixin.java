/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.loki.lovisual.mixininterface.IItemEntityRenderState;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements IItemEntityRenderState {
    @Unique
    private boolean lovisual$onGround;

    @Override
    public boolean lovisual$isOnGround() {
        return lovisual$onGround;
    }

    @Override
    public void lovisual$setOnGround(boolean onGround) {
        lovisual$onGround = onGround;
    }
}
