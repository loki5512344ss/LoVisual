package dev.loki.lovisual.mixins.entity.player;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.input.SprintControlEvent;
import dev.loki.lovisual.events.impl.movement.MovementInputEvent;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.mixins.accessors.misc.InputAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

final class LocalPlayerInputHandler {

    boolean shouldBlockWASD() {
        Freecam fc = Modules.get(Freecam.class);
        return fc != null && fc.isEnabled() && fc.isCameraInput();
    }

    void handleFreecamAfterInputTick(LocalPlayer self) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc == null || !fc.isEnabled() || !fc.isCameraInput()) return;
        if (self.input == null) return;
        ((InputAccessor) self.input).setMovementVector(Vec2.ZERO);
        boolean sprint = self.input.keyPresses.sprint();
        self.input.keyPresses = new Input(false, false, false, false, false, false, sprint);
    }

    void handleAfterInputTickSprint(LocalPlayer self) {
        if (self.input == null) return;
        Input original = self.input.keyPresses;
        if (original == null) return;
        Input input = original;
        if (Events.BUS.hasListeners(MovementInputEvent.class)) {
            MovementInputEvent event = new MovementInputEvent(input);
            Events.BUS.post(event);
            Input converted = event.toPlayerInput();
            if (!sameInput(input, converted)) input = converted;
        }
        Input transformed = transformDirection(input);
        if (!sameInput(input, transformed)) input = transformed;
        if (Events.BUS.hasListeners(SprintControlEvent.class)) {
            float forward = impulse(input.forward(), input.backward());
            float sideways = impulse(input.left(), input.right());
            SprintControlEvent event = new SprintControlEvent(forward, sideways, input.sprint(), SprintControlEvent.Source.INPUT);
            Events.BUS.post(event);
            if (event.shouldSprint() != input.sprint()) {
                input = new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), event.shouldSprint());
            }
        }
        if (!sameInput(original, input)) applyPlayerInput(self, input);
    }

    void applyPlayerInput(LocalPlayer self, Input input) {
        if (self.input == null || input == null) return;
        InputAccessor accessor = (InputAccessor) self.input;
        accessor.setPlayerInput(input);
        float forward = impulse(input.forward(), input.backward());
        float sideways = impulse(input.left(), input.right());
        accessor.setMovementVector(new Vec2(sideways, forward).normalized());
    }

    Input transformDirection(Input input) { return input; }

    boolean postSprintControl(LocalPlayer self, boolean original, SprintControlEvent.Source source) {
        if (self.input == null) return original;
        if (!Events.BUS.hasListeners(SprintControlEvent.class)) return original;
        Vec2 movement = self.input.getMoveVector();
        SprintControlEvent event = new SprintControlEvent(movement.y, movement.x, original, source);
        Events.BUS.post(event);
        return event.shouldSprint();
    }

    boolean redirectShift(Input in, LocalPlayer self) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && fc.isCameraInput() && self.getAbilities().flying) return false;
        return in.shift();
    }

    boolean sameInput(Input a, Input b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.forward() == b.forward() && a.backward() == b.backward() && a.left() == b.left() && a.right() == b.right() && a.jump() == b.jump() && a.shift() == b.shift() && a.sprint() == b.sprint();
    }

    float impulse(boolean positive, boolean negative) {
        if (positive == negative) return 0.0f;
        return positive ? 1.0f : -1.0f;
    }
}
