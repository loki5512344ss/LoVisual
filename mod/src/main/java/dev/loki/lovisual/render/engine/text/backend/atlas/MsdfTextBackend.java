package dev.loki.lovisual.render.engine.text.backend.atlas;

import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;
import dev.loki.lovisual.render.engine.text.glyph.CustomTextRenderer;
import dev.loki.lovisual.render.engine.text.font.info.TextBackendPreference;
import dev.loki.lovisual.render.engine.text.command.TextCommandStats;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;
import dev.loki.lovisual.render.engine.text.backend.backend.TextBackend;
import dev.loki.lovisual.render.engine.text.backend.backend.TextDrawCommand;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementTransform;

/**
 * MSDF backend for large, animated, scalable and special text.
 */
public final class MsdfTextBackend implements TextBackend {
    private final TextCommandStats stats;

    public MsdfTextBackend() {
        this(null);
    }

    public MsdfTextBackend(TextCommandStats stats) {
        this.stats = stats;
    }

    @Override
    public String id() {
        return "lovisual_msdf_text";
    }

    @Override
    public boolean supports(TextDrawCommand command) {
        if (command.preference() == TextBackendPreference.MSDF) return true;
        return command.auto() && command.renderer() instanceof CustomTextRenderer && (command.size() >= 1.75f || command.customEffects());
    }

    @Override
    public boolean supports(TextDrawCommand command, TextPlacementTransform placement) {
        if (placement != null && placement.world()) {
            return command.preference() == TextBackendPreference.MSDF || command.auto() || command.customEffects();
        }
        return supports(command);
    }

    @Override
    public void draw(TextDrawCommand command, TextPlacementTransform placement, RenderFrameContext context, LoVisualRhi rhi) {
        TextRenderer renderer = command.renderer() != null ? command.renderer() : TextRenderer.get();
        boolean wasBuilding = renderer.isBuilding();
        if (!wasBuilding) renderer.begin(command.size(), false, command.big());
        try {
            renderer.render(command.text(), command.x(), command.y(), command.color(), command.shadow());
        } finally {
            if (!wasBuilding) renderer.end();
        }
    }

    @Override
    public void drawBatch(List<TextDrawCommand> commands, int start, int end, RenderFrameContext context, LoVisualRhi rhi) {
        if (commands == null || start < 0 || end <= start || start >= commands.size()) return;
        TextDrawCommand first = commands.get(start);
        TextRenderer renderer = first.renderer() != null ? first.renderer() : TextRenderer.get();
        boolean wasBuilding = renderer.isBuilding();
        if (!wasBuilding) renderer.begin(first.size(), false, first.big());
        try {
            for (int i = start; i < end && i < commands.size(); i++) {
                TextDrawCommand command = commands.get(i);
                renderer.render(command.text(), command.x(), command.y(), command.color(), command.shadow());
            }
        } finally {
            if (!wasBuilding) renderer.end();
        }
    }
}
