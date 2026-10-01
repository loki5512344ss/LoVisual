package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.mainmenu.hero.LoVisualHeroMenuScreen;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/** Picks the main menu implementation from {@code menuLayout} and owns the vanilla fallback. */
public final class MainMenus {
    private MainMenus() {
    }

    public static Screen create() {
        MainConfig cfg = MainConfig.get();
        boolean hero = cfg != null && "hero".equalsIgnoreCase(cfg.getMenuLayout());
        return hero ? new LoVisualHeroMenuScreen() : new LoVisualMainMenuScreen();
    }

    public static boolean isMainMenu(Screen screen) {
        return screen instanceof LoVisualMainMenuScreen || screen instanceof LoVisualHeroMenuScreen;
    }

    public static boolean shouldUseVanillaTitleScreen() {
        return LoVisualMainMenuScreen.shouldUseVanillaTitleScreen();
    }

    /** Shows the vanilla title screen; a non-null error also disables the custom menu for this session. */
    public static void fallbackToVanilla(Throwable error) {
        if (error != null) {
            LoVisualMainMenuScreen.forceVanillaTitleScreen = true;
            DebugLog.errorOnce("main_menu_vanilla_fallback", "Main menu failed, switching to vanilla title screen", error);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && !(ClientScreen.current() instanceof TitleScreen)) {
            ClientScreen.show(mc, new TitleScreen(false));
        }
    }
}
