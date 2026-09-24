package dev.loki.lovisual.util.network.control;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.entity.AttackEntityEvent;
import dev.loki.lovisual.events.impl.entity.EventTargetChanged;
import dev.loki.lovisual.events.impl.network.BlinkPacketEvent;
import dev.loki.lovisual.events.impl.render.GameTickEvent;
import dev.loki.lovisual.util.network.control.BacktrackModel.Config;

public final class BacktrackController {
    public static final BacktrackController INSTANCE = new BacktrackController();
    final VecDeltaCodec delayedPosition = new VecDeltaCodec();
    volatile boolean enabled;
    Config config = Config.defaults();
    Entity target;
    int currentDelayMs = BacktrackEngine.randomBetween(config.minDelayMs(), config.maxDelayMs());
    int currentChance = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 100);
    long nextAllowedBacktrackMs;
    long trackingBufferUntilMs;
    long lastAttackMs;
    boolean shouldPause;

    private BacktrackController() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        clear(!enabled);
    }

    public Config getConfig() {
        return config;
    }

    public void configure(Config config) {
        this.config = config != null ? config : Config.defaults();
        this.currentDelayMs = BacktrackEngine.randomBetween(this.config.minDelayMs(), this.config.maxDelayMs());
    }

    public boolean isLagging() {
        return enabled && BacktrackEngine.hasQueuedIncoming();
    }

    public Vec3 getTrackedPosition() {
        return delayedPosition.getBase();
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        BacktrackTargeting.processTarget(this, target);
    }

    public boolean shouldRenderTrackedBody() {
        return BacktrackTargeting.shouldRenderTrackedBody(this);
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        BacktrackTargeting.onAttack(this, event);
    }

    @EventHandler
    public void onTargetChanged(EventTargetChanged event) {
        BacktrackTargeting.onTargetChanged(this, event);
    }

    @EventHandler
    public void onTick(GameTickEvent event) {
        BacktrackTargeting.onTick(this, event);
    }

    @EventHandler
    public void onBlinkPacket(BlinkPacketEvent event) {
        BacktrackPacketFlow.onBlinkPacket(this, event);
    }

    public void clear(boolean handlePackets) {
        BacktrackTargeting.clear(this, handlePackets);
    }
}
