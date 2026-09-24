package dev.loki.lovisual.mixins.hud.tablist;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab.CustomTabList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$extractCustomTabList(GuiGraphicsExtractor context,
                                                int width,
                                                Scoreboard scoreboard,
                                                Objective objective,
                                                CallbackInfo ci) {

        if (!CustomTabList.shouldReplaceVanilla()) return;
        CustomTabList.renderVanillaTabStratum(context, width);
        ci.cancel();
    }
}
