package dev.loki.lovisual.mixins.gui.screens;

import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.mainmenu.MainMenus;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void lovisual$replaceTitleScreen(CallbackInfo ci) {

        if (MainMenus.shouldUseVanillaTitleScreen()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (MainMenus.isMainMenu(ClientScreen.current())) return;
        ClientScreen.show(mc, MainMenus.create());
        ci.cancel();
    }
}

