package dev.loki.lovisual.mixins.network.game;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;

/**
 * Hook-only mixin: packet bodies live in handlers/ (ClientPacketListenerPvpHandler,
 * ClientPacketListenerStaffHandler, ClientPacketListenerSoundHandler). Disabled
 * legacy sound/inventory cooldown hooks are documented in ClientPacketListenerPvpHandler.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handlePlayerAbilities", at = @At("HEAD"), cancellable = true)
    private void lovisual$flightAbilities(ClientboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        // Flight module removed; no abilities override remains.
    }

    @Inject(method = "handleTabListCustomisation", at = @At("TAIL"))
    private void lovisual$captureTab(ClientboundTabListPacket packet, CallbackInfo ci) {
        ClientPacketListenerPvpHandler.captureTab(packet);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void onChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        ClientPacketListenerPvpHandler.onSystemChat(packet);
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void lovisual$onChorus(ClientboundSoundPacket packet, CallbackInfo ci) {
        // Opponent cooldown tracking disabled (legacy system; see ClientPacketListenerPvpHandler).
    }

    @Inject(method = "handlePlayerChat", at = @At("HEAD"))
    private void lovisual$pvpChatMessage(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        ClientPacketListenerPvpHandler.onPlayerChat(packet);
    }

    @Inject(method = "handleRemoveEntities", at = @At("TAIL"))
    private void lovisual$onEntitiesDestroy(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        ClientPacketListenerStaffHandler.onEntitiesDestroy(packet);
    }

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void lovisual$onEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        ClientPacketListenerStaffHandler.onEntitySpawn(packet);
    }

    @Redirect(method = "handlePlayerInfoUpdate", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void lovisual$silenceUnknownPlayerWarn(Logger instance, String format, Object arg1, Object arg2) {
        // Серверы HolyWorld/Funtime шлют UPDATE_DISPLAY_NAME для фейковых записей таба без ADD_PLAYER.
        // Ванилла флудит WARN "Ignoring player info update for unknown player" десятки раз в тик.
        // Глушим спам -> debug, чтобы лог остался читаемым. Полный update ниже всё равно трекается в lovisual$onPlayerList.
        instance.debug(format, arg1, arg2);
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private void lovisual$onPlayerList(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        ClientPacketListenerStaffHandler.onPlayerList(packet);
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("TAIL"))
    private void lovisual$onPlayerRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        ClientPacketListenerStaffHandler.onPlayerRemove(packet);
    }

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void lovisual$captureServerTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        WorldTweaks.setServerTimeOfDay(packet.gameTime());
    }

    @Inject(method = "handleTabListCustomisation", at = @At("TAIL"))
    private void lovisual$onTab(ClientboundTabListPacket packet, CallbackInfo ci) {
        ClientPacketListenerPvpHandler.parseTabMetrics(packet.header());
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void lovisual$debugSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        // PvP cooldown confirmation disabled (legacy system; see ClientPacketListenerPvpHandler).
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void lovisual$debugFullInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        // PvP cooldown confirmation disabled (legacy system; see ClientPacketListenerPvpHandler).
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void lovisual$filterSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.filterSound(packet, ci);
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"), cancellable = true)
    private void lovisual$filterSoundFromEntity(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.filterSoundFromEntity(packet, ci);
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void lovisual$heuristicParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.heuristicParticle(packet);
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void lovisual$onPearl(ClientboundSoundPacket packet, CallbackInfo ci) {
        // Opponent cooldown tracking disabled (legacy system; see ClientPacketListenerPvpHandler).
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void lovisual$onPlaySound(ClientboundSoundPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.onPlaySound(packet);
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"))
    private void lovisual$onPlaySoundFromEntity(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.onPlaySoundFromEntity(packet);
    }

    @Inject(method = "handleUpdateMobEffect", at = @At("HEAD"))
    private void lovisual$logStatusEffect(ClientboundUpdateMobEffectPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.logStatusEffect(packet);
    }

    @Inject(method = "handleRemoveMobEffect", at = @At("HEAD"))
    private void lovisual$logRemove(ClientboundRemoveMobEffectPacket packet, CallbackInfo ci) {
        ClientPacketListenerSoundHandler.logRemove(packet);
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void lovisual$onTotemSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        // Opponent cooldown tracking disabled (legacy system; see ClientPacketListenerPvpHandler).
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void lovisual$onTotemStatus(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        ClientPacketListenerPvpHandler.onTotemStatus(packet, (ClientPacketListener) (Object) this);
    }
}
