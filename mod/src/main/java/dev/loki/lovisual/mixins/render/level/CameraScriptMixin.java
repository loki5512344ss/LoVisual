package dev.loki.lovisual.mixins.render.level;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.items.held.HoldMyItems;
import dev.loki.lovisual.features.module.modules.visuals.items.held.lua.HandScripts;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Смещение камеры из Lua-скриптов HoldMyItems ({@code C:setCamRot}, {@code C:setCamPos}). */
@Mixin(Camera.class)
public abstract class CameraScriptMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void move(float surge, float heave, float sway);

    @Shadow
    public abstract float yRot();

    @Shadow
    public abstract float xRot();

    @Inject(method = "update", at = @At("TAIL"))
    private void lovisual$applyScriptCamera(DeltaTracker tickCounter, CallbackInfo ci) {
        HoldMyItems held = Modules.get(HoldMyItems.class);
        HandScripts scripts = held == null ? null : held.scriptsOrNull();
        float[] c = scripts == null ? null : scripts.vanilla().cameraOffset();
        if (c == null) return;
        if (c[0] != 0.0f || c[1] != 0.0f) setRotation(yRot() + c[0], Math.max(-90.0f, Math.min(90.0f, xRot() + c[1])));
        if (c[2] != 0.0f || c[3] != 0.0f || c[4] != 0.0f) move(c[2], c[3], c[4]);
    }
}
