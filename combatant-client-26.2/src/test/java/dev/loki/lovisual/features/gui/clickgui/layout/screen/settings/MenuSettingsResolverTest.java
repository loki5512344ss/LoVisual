package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.hud.HudElementAutoLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MenuSettingsResolverTest {

    @BeforeAll
    static void loadWidgets() {
        HudElementAutoLoader.loadDraggable("dev.loki.lovisual.features.gui.hud.draggable.impl");
    }

    @Test
    void hudCategoryBuildsNonEmptyCards() {
        List<ModuleComponent.CardEntry> cards = MenuSettingsResolver.buildCards(MenuScreen.Category.HUD);
        assertFalse(cards.isEmpty(), "HUD category should list registered draggable widgets");
    }

    @Test
    void uiCategoryBuildsNonEmptyCards() {
        List<ModuleComponent.CardEntry> cards = MenuSettingsResolver.buildCards(MenuScreen.Category.UI);
        assertFalse(cards.isEmpty(), "UI category should list static HUD cards");
    }

    @Test
    void hudWidgetSettingsResolve() {
        List<ModuleComponent.CardEntry> cards = MenuSettingsResolver.buildCards(MenuScreen.Category.HUD);
        assertFalse(cards.isEmpty());
        String id = cards.get(0).id();
        MenuSettingsResolver.ResolvedSettings resolved = MenuSettingsResolver.resolveSettings(id);
        assertNotNull(resolved, "settings should resolve for HUD widget " + id);
    }
}
