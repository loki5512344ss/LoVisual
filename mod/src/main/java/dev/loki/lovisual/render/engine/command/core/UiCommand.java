package dev.loki.lovisual.render.engine.command.core;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

/**
 * Normalized UI command root.
 * <p>
 * Renderer2D facade methods and the newer shape/effect/text/item command families can
 * coexist while callers move toward explicit UI command boundaries.
 */
public interface UiCommand {
    UiCommandKind kind();
}
