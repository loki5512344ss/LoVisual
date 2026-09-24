package dev.loki.lovisual.mixins.hud.tooltips;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;

@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {

    @Inject(method = "addToast(Lnet/minecraft/client/gui/components/toasts/Toast;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$noRender$hideToasts(Toast toast, CallbackInfo ci) {
        NoRender module = Modules.get(NoRender.class);
        if (module != null && module.hideToastHints()) {
            ci.cancel();
        }
    }
}
