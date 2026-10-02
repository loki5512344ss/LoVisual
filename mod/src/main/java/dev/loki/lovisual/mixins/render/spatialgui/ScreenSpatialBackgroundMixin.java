package dev.loki.lovisual.mixins.render.spatialgui;

import dev.loki.lovisual.features.module.modules.visuals.spatialgui.SpatialGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While SpatialGui floats the screen in the world, the vanilla full-screen dimming gradient must not be drawn into
 * the capture: it would turn the whole plane into a dark translucent sheet around (and behind) the inventory.
 * Same fix as the reference Spatial-GUI mod's {@code ScreenMixin}.
 */
@Mixin(Screen.class)
public abstract class ScreenSpatialBackgroundMixin {

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void lovisual$noDimInSpatialCapture(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        SpatialGui module = SpatialGui.getInstance();
        if (module != null && module.isCapturing()) ci.cancel();
    }
}
