package dev.loki.lovisual.mixins.security.net;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.security.BackdoorProtection;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class ClientLoginNetworkHandlerMixin {

    @Shadow
    @Final
    private Connection connection;

    @Inject(method = "handleLoginFinished", at = @At("HEAD"))
    private void lovisual$backdoor$captureServerConnection(ClientboundLoginFinishedPacket packet, CallbackInfo ci) {
        BackdoorProtection.INSTANCE.updateServerConnection(connection);
    }
}
