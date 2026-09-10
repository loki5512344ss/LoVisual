package dev.loki.lovisual.features.gui.clickgui.font;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;

public final class ClickGuiFonts {

    private ClickGuiFonts() {
    }

    public static TextRenderer getInterRegular() {

        if (ClickGuiRenderer.interRegular == null) {
            ClickGuiRenderer.interRegular = Fonts.renderer("Inter", FontInfo.Type.Regular, TextRenderer.get());
        }
        return ClickGuiRenderer.interRegular;
    }

    public static Renderer2D currentRenderer() {

        return ClickGuiRenderer.renderer;
    }

    public static TextRenderer getInterMedium() {

        if (ClickGuiRenderer.interMedium == null) {
            ClickGuiRenderer.interMedium = Fonts.renderer("InterMedium", FontInfo.Type.Regular, getInterRegular());
        }
        return ClickGuiRenderer.interMedium;
    }

    public static TextRenderer getIosevkaRegular() {

        return getSfMedium();
    }

    public static TextRenderer getSfMedium() {

        if (ClickGuiRenderer.onestMedium == null) {
            ClickGuiRenderer.onestMedium = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        }
        return ClickGuiRenderer.onestMedium;
    }

    public static TextRenderer getSfProDisplaySemibold() {

        if (ClickGuiRenderer.onestBold == null) {
            ClickGuiRenderer.onestBold = Fonts.renderer("OnestBold", FontInfo.Type.Regular, getInterMedium());
        }
        return ClickGuiRenderer.onestBold;
    }

    public static TextRenderer getMonsterratRegular() {

        if (ClickGuiRenderer.monsterratRegular == null) {
            ClickGuiRenderer.monsterratRegular = Fonts.renderer("Monsterrat", FontInfo.Type.Regular, TextRenderer.get());
        }
        return ClickGuiRenderer.monsterratRegular;
    }
}
