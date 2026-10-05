package dev.loki.lovisual.mixins.render.knife;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.knife.Knife;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Knife: left click plays the attack clips, right click the third one. */
@Mixin(Minecraft.class)
public abstract class KnifeInputMixin {

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void lovisual$knifePrimary(CallbackInfoReturnable<Boolean> cir) {
        Knife knife = Modules.get(Knife.class);
        if (knife != null) knife.onAttack(true);
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void lovisual$knifeSecondary(CallbackInfo ci) {
        Knife knife = Modules.get(Knife.class);
        if (knife != null) knife.onAttack(false);
    }
}
