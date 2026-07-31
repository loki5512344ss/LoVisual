/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntityRenderState.class)
public interface LivingEntityRenderStateAccessor {

    @Accessor("bodyRot")
    void lovisual$setBodyYaw(float bodyYaw);

    @Accessor("yRot")
    void lovisual$setRelativeHeadYaw(float relativeHeadYaw);

    @Accessor("xRot")
    void lovisual$setPitch(float pitch);
}
