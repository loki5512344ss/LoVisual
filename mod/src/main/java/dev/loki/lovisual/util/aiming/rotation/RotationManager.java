package dev.loki.lovisual.util.aiming.rotation;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.input.RotationUpdateEvent;
import dev.loki.lovisual.events.impl.network.EventSync;
import dev.loki.lovisual.events.impl.network.PacketEvent;
import dev.loki.lovisual.events.impl.world.PlayerVelocityStrafe;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.mixininterface.entity.ILocalPlayer;
import dev.loki.lovisual.mixins.accessors.entity.EntityInvoker;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.MovementCorrection;
import dev.loki.lovisual.util.aiming.rotation.internal.RotationPackets;
import dev.loki.lovisual.util.aiming.rotation.internal.RotationState;
import dev.loki.lovisual.util.aiming.rotation.internal.RotationStepper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Public rotation facade: singleton entry point, event wiring and accessors.
 * The state machine lives in {@link RotationStepper}, state in
 * {@link RotationState} and server-rotation tracking in {@link RotationPackets}.
 */
public final class RotationManager {

    public static final RotationManager INSTANCE = new RotationManager();

    private final RotationState state = new RotationState();
    private final RotationPackets packets = new RotationPackets();
    private int lastLifecycleAge = Integer.MIN_VALUE;

    private RotationManager() {
    }

    public static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    public static Rotation getPlayerLastRotation() {
        LocalPlayer player = player();
        if (player instanceof ILocalPlayer access) {
            return new Rotation(access.lovisual$getLastYaw(), access.lovisual$getLastPitch(), true);
        }
        if (player != null) {
            return new Rotation(player.getYRot(), player.getXRot(), true);
        }
        return Rotation.ZERO;
    }

    public static double boxedDistanceToPlayer(Entity entity) {
        LocalPlayer player = player();
        if (player == null || entity == null) return 0.0;
        Vec3 eyes = player.getEyePosition();
        var box = entity.getBoundingBox();
        double dx = 0.0;
        if (eyes.x < box.minX) dx = box.minX - eyes.x;
        else if (eyes.x > box.maxX) dx = eyes.x - box.maxX;
        double dy = 0.0;
        if (eyes.y < box.minY) dy = box.minY - eyes.y;
        else if (eyes.y > box.maxY) dy = eyes.y - box.maxY;
        double dz = 0.0;
        if (eyes.z < box.minZ) dz = box.minZ - eyes.z;
        else if (eyes.z > box.maxZ) dz = eyes.z - box.maxZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public RotationTarget getActiveRotationTarget() {
        return state.activeTarget();
    }

    public RotationTarget getPreviousRotationTarget() {
        return state.previousTarget();
    }

    public Rotation getCurrentRotation() {
        return state.current();
    }

    public Rotation getPreviousRotation() {
        return state.previous();
    }

    public Rotation getServerRotation() {
        return packets.actual();
    }

    public Rotation getMovementRotation() {
        LocalPlayer player = player();
        Rotation current = state.current();
        Rotation fallback = player != null
                ? new Rotation(player.getYRot(), player.getXRot(), true)
                : (current != null ? current : Rotation.ZERO);

        RotationTarget active = getActiveRotationTarget();
        if (current == null || active == null || active.movementCorrection == MovementCorrection.OFF) {
            return fallback;
        }

        return current;
    }

    public void setRotationTarget(RotationTarget plan, int priority, Object provider) {
        state.request(plan, priority, provider);
    }

    public void setRotationTarget(RotationTarget plan, int priority) {
        setRotationTarget(plan, priority, null);
    }

    /**
     * Update current rotation to a new rotation step.
     */
    public void update() {
        LocalPlayer player = player();
        if (player == null) {
            clear();
            return;
        }

        RotationStepper.step(state, player, packets.actual());
    }

    public void clear() {
        state.clearAll();
    }

    public void clear(Object provider) {
        state.clearProvider(provider);
    }

    /**
     * Stops accepting new rotations from provider, but keeps current/previous state
     * so update() can return camera smoothly instead of snapping instantly.
     */
    public void release(Object provider) {
        release(provider, true);
    }

    public void release(Object provider, boolean smoothReturn) {
        state.releaseProvider(provider, smoothReturn);
    }

    @EventHandler
    public void onSync(EventSync e) {
        RotationTarget active = state.activeTarget();
        Rotation current = state.current();
        if (active == null || current == null) return;

        e.setRotation(current.yaw(), current.pitch(), true);
    }

    @EventHandler(priority = 100)
    public void onVelocityStrafe(PlayerVelocityStrafe event) {
        RotationTarget active = getActiveRotationTarget();
        if (active == null) return;
        if (active.movementCorrection == MovementCorrection.OFF) {
            return;
        }

        Rotation rotation = getMovementRotation();
        if (rotation == null) return;

        Vec3 velocity = EntityInvoker.lovisual$movementInputToVelocity(
                event.getMovementInput(),
                event.getSpeed(),
                rotation.yaw()
        );
        event.setVelocity(velocity);
    }

    @EventHandler(priority = -100)
    public void onPacketSend(PacketEvent.Send e) {
        packets.onSend(e);
    }

    @EventHandler(priority = -100)
    public void onPacketReceive(PacketEvent.Receive e) {
        packets.onReceive(e);
    }

    public void runTickLifecycle() {
        LocalPlayer player = player();
        if (player == null) {
            lastLifecycleAge = Integer.MIN_VALUE;
            return;
        }

        if (player.tickCount == lastLifecycleAge) {
            return;
        }
        lastLifecycleAge = player.tickCount;

        Events.BUS.post(new RotationUpdateEvent(RotationUpdateEvent.Type.PRE));
        update();
        Events.BUS.post(new RotationUpdateEvent(RotationUpdateEvent.Type.POST));
    }
}
