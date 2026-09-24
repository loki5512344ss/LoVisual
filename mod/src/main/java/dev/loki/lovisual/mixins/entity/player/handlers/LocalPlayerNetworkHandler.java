package dev.loki.lovisual.mixins.entity.player;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.network.EventPostSync;
import dev.loki.lovisual.events.impl.network.EventSync;
import dev.loki.lovisual.events.impl.player.PostPlayerUpdateEvent;
import dev.loki.lovisual.util.combat.rotation.SprintController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

final class LocalPlayerNetworkHandler {

    private Runnable eventPostAction;
    private EventSync activeSyncEvent;
    private boolean postUpdateLock;

    boolean onSendPositionHead(LocalPlayer self, CallbackInfo ci) {
        if (!Events.BUS.hasListeners(EventSync.class)) return false;
        eventPostAction = null;
        EventSync event = new EventSync(self.getYRot(), self.getXRot());
        Events.BUS.post(event);
        activeSyncEvent = event;
        eventPostAction = event.getPostAction();
        if (event.isCancelled()) {
            activeSyncEvent = null;
            eventPostAction = null;
            ci.cancel();
            return true;
        }
        return false;
    }

    void onSendPositionReturn() {
        if (Events.BUS.hasListeners(EventPostSync.class)) Events.BUS.post(new EventPostSync());
        if (eventPostAction != null) { eventPostAction.run(); eventPostAction = null; }
        activeSyncEvent = null;
    }

    EventSync getActiveSyncEvent() { return activeSyncEvent; }

    boolean onPostPlayerUpdate(LocalPlayer self, CallbackInfo ci, Runnable sendPosition) {
        if (!Events.BUS.hasListeners(PostPlayerUpdateEvent.class)) return false;
        if (postUpdateLock) return false;
        PostPlayerUpdateEvent event = new PostPlayerUpdateEvent();
        Events.BUS.post(event);
        if (!event.isCancelled()) return false;
        ci.cancel();
        int iterations = event.getIterations();
        if (iterations <= 0) return true;
        for (int i = 0; i < iterations; i++) {
            postUpdateLock = true;
            self.tick();
            postUpdateLock = false;
            sendPosition.run();
        }
        return true;
    }

    boolean shouldBlockSprintNetwork(boolean original) {
        if (SprintController.INSTANCE.isSprintBlocked()) return false;
        return original;
    }
}
