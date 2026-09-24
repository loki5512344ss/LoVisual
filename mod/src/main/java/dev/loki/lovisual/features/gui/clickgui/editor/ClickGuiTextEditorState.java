package dev.loki.lovisual.features.gui.clickgui.editor;

import com.mojang.blaze3d.platform.InputConstants;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.owner.TextEditorOwner;
import dev.loki.lovisual.util.text.edit.ClipboardUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import static dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiEditorCaret.clamp;

/**
 * State container for the modal text editor shown from ClickGui string / list
 * settings. Owns the mutable buffer, caret, selection, scroll offsets, and the
 * per-frame modal / text-view / footer rectangles. Delegates caret math and
 * viewport adjustment to {@link ClickGuiEditorCaret} and the draw pass to
 * {@link ClickGuiEditorPainter}.
 */
public final class ClickGuiTextEditorState {

    static final float WIDTH = 360f;
    static final float HEIGHT = 240f;
    static final float PADDING = 14f;
    static final float TEXT_PADDING = 8f;
    static final float BUTTON_W = 70f;
    static final float BUTTON_H = 24f;
    static final float LINE_H = 18f;

    // Package-private so sibling helpers in {@code impl/} can read + write.
    final TextEditorOwner owner;
    final String title;
    final StringBuilder buffer;
    final boolean singleLine;
    final float anchorX, anchorY, anchorW;
    final ClickGuiEditorCaret caretHelper;
    final ClickGuiEditorPainter painter;
    final dev.loki.lovisual.util.text.edit.TextSelection selection = new dev.loki.lovisual.util.text.edit.TextSelection();
    int caret;
    float modalX, modalY;
    float textX, textY, textW, textH;
    float saveX, saveY, saveW, saveH;
    float cancelX, cancelY, cancelW, cancelH;
    float scroll;
    float scrollX;
    int preferredColumn = -1;
    boolean dragging = false;

    public ClickGuiTextEditorState(TextEditorOwner owner, String title, String seed, float anchorX, float anchorY, float anchorW) {
        this.owner = owner;
        this.title = title == null ? "Edit list" : title;
        this.singleLine = owner != null && owner.isSingleLine();
        String normalizedSeed = singleLine ? ClickGuiEditorCaret.normalizeSingleLine(seed) : seed;
        this.buffer = new StringBuilder(normalizedSeed == null ? "" : normalizedSeed);
        this.caret = buffer.length();
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorW = anchorW;
        this.caretHelper = new ClickGuiEditorCaret(this);
        this.painter = new ClickGuiEditorPainter(this, caretHelper);
    }

    public void handleMouseDown(float mx, float my, int button) {
        if (button != 0) return;
        painter.layout(ClickGuiRenderer.framebufferWidth(), ClickGuiRenderer.framebufferHeight());
        if (!ClickGuiEditorCaret.inside(mx, my, modalX, modalY, WIDTH, HEIGHT)) { ClickGuiRenderer.closeTextEditor(false); return; }
        if (ClickGuiEditorCaret.inside(mx, my, saveX, saveY, saveW, saveH)) { ClickGuiRenderer.closeTextEditor(true); return; }
        if (ClickGuiEditorCaret.inside(mx, my, cancelX, cancelY, cancelW, cancelH)) { ClickGuiRenderer.closeTextEditor(false); return; }
        if (ClickGuiEditorCaret.inside(mx, my, textX, textY, textW, textH)) {
            int target = caretHelper.caretFromPoint(mx, my);
            caretHelper.updateSelectionForMove(target, isShiftDown());
            caret = target;
            preferredColumn = -1;
            caretHelper.ensureCaretVisible();
            dragging = true;
            return;
        }
        selection.clear();
    }

    public void handleMouseUp(float mx, float my, int button) { if (button == 0) dragging = false; }

    public void handleMouseMove(float mx, float my) {
        if (!dragging) return;
        int target = caretHelper.caretFromPoint(mx, my);
        selection.update(0, target);
        caret = target;
        preferredColumn = -1;
        caretHelper.ensureCaretVisible();
    }

