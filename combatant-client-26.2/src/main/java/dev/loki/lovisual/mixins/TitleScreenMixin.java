/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.mainmenu.LoVisualMainMenuScreen;
import dev.loki.lovisual.runtime.RuntimeGate;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void combatant$replaceTitleScreen(CallbackInfo ci) {
        if (RuntimeGate.isPanic()) return;
        if (LoVisualMainMenuScreen.shouldUseVanillaTitleScreen()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (ClientScreen.current() instanceof LoVisualMainMenuScreen) return;
        ClientScreen.show(mc, new LoVisualMainMenuScreen());
        ci.cancel();
    }
}




