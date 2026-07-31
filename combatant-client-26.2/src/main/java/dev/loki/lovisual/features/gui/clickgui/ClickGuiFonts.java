package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.FontInfo;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;

final class ClickGuiFonts {

    private ClickGuiFonts() {
    }

    static TextRenderer getInterRegular() {
        if (ClickGuiRenderer.interRegular == null) {
            ClickGuiRenderer.interRegular = Fonts.renderer("Inter", FontInfo.Type.Regular, TextRenderer.get());
        }
        return ClickGuiRenderer.interRegular;
    }

    static Renderer2D currentRenderer() {
        return ClickGuiRenderer.renderer;
    }

    static TextRenderer getInterMedium() {
        if (ClickGuiRenderer.interMedium == null) {
            ClickGuiRenderer.interMedium = Fonts.renderer("InterMedium", FontInfo.Type.Regular, getInterRegular());
        }
        return ClickGuiRenderer.interMedium;
    }

    static TextRenderer getIosevkaRegular() {
        return getSfMedium();
    }

    static TextRenderer getSfMedium() {
        if (ClickGuiRenderer.onestMedium == null) {
            ClickGuiRenderer.onestMedium = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        }
        return ClickGuiRenderer.onestMedium;
    }

    static TextRenderer getSfProDisplaySemibold() {
        if (ClickGuiRenderer.onestBold == null) {
            ClickGuiRenderer.onestBold = Fonts.renderer("OnestBold", FontInfo.Type.Regular, getInterMedium());
        }
        return ClickGuiRenderer.onestBold;
    }

    static TextRenderer getMonsterratRegular() {
        if (ClickGuiRenderer.monsterratRegular == null) {
            ClickGuiRenderer.monsterratRegular = Fonts.renderer("Monsterrat", FontInfo.Type.Regular, TextRenderer.get());
        }
        return ClickGuiRenderer.monsterratRegular;
    }
}
