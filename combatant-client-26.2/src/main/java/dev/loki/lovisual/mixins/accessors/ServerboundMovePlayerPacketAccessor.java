/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Mutable
    @Accessor("x")
    void lovisual$setX(double x);

    @Mutable
    @Accessor("y")
    void lovisual$setY(double y);

    @Mutable
    @Accessor("z")
    void lovisual$setZ(double z);

    @Mutable
    @Accessor("onGround")
    void lovisual$setOnGround(boolean onGround);
}
