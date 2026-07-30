/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.prewarm;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.render.engine.svg.SvgMsdfRegistry;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.runtime.RuntimeGate;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public enum RenderPrewarmManager {
    ;

    public static void prewarm(String reason) {
        if (!RuntimeGate.canRunClientLogic()) return;

        RenderPrewarmCollectEvent event = new RenderPrewarmCollectEvent(reason);
        Events.BUS.post(event);
        UiScriptStats uiScripts = prewarmUiScripts();
        if (event.isEmpty()) {
            DebugLog.renderThread("[Combatant][Prewarm] %s: ui-scripts=%d changed, %d cached, %d errors",
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

        DebugLog.renderThread("[Combatant][Prewarm] %s: fonts=%d/%d svg-msdf=%d/%d ui-scripts=%d changed, %d cached, %d errors",
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
