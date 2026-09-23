/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.prewarm;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.render.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.render.engine.svg.parser.SvgMsdfRegistry;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public enum RenderPrewarmManager {
    ;

    public static void prewarm(String reason) {
        if (!Lifecycle.isActive()) return;

        RenderPrewarmCollectEvent event = new RenderPrewarmCollectEvent(reason);
        Events.BUS.post(event);
        UiScriptStats uiScripts = prewarmUiScripts();
        if (event.isEmpty()) {
            DebugLog.renderThread("[LoVisual][Prewarm] %s: ui-scripts=%d changed, %d cached, %d errors",
                    event.reason(), uiScripts.changed(), uiScripts.cached(), uiScripts.errors());
            return;
        }

        int fonts = Fonts.prewarmRenderers(event.fonts());
        int svg = 0;
        for (Identifier id : event.svgMsdfIcons()) {
            if (SvgMsdfRegistry.preload(id)) {
                svg++;
            }
        }

        DebugLog.renderThread("[LoVisual][Prewarm] %s: fonts=%d/%d svg-msdf=%d/%d ui-scripts=%d changed, %d cached, %d errors",
                event.reason(), fonts, event.fonts().size(), svg, event.svgMsdfIcons().size(),
                uiScripts.changed(), uiScripts.cached(), uiScripts.errors());
    }

    private static UiScriptStats prewarmUiScripts() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) {
            return new UiScriptStats(0, 0, 0);
        }
        var stats = HudScriptLayouts.prewarmRegistered(mc.getResourceManager());
        return new UiScriptStats(stats.changed(), stats.unchanged(), stats.errors());
    }

    private record UiScriptStats(int changed, int cached, int errors) {
    }
}
