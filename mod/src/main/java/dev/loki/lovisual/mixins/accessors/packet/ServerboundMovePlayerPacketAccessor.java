package dev.loki.lovisual.mixins.accessors.packet;

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
