package dev.loki.lovisual.render.helpers.clip;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiRect;
import dev.loki.lovisual.render.engine.renderer.ui.draw.corner.UiCornerMode;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShapeKind;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Higher-level clipping stack: rectangles route to {@link ScissorFunction};
 * non-rect primitives use the active RHI shape-clip backend (GL → stencil).
 * Geometry rasterization lives in {@link ClipGeometry}, diagnostics in {@link ClipDebug}.
 */
public enum ClipFunction {
    ;
    private static final int MAX_SHAPE_DEPTH = 250;
    private static final Deque<Layer> STACK = new ArrayDeque<>();

    // ---- Public push API --------------------------------------------------------------------

    public static boolean pushRaw(float x, float y, float width, float height) { return pushRect(x, y, width, height); }

    public static boolean pushRect(double x, double y, double width, double height) {
        boolean pushed = ScissorFunction.pushRaw((float) x, (float) y, (float) width, (float) height);
        if (pushed) STACK.push(Layer.rect());
        return pushed;
    }

    public static boolean pushRoundedRect(double x, double y, double w, double h, double r) { return push(UiShape.roundedRect(x, y, w, h, r)); }

    public static boolean pushRoundedRect(double x, double y, double w, double h, double tl, double tr, double br, double bl) {
        return push(UiShape.roundedRect(x, y, w, h, tl, tr, br, bl));
    }

    public static boolean pushChamferedRect(double x, double y, double w, double h, double c) { return push(UiShape.chamferedRect(x, y, w, h, c, c, c, c)); }
    public static boolean pushCircle(double cx, double cy, double r) { return push(UiShape.circle(cx, cy, r)); }

    /** Push an arbitrary shape clip; plain rects fall through to scissor, others go through the stencil backend. */
    public static boolean push(UiShape shape) {
        if (shape == null) return false;
        UiRect bounds = shape.bounds();
        if (bounds == null || bounds.empty()) return false;
        if (isPlainRect(shape)) return pushRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        if (activeShapeDepth() >= MAX_SHAPE_DEPTH) { ClipDebug.warnShapeDepth(MAX_SHAPE_DEPTH); return false; }
        Renderer2D.flushBatch(Renderer2D.FlushReason.SCISSOR);
        Renderer2D.prepareLiquidGlassBlurBeforeShapeClipIfRequested();
        ShapeClipBackend clip = clipBackend();
        if (!clip.supported()) { ClipDebug.warnShapeUnsupported(shape, clip, "backend unsupported", activeShapeDepth()); return false; }
        if (!ScissorFunction.pushRaw(bounds.x(), bounds.y(), bounds.width(), bounds.height())) return false;
        int parent = currentShapeReference(), reference = parent + 1;
        boolean clear = activeShapeDepth() == 0;
        if (Renderer2D.isDeferredExtractRecording()) Renderer2D.deferRenderThreadAction(() -> doPushClipOps(clip, shape, parent, reference, clear));
        else doPushClipOps(clip, shape, parent, reference, clear);
        STACK.push(Layer.shape(shape, reference, parent));
        return true;
    }

    private static void doPushClipOps(ShapeClipBackend clip, UiShape shape, int parent, int reference, boolean clear) {
        clip.requireRenderPassAttachment("ClipFunction.push kind=" + shape.kind() + " bounds=" + shape.bounds());
        if (clear) clip.requestClear("first shape layer");
        clip.beginWrite(parent, reference);
        ClipGeometry.renderMaskShape(shape);
        clip.beginTest(reference);
        ClipDebug.renderStencilDebugProbe(shape, reference, false);
    }

    // ---- Pop ------------------------------------------------------------------------------------

    public static void pop() {
        if (STACK.isEmpty()) return;
        Layer layer = STACK.pop();
        if (!layer.shape) { ScissorFunction.pop(); applyPreviousShapeTest(); return; }
        Renderer2D.flushBatch(Renderer2D.FlushReason.SCISSOR);
        int prevRef = currentShapeReference();
        if (Renderer2D.isDeferredExtractRecording()) {
            Renderer2D.deferRenderThreadAction(() -> doPopClipOps(layer, prevRef));
            ScissorFunction.pop(); return;
        }
        doPopClipOps(layer, prevRef);
        ScissorFunction.pop();
        applyPreviousShapeTest();
    }

    private static void doPopClipOps(Layer layer, int prevRef) {
        ClipDebug.renderStencilDebugProbe(layer.shapeValue, layer.reference, true);
        Renderer2D.flushBatch(Renderer2D.FlushReason.SCISSOR);
        ShapeClipBackend clip = clipBackend();
        clip.beginRestore(layer.reference, layer.parentReference);
        ClipGeometry.renderMaskShape(layer.shapeValue);
        if (prevRef > 0) clip.beginTest(prevRef); else clip.disable();
    }

    // ---- Scopes / queries ---------------------------------------------------------------------

    public static Scope scope(UiShape shape) { return new Scope(push(shape)); }
    public static Scope rectScope(double x, double y, double w, double h) { return new Scope(pushRect(x, y, w, h)); }
    public static int depth() { return STACK.size(); }
    public static boolean isShapeClipActive() { return currentShapeReference() > 0; }

    /** @deprecated use {@link #isShapeClipActive()} — the backend is no longer required to be stencil. */
    @Deprecated public static boolean isStencilActive() { return isShapeClipActive(); }

    // ---- Internals ----------------------------------------------------------------------------

    private static void applyPreviousShapeTest() {
        int ref = currentShapeReference();
        if (ref > 0) clipBackend().beginTest(ref); else clipBackend().disable();
    }

    private static ShapeClipBackend clipBackend() { return LoVisualRenderSystem.rhi().shapeClip(); }

    private static int currentShapeReference() {
        for (Layer l : STACK) if (l.shape) return l.reference;
        return 0;
    }

    private static int activeShapeDepth() {
        int d = 0;
        for (Layer l : STACK) if (l.shape) d++;
        return d;
    }

    private static boolean isPlainRect(UiShape shape) {
        return shape.kind() == UiShapeKind.RECT && shape.cornerMode() == UiCornerMode.NONE;
    }

    // ---- Layer / Scope -----------------------------------------------------------------------

    private record Layer(boolean shape, UiShape shapeValue, int reference, int parentReference) {
        static Layer rect() { return new Layer(false, null, 0, 0); }
        static Layer shape(UiShape s, int ref, int parent) { return new Layer(true, s, ref, parent); }
    }

    /** Auto-closeable scope; pops the clip on {@link #close()} if the push succeeded. */
    public static final class Scope implements AutoCloseable {
        private boolean active;
        private Scope(boolean active) { this.active = active; }
        public boolean active() { return active; }
        @Override public void close() { if (!active) return; active = false; ClipFunction.pop(); }
    }
}