    public void insertChar(char c) { if (!Character.isISOControl(c)) insertRaw(String.valueOf(c), 1); }
    public void insertNewline() { if (!singleLine) insertRaw("\n", 1); }
    public void pasteFromClipboard() {
        String clip = ClipboardUtil.get();
        if (clip == null || clip.isEmpty()) return;
        String normalized = clip.replace("\r\n", "\n").replace('\r', '\n');
        if (singleLine) normalized = ClickGuiEditorCaret.normalizeSingleLine(normalized);
        insertRaw(normalized, normalized.length());
    }

    public void backspace() {
        if (caretHelper.deleteSelection()) { preferredColumn = -1; caretHelper.ensureCaretVisible(); return; }
        if (caret == 0) return;
        buffer.deleteCharAt(caret - 1);
        caret--;
        selection.clear();
        preferredColumn = -1;
        caretHelper.ensureCaretVisible();
        dragging = false;
    }

    public void deleteForward() {
        if (caretHelper.deleteSelection()) { preferredColumn = -1; caretHelper.ensureCaretVisible(); return; }
        if (caret >= buffer.length()) return;
        buffer.deleteCharAt(caret);
        selection.clear();
        preferredColumn = -1;
        caretHelper.ensureCaretVisible();
        dragging = false;
    }

    public void moveCaret(int delta, boolean extend) { moveInternal(clamp(caret + delta, 0, buffer.length()), extend, true); }
    public void moveCaretToStart(boolean extend) { moveInternal(0, extend, true); }
    public void moveCaretToEnd(boolean extend) { moveInternal(buffer.length(), extend, true); }

    public void moveCaretVertical(int dir, boolean extend) {
        String[] lines = buffer.toString().split("\\n", -1);
        int[] pos = ClickGuiEditorCaret.caretLineAndColumn(lines, caret);
        if (preferredColumn < 0) preferredColumn = pos[1];
        int targetLine = clamp(pos[0] + dir, 0, Math.max(0, lines.length - 1));
        int targetCol = Math.min(preferredColumn, lines[targetLine].length());
        int targetIndex = caretHelper.indexForLineColumn(lines, targetLine, targetCol);
        moveInternal(targetIndex, extend, false);
    }

    public void copySelection() {
        if (selection.appliesToLine(0) && selection.hasRange()) {
            int start = clamp(Math.min(selection.start(), selection.end()), 0, buffer.length());
            int end = clamp(Math.max(selection.start(), selection.end()) + 1, 0, buffer.length());
            if (end > start) { ClipboardUtil.copy(buffer.substring(start, end)); return; }
        }
        if (buffer.length() > 0) ClipboardUtil.copy(buffer.toString());
    }

    public void selectAll() {
        selection.update(0, 0);
        selection.updateCaret(buffer.length());
        caret = buffer.length();
        preferredColumn = -1;
        caretHelper.ensureCaretVisible();
    }

    public void save() { owner.applyEditorText(buffer.toString()); }

    public void scroll(double delta) {
        String[] lines = buffer.toString().split("\\n", -1);
        float visible = textH - TEXT_PADDING * 2f;
        float content = lines.length * LINE_H;
        float minScroll = Math.min(0f, visible - content);
        scroll = clamp(scroll + (float) (delta * LINE_H * 1.5f), minScroll, 0f);
    }

    public void render(int fbw, int fbh) { painter.render(fbw, fbh); }

    private void moveInternal(int target, boolean extend, boolean resetPreferred) {
        caretHelper.updateSelectionForMove(target, extend);
        caret = target;
        if (resetPreferred) preferredColumn = -1;
        caretHelper.ensureCaretVisible();
    }

    /** Shared body for insertChar / insertNewline / paste: delete selection, splice text in, refresh caret. */
    private void insertRaw(String text, int caretAdvance) {
        caretHelper.deleteSelection();
        buffer.insert(caret, text);
        caret += caretAdvance;
        selection.clear();
        preferredColumn = -1;
        caretHelper.ensureCaretVisible();
        dragging = false;
    }

    private static boolean isShiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
