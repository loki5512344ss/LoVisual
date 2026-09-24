package dev.loki.lovisual.features.gui.hud.base;

import dev.loki.lovisual.events.impl.render.RenderPrewarmCollectEvent;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

/**
 * Registers the common font set every HUD element may draw with, so the render pipeline can
 * pre-warm them before the first frame. Used by {@link BaseHudElement#collectRenderPrewarm}.
 */
final class HudPrewarm {

    private HudPrewarm() { }

    /** Queue the standard HUD font atlas set (Inter / Onest / Iosevka / icon fonts) for pre-warm. */
    static void standard(RenderPrewarmCollectEvent event) {
        if (event == null) return;
        event.font("Inter", FontInfo.Type.Regular).font("Inter", FontInfo.Type.Bold)
                .font("InterMedium", FontInfo.Type.Regular).font("OnestMedium", FontInfo.Type.Regular)
                .font("Onest", FontInfo.Type.Regular).font("Onest", FontInfo.Type.Bold)
                .font("OnestMedium", FontInfo.Type.Regular).font("OnestBold", FontInfo.Type.Regular)
                .font("Iosevka", FontInfo.Type.Regular).font("Iosevka", FontInfo.Type.Bold)
                .font("Comfortaa", FontInfo.Type.Regular).font("Icons", FontInfo.Type.Regular)
                .font("IconsNur", FontInfo.Type.Regular).font("WeatherIcons", FontInfo.Type.Regular)
                .font("MediaPlayer", FontInfo.Type.Regular);
    }
}
