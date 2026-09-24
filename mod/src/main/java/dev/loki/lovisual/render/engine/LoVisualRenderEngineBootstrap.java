package dev.loki.lovisual.render.engine;

import dev.loki.lovisual.render.engine.guard.boundary.RenderBoundaryAudit;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.parser.SvgRegistry;
import dev.loki.lovisual.render.engine.visuals.LoVisualVisuals;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;

public enum LoVisualRenderEngineBootstrap {
    ;

    public static void init() {
        RenderBoundaryAudit.runOnce();
        // RHI backend selection must be lazy: onInitializeClient can run before RenderSystem has
        // created the final GpuDevice, which would permanently select the wrong backend on Vulkan.
        Renderer2D.init();
        IrisRuntime.registerLoVisualPipelines();
        SvgRegistry.init();
        LoVisualVisuals.init();
    }

}
