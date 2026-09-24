package dev.loki.lovisual.mixins.network.packet;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.protect.NoServerPack;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {

    @Final
    @Shadow
    protected Connection connection;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$flightSendPacket(Packet<?> packet, CallbackInfo ci) {
        // Flight module removed; no move-packet override remains.
    }

    @Inject(
            method = "handleResourcePackPush(Lnet/minecraft/network/protocol/common/ClientboundResourcePackPushPacket;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$onResourcePackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        NoServerPack module = Modules.get(NoServerPack.class);
        if (module == null || !module.shouldDeclinePush()) return;
        // Same terminal answer vanilla sends when the pack is refused, but
        // before any prompt screen or download can start.
        connection.send(new ServerboundResourcePackPacket(
                packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
        module.onPackDeclined(packet.id());
        ci.cancel();
    }
}
