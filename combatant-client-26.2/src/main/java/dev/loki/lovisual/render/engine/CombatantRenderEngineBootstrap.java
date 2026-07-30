/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine;

import dev.loki.lovisual.render.engine.guard.RenderBoundaryAudit;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.SvgRegistry;
import dev.loki.lovisual.render.engine.visuals.CombatantVisuals;
import dev.loki.lovisual.render.iris.IrisRuntime;

public enum CombatantRenderEngineBootstrap {
    ;

    public static void init() {
        RenderBoundaryAudit.runOnce();
        // RHI backend selection must be lazy: onInitializeClient can run before RenderSystem has
        // created the final GpuDevice, which would permanently select the wrong backend on Vulkan.
        Renderer2D.init();
        IrisRuntime.registerCombatantPipelines();
        SvgRegistry.init();
        CombatantVisuals.init();
    }

}
