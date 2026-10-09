package dev.loki.lovisual.mixins.input.keyboard;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.rotation.RotationUtil;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.aiming.features.MovementCorrection;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "NEW",
                    target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"
            )
    )
    private Input lovisual$applyFreeCorrection(Input original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return original;
        }

        var target = RotationManager.INSTANCE.getActiveRotationTarget();
        if (target == null
                || target.movementCorrection != MovementCorrection.SILENT
                || !target.freeCorrection) {
            return original;
        }

        Rotation rotation = RotationManager.INSTANCE.getMovementRotation();
        return RotationUtil.correctMovementInput(original, mc.player.getYRot(), rotation.yaw());
    }
}
