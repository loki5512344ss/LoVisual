package dev.loki.lovisual.mixinhandlers.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.sound.NoSound;
import dev.loki.lovisual.features.module.modules.visuals.esp.audio.SoundESP;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.player.effect.heuristics.StatusEffectHeuristics;

/**
 * Sound, particle and status-effect packet bodies for {@link ClientPacketListenerMixin}.
 * Hook-only mixin delegates here so the injection class stays focused on the
 * {@code @Inject}/{@code @Redirect} wiring. Physically under {@code handlers/} but
 * declares the parent package for direct access.
 */
public final class ClientPacketListenerSoundHandler {

    private ClientPacketListenerSoundHandler() {
    }

    public static void filterSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        NoSound module = Modules.get(NoSound.class);
        if (module == null || !module.isEnabled()) return;
        if (packet == null || packet.getSound() == null) return;
        String id = packet.getSound().value().location().toString().toLowerCase();
        if (module.shouldMute(id)) {
            ci.cancel();
        }
    }

    public static void filterSoundFromEntity(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        NoSound module = Modules.get(NoSound.class);
        if (module == null || !module.isEnabled()) return;
        if (packet == null || packet.getSound() == null) return;
        String id = packet.getSound().value().location().toString().toLowerCase();
        if (module.shouldMute(id)) {
            ci.cancel();
        }
    }

    public static void onPlaySound(ClientboundSoundPacket packet) {
        SoundESP esp = Modules.get(SoundESP.class);
        if (esp == null || !esp.isEnabled()) return;
        if (packet == null || packet.getSound() == null) return;
        Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        esp.handleSound(packet.getSound().value().location(), pos);
    }

    public static void onPlaySoundFromEntity(ClientboundSoundEntityPacket packet) {
        SoundESP esp = Modules.get(SoundESP.class);
        if (esp == null || !esp.isEnabled()) return;
        if (packet == null || packet.getSound() == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(packet.getId());
        if (entity == null) return;

        esp.handleSound(packet.getSound().value().location(), entity.position());
    }

    public static void heuristicParticle(ClientboundLevelParticlesPacket packet) {
        var p = packet.getParticle();
        // Log raw particles once per hook for diagnostics
        DebugLog.info("Particle packet type=%s pos=%.2f %.2f %.2f count=%d",
                p.getType().toString(),
                packet.getX(), packet.getY(), packet.getZ(),
                packet.getCount());
        StatusEffectHeuristics.handle(p, packet.getX(), packet.getY(), packet.getZ());
    }

    public static void logStatusEffect(ClientboundUpdateMobEffectPacket packet) {
        DebugLog.server("Packet: add/refresh effect eid=%d effect=%s amp=%d dur=%d particles=%s icon=%s ambient=%s keepFading=%s",
                packet.getEntityId(),
                packet.getEffect().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown"),
                packet.getEffectAmplifier(),
                packet.getEffectDurationTicks(),
                packet.isEffectVisible(),
                packet.effectShowsIcon(),
                packet.isEffectAmbient(),
                packet.shouldBlend()
        );
    }

    public static void logRemove(ClientboundRemoveMobEffectPacket packet) {
        DebugLog.server("Packet: remove effect eid=%d (%s) effect=%s",
                packet.entityId(),
                resolveName(packet.entityId()),
                packet.effect().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown"));
    }

    private static String resolveName(int entityId) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return "unknown";
        Entity e = mc.level.getEntity(entityId);
        if (e == null) return "unknown";
        if (e instanceof Player p) return p.getName().getString();
        return e.getName().getString();
    }
}
