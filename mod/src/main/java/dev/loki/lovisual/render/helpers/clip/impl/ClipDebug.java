package dev.loki.lovisual.render.helpers.clip;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiRect;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.util.logging.DebugLog;

/**
 * Diagnostic helpers for {@link ClipFunction}: one-shot warnings when the shape-clip
 * backend is unsupported or the stack overflows, and an optional colored-stencil
 * debug probe that draws a translucent overlay over the clip bounds.
 * Sibling folder sharing the parent's package.
 */
final class ClipDebug {

    private static boolean warnedShapeUnsupported;
    private static boolean warnedShapeDepth;

    private ClipDebug() { }

    /** Log-once warning when the active RHI backend rejects shape clips. */
    static void warnShapeUnsupported(UiShape shape, ShapeClipBackend backend, String reason, int depth) {
        if (warnedShapeUnsupported) return;
        warnedShapeUnsupported = true;
        UiRect b = shape.bounds();
        DebugLog.warnOnChange("clipfunction.shape.unsupported",
                reason + "|" + shape.kind() + "|" + b + "|" + depth + "|" + backend,
                "ClipFunction: shape push rejected: %s. kind=%s bounds=%s depth=%d backend=%s",
                reason, shape.kind(), b, depth, backend);
    }

    /** Log-once warning when the clip shape stack exceeds the maximum depth. */
    static void warnShapeDepth(int maxDepth) {
        if (warnedShapeDepth) return;
        warnedShapeDepth = true;
        DebugLog.warn("ClipFunction: shape clip stack exceeded %d layers. Clip push rejected.", maxDepth);
    }

    /** Draws a translucent green/red quad + border over the shape bounds when stencil debugging is enabled. */
    static void renderStencilDebugProbe(UiShape shape, int reference, boolean overlay) {
        if (!DebugLog.isStencilDebugEnabled()) return;
        UiRect b = shape.bounds();
        if (b == null || b.empty()) return;
        int fill = overlay ? 0x5AFF2A2A : 0x2600FF7A;
        int border = overlay ? 0xFFFFE600 : 0xC000FF7A;
        boolean auto = !Renderer2D.isBatching();
        if (auto) Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(b.x(), b.y(), b.width(), b.height(), fill);
        Renderer2D.COLOR.boxLines(b.x(), b.y(), b.width(), b.height(), border);
        if (auto) Renderer2D.COLOR.render();
        else Renderer2D.flushBatch(Renderer2D.FlushReason.SCISSOR);
        DebugLog.stencilOnChange("clipfunction.debug.probe",
                (overlay ? "overlay|" : "push|") + shape.kind() + "|" + reference + "|" + b,
                "ClipFunction debug probe rendered through stencil ref=%d kind=%s overlay=%s bounds=%s",
                reference, shape.kind(), overlay, b);
    }
}
