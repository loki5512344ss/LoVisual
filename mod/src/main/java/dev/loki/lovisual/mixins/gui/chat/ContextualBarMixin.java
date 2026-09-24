package dev.loki.lovisual.mixins.gui.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar;
import dev.loki.lovisual.Lifecycle;

@Mixin(ContextualBar.class)
public interface ContextualBarMixin {

    @Inject(method = "extractExperienceLevel", at = @At("HEAD"), cancellable = true)
    private static void lovisual$drawExperienceLevel(GuiGraphicsExtractor context,
                                                      Font textRenderer,
                                                      int level,
                                                      CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        CustomBar bar = CustomBar.get();
        if (bar == null) return;
        if (!bar.isHudBarEnabled() || !bar.isHotbarXpBarEnabled()) return;

        ci.cancel();
    }
}

