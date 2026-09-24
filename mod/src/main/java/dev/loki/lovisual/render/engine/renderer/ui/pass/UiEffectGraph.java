package dev.loki.lovisual.render.engine.renderer.ui.pass;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;


import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandBuffer;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;

/**
 * Owner for UI effects. Blur/liquid draw paths still execute through Renderer2D ordered
 * batches, but effect discovery is command-buffer based and not hidden in primitive flush.
 */
public final class UiEffectGraph {
    private int lastEffectCommands;

    public void execute(UiCommandBuffer commands, RenderFrameContext context, LoVisualRhi rhi) {
        try (RenderCostProfiler.Scope ignored = RenderCostProfiler.uiEffect("ui_effect_graph")) {
            int effects = 0;
            if (commands != null) {
                for (UiCommand command : commands.commands()) {
                    UiCommandKind kind = command.kind();
                    if (kind == UiCommandKind.BLUR_REGION || kind == UiCommandKind.LIQUID_GLASS_REGION || kind == UiCommandKind.EFFECT_REGION) {
                        effects++;
                        try (RenderCostProfiler.Scope ignoredEffect = RenderCostProfiler.uiEffect(kind.name().toLowerCase())) {
                            // Current draw work still runs through Renderer2D ordered batches; this scope attributes
                            // command discovery/compilation to the concrete effect kind.
                        }
                    }
                }
            }
            lastEffectCommands = effects;
        }
    }

    public int lastEffectCommands() {
        return lastEffectCommands;
    }
}
