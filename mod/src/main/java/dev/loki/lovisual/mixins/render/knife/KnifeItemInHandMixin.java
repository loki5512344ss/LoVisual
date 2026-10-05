package dev.loki.lovisual.mixins.render.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.knife.Knife;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Knife: replaces the first person sword with the skin's view model. Runs before the other hand hooks
 * (low priority) so cancelling here cannot leave their pushed poses unpopped.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class KnifeItemInHandMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void lovisual$knifeFirstPerson(AbstractClientPlayer player, float tickDelta, float pitch,
                                           InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                                           PoseStack matrices, SubmitNodeCollector queue, int light, CallbackInfo ci) {
        Knife knife = Modules.get(Knife.class);
        if (knife == null || player.isScoping()) return;
        if (knife.renderFirstPerson(player, hand, item, matrices, queue, light, equipProgress)) ci.cancel();
    }
}
