package dev.loki.lovisual.mixins.input.keyboard;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.input.KeybindIsPressedEvent;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Shadow
    private boolean isDown;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void lovisual$keybindIsPressedEvent(CallbackInfoReturnable<Boolean> cir) {
        if (!Events.BUS.hasListeners(KeybindIsPressedEvent.class)) {
            return;
        }

        KeybindIsPressedEvent event = new KeybindIsPressedEvent((KeyMapping) (Object) this, isDown);
        Events.BUS.post(event);
        cir.setReturnValue(event.isPressed());
    }
}
