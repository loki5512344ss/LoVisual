package dev.loki.lovisual.util.network.control;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.network.packet.TransferOrigin;
import dev.loki.lovisual.util.target.TargetingUtil;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stateless helpers for {@link BacktrackController}: packet classification,
 * enemy search, boxed-distance math, delay randomization and the queued-incoming
 * probe. No controller instance state is touched. Package-private helper (impl
 * subfolder, {@code control} package).
 */
final class BacktrackEngine {

    private BacktrackEngine() {
    }

    static boolean shouldAlwaysPass(Packet<?> packet) {
        return packet instanceof ServerboundChatPacket
                || packet instanceof ServerboundChatCommandPacket
                || packet instanceof ClientboundPingPacket
                || packet instanceof ClientboundKeepAlivePacket
                || packet instanceof ClientboundSystemChatPacket
                || isPlayerHurtSound(packet);
    }

    static boolean isPlayerHurtSound(Packet<?> packet) {
        return packet instanceof ClientboundSoundPacket sound && sound.getSound().value() == SoundEvents.PLAYER_HURT
                || packet instanceof ClientboundSoundEntityPacket entitySound
                && entitySound.getSound().value() == SoundEvents.PLAYER_HURT;
    }

    static boolean shouldClearOnSafetyPacket(Packet<?> packet) {
        return packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundDisconnectPacket
                || packet instanceof ClientboundSetHealthPacket health && health.getHealth() <= 0.0f;
    }

    static boolean isTargetEntityPacket(ClientboundMoveEntityPacket packet, Entity target, Minecraft mc) {
        if (packet == null || target == null || mc == null || mc.level == null) {
            return false;
        }
        Entity packetEntity = packet.getEntity(mc.level);
        return packetEntity != null && packetEntity.getId() == target.getId();
    }

    static LivingEntity findEnemy(Minecraft mc, double minRange, double maxRange) {
        if (mc == null || mc.level == null || mc.player == null) {
            return null;
        }

        double safeMin = Math.max(0.0, Math.min(minRange, maxRange));
        double safeMax = Math.max(safeMin, Math.max(minRange, maxRange));
        AABB searchBox = mc.player.getBoundingBox().inflate(safeMax);
        double minRangeSq = safeMin * safeMin;
        double maxRangeSq = safeMax * safeMax;

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> e instanceof LivingEntity)) {
            LivingEntity living = (LivingEntity) entity;
            if (!TargetingUtil.isValidCombatTarget(living)) {
                continue;
            }

            double dist = TargetingUtil.distanceToEntityBoxSq(mc.player.getEyePosition(), living);
            if (dist >= minRangeSq && dist <= maxRangeSq && dist < bestDist) {
                best = living;
                bestDist = dist;
            }
        }
        return best;
    }

    static double boxedDistanceSq(Entity entity, Vec3 position) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || entity == null || position == null) {
            return Double.MAX_VALUE;
        }

        AABB movedBox = entity.getBoundingBox().move(position.subtract(entity.position()));
        return TargetingUtil.distanceToBoxSq(mc.player.getEyePosition(), movedBox);
    }

    static int randomBetween(int a, int b) {
        int min = Math.max(0, Math.min(a, b));
        int max = Math.max(min, Math.max(a, b));
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    static boolean hasQueuedIncoming() {
        return BlinkManager.INSTANCE.getPacketQueue().stream().anyMatch(snapshot -> snapshot.origin() == TransferOrigin.INCOMING);
    }
}
