package dev.loki.lovisual.render.engine.command.effect;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

/**
 * Explicit marker for draw work that is executed by the ordered Renderer2D batch backend.
 *
 * <p>The command/RHI layer can see these batches instead of treating them as hidden side
 * effects inside Renderer2D. The payload is intentionally small: it is for scheduling and
 * profiling boundaries, not for replaying meshes.</p>
 */
public record UiBatchBackendCommand(String backend, String batchType) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.PRIMITIVE;
    }
}
