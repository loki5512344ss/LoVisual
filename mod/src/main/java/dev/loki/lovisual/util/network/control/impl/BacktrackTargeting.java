package dev.loki.lovisual.util.network.control;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.impl.entity.AttackEntityEvent;
import dev.loki.lovisual.events.impl.entity.EventTargetChanged;
import dev.loki.lovisual.events.impl.render.GameTickEvent;
import dev.loki.lovisual.util.network.control.BacktrackModel.TargetMode;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stateful target acquisition and eligibility logic for {@link BacktrackController}.
 * All methods are static and mutate the controller's package-private fields through
 * the passed-in instance. Pure packet classification and geometry live in
 * {@link BacktrackEngine}. Package-private helper (impl subfolder, {@code control} package).
 */
final class BacktrackTargeting {

    private BacktrackTargeting() {
    }

    static void onAttack(BacktrackController c, AttackEntityEvent event) {
        if (!c.enabled || c.config.targetMode() != TargetMode.ATTACK || event == null) {
            return;
        }

        c.lastAttackMs = System.currentTimeMillis();
        c.currentChance = ThreadLocalRandom.current().nextInt(0, 100);
        processTarget(c, event.getTarget());
    }

    static void onTargetChanged(BacktrackController c, EventTargetChanged event) {
        if (!c.enabled) {
            return;
        }
        if (c.config.targetMode() != TargetMode.ATTACK) {
            return;
        }
        if (event.currentSource == dev.loki.lovisual.util.target.TargetManager.Source.ATTACK && event.current != null) {
            c.lastAttackMs = System.currentTimeMillis();
            c.currentChance = ThreadLocalRandom.current().nextInt(0, 100);
            processTarget(c, event.current);
        }
    }

    static void onTick(BacktrackController c, GameTickEvent event) {
        if (!c.enabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            clear(c, true);
            return;
        }

        if (c.config.targetMode() == TargetMode.RANGE) {
            LivingEntity enemy = BacktrackEngine.findEnemy(mc, c.config.minRange(), c.config.maxRange());
            if (enemy != null) {
                processTarget(c, enemy);
            } else if (c.target != null || BacktrackEngine.hasQueuedIncoming()) {
                clear(c, true);
            }
        }

        boolean hadQueuedIncoming = BacktrackEngine.hasQueuedIncoming();
        boolean shouldCancel = shouldCancelPackets(c);
        if (shouldCancel) {
            long now = System.currentTimeMillis();
            BlinkManager.INSTANCE.flush(snapshot ->
                    snapshot.origin() == dev.loki.lovisual.util.network.packet.TransferOrigin.INCOMING
                            && snapshot.timestamp() <= now - c.currentDelayMs
            );
        } else if (hadQueuedIncoming) {
            BlinkManager.INSTANCE.flush(dev.loki.lovisual.util.network.packet.TransferOrigin.INCOMING);
        }

        if (!BacktrackEngine.hasQueuedIncoming()) {
            c.currentDelayMs = BacktrackEngine.randomBetween(c.config.minDelayMs(), c.config.maxDelayMs());
        }
    }

    static void processTarget(BacktrackController c, Entity enemy) {
        if (!(enemy instanceof LivingEntity living)) {
            return;
        }

        c.shouldPause = living.hurtTime >= c.config.pauseHurtTime();
        if (!shouldBacktrack(c, living)) {
            return;
        }

        if (enemy != c.target) {
            clear(c, true, false, false);
            c.delayedPosition.setBase(enemy.getPositionCodec().getBase());
        }

        c.target = enemy;
    }

    static boolean shouldCancelPackets(BacktrackController c) {
        return c.target instanceof LivingEntity living && living.isAlive() && shouldBacktrack(c, living);
    }

    static boolean shouldBacktrack(BacktrackController c, LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !dev.loki.lovisual.util.target.TargetingUtil.isValidCombatTarget(entity)) {
            return false;
        }

        long now = System.currentTimeMillis();
        boolean inRange = BacktrackEngine.boxedDistanceSq(entity, entity.position()) >= c.config.minRange() * c.config.minRange()
                && BacktrackEngine.boxedDistanceSq(entity, entity.position()) <= c.config.maxRange() * c.config.maxRange();
        if (inRange) {
            c.trackingBufferUntilMs = now + c.config.trackingBufferMs();
        }

        boolean attackWindow = c.config.targetMode() != TargetMode.ATTACK
                || now - c.lastAttackMs <= c.config.lastAttackTimeToWorkMs();

        return (inRange || now < c.trackingBufferUntilMs)
                && mc.player.tickCount > 10
                && c.currentChance < c.config.chancePercent()
                && now >= c.nextAllowedBacktrackMs
                && !shouldPause(c)
                && attackWindow;
    }

    static boolean shouldPause(BacktrackController c) {
        return c.config.pauseOnHurtTime() && c.shouldPause;
    }

    static void clear(BacktrackController c, boolean handlePackets) {
        clear(c, handlePackets, false, true);
    }

    static void clear(BacktrackController c, boolean handlePackets, boolean clearOnly, boolean resetNextBacktrackDelay) {
        if (handlePackets && !clearOnly) {
            BlinkManager.INSTANCE.flush(dev.loki.lovisual.util.network.packet.TransferOrigin.INCOMING);
        } else if (clearOnly) {
            BlinkManager.INSTANCE.getPacketQueue().removeIf(snapshot ->
                    snapshot.origin() == dev.loki.lovisual.util.network.packet.TransferOrigin.INCOMING);
        }

        if (c.target != null && resetNextBacktrackDelay) {
            c.nextAllowedBacktrackMs = System.currentTimeMillis()
                    + BacktrackEngine.randomBetween(c.config.minNextBacktrackDelayMs(), c.config.maxNextBacktrackDelayMs());
        }

        c.target = null;
        c.delayedPosition.setBase(Vec3.ZERO);
        c.shouldPause = false;
    }

    static boolean shouldRenderTrackedBody(BacktrackController c) {
        Entity currentTarget = c.target;
        if (!c.enabled
                || !(currentTarget instanceof LivingEntity living)
                || !living.isAlive()
                || c.delayedPosition.getBase().equals(Vec3.ZERO)) {
            return false;
        }

        return c.config.targetMode() == TargetMode.RANGE
                || BacktrackEngine.hasQueuedIncoming()
                || System.currentTimeMillis() - c.lastAttackMs <= c.config.lastAttackTimeToWorkMs();
    }
}
